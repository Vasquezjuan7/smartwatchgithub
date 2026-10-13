package com.example.githubmovil.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PullRequest(
    val repoFullName: String,
    val number: Int,
    val title: String,
    val author: String,
    val headBranch: String,
    val baseBranch: String,
    val body: String,
    val updatedAt: String,
    val draft: Boolean,
) {
    /** Identificador único entre todos los repos: "owner/repo#123". */
    val key: String get() = "$repoFullName#$number"
}

data class DeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val intervalSeconds: Int,
    val expiresInSeconds: Int,
)

/** Error devuelto por GitHub, con el mensaje legible que manda la API. */
class GitHubException(val code: Int, message: String) : Exception(message)

/**
 * Cliente de GitHub que corre directo en el reloj: ya no hace falta el servidor Node.
 * Usa OAuth Device Flow, así cada usuario autoriza su propia cuenta y
 * no hay ningún token escrito en el código.
 */
object GitHubClient {
    private const val API = "https://api.github.com"
    private const val SCOPES = "repo"

    /** Máximo de repos (los que tuvieron push más reciente) donde se buscan PRs. */
    private const val MAX_REPOS = 60
    private const val PARALLEL_REQUESTS = 6

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // ---------- Login (Device Flow) ----------

    suspend fun requestDeviceCode(clientId: String): DeviceCode {
        val form = FormBody.Builder()
            .add("client_id", clientId)
            .add("scope", SCOPES)
            .build()
        val json = JSONObject(send("POST", "https://github.com/login/device/code", null, form))
        if (json.has("error")) {
            throw GitHubException(400, json.optString("error_description", json.getString("error")))
        }
        return DeviceCode(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.getString("verification_uri"),
            intervalSeconds = json.optInt("interval", 5),
            expiresInSeconds = json.optInt("expires_in", 900),
        )
    }

    /** Espera hasta que el usuario introduzca el código en github.com/login/device. */
    suspend fun pollForToken(clientId: String, code: DeviceCode): String {
        var interval = code.intervalSeconds.coerceAtLeast(5)
        val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(interval * 1000L)
            val form = FormBody.Builder()
                .add("client_id", clientId)
                .add("device_code", code.deviceCode)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build()
            val json = JSONObject(send("POST", "https://github.com/login/oauth/access_token", null, form))
            when (json.optString("error")) {
                "" -> return json.getString("access_token")
                "authorization_pending" -> Unit
                "slow_down" -> interval = json.optInt("interval", interval + 5)
                "expired_token" -> throw GitHubException(400, "El código caducó. Inténtalo de nuevo.")
                "access_denied" -> throw GitHubException(400, "Autorización cancelada.")
                else -> throw GitHubException(400, json.optString("error_description", json.getString("error")))
            }
        }
        throw GitHubException(400, "El código caducó. Inténtalo de nuevo.")
    }

    // ---------- API ----------

    suspend fun currentUser(token: String): String =
        JSONObject(send("GET", "$API/user", token)).getString("login")

    /**
     * PRs abiertos de TODOS los repos donde el usuario puede hacer merge
     * (propios, de colaborador y de organizaciones), del más reciente al más antiguo.
     */
    suspend fun openPullRequests(token: String): List<PullRequest> = coroutineScope {
        val repos = writableReposWithOpenItems(token)
        val limiter = Semaphore(PARALLEL_REQUESTS)
        repos.map { fullName ->
            async {
                limiter.withPermit {
                    // Un repo que falle (p. ej. una org que bloquea apps OAuth) no rompe la lista entera.
                    runCatching { pullsForRepo(token, fullName) }.getOrDefault(emptyList())
                }
            }
        }.awaitAll()
            .flatten()
            .sortedByDescending { it.updatedAt }
    }

    suspend fun merge(token: String, pr: PullRequest) {
        val body = JSONObject()
            .put("commit_title", "${pr.title} (#${pr.number})")
            .put("commit_message", "Aprobado desde el smartwatch")
            .toString().toRequestBody(JSON)
        send("PUT", "$API/repos/${pr.repoFullName}/pulls/${pr.number}/merge", token, body)
    }

    suspend fun close(token: String, pr: PullRequest) {
        val body = JSONObject().put("state", "closed").toString().toRequestBody(JSON)
        send("PATCH", "$API/repos/${pr.repoFullName}/pulls/${pr.number}", token, body)
    }

    private suspend fun writableReposWithOpenItems(token: String): List<String> {
        val result = mutableListOf<String>()
        var page = 1
        while (result.size < MAX_REPOS && page <= 5) {
            val url = "$API/user/repos?per_page=100&page=$page&sort=pushed" +
                "&affiliation=owner,collaborator,organization_member"
            val repos = JSONArray(send("GET", url, token))
            for (i in 0 until repos.length()) {
                val repo = repos.getJSONObject(i)
                val canPush = repo.optJSONObject("permissions")?.optBoolean("push") == true
                // open_issues_count incluye PRs: si es 0 no hace falta preguntar por ese repo.
                if (canPush && !repo.optBoolean("archived") && repo.optInt("open_issues_count") > 0) {
                    result += repo.getString("full_name")
                }
            }
            if (repos.length() < 100) break
            page++
        }
        return result.take(MAX_REPOS)
    }

    private suspend fun pullsForRepo(token: String, fullName: String): List<PullRequest> {
        val pulls = JSONArray(send("GET", "$API/repos/$fullName/pulls?state=open&per_page=30", token))
        return (0 until pulls.length()).map { i ->
            val pr = pulls.getJSONObject(i)
            val rawBody = if (pr.isNull("body")) "" else pr.getString("body")
            PullRequest(
                repoFullName = fullName,
                number = pr.getInt("number"),
                title = pr.getString("title"),
                author = pr.optJSONObject("user")?.optString("login") ?: "Desconocido",
                headBranch = pr.getJSONObject("head").getString("ref"),
                baseBranch = pr.getJSONObject("base").getString("ref"),
                body = rawBody.trim().ifEmpty { "Sin descripción." }.take(800),
                updatedAt = pr.getString("updated_at"),
                draft = pr.optBoolean("draft"),
            )
        }
    }

    private suspend fun send(
        method: String,
        url: String,
        token: String?,
        body: RequestBody? = null,
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", if (url.startsWith(API)) "application/vnd.github+json" else "application/json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .method(method, body)
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
                throw GitHubException(response.code, message)
            }
            text
        }
    }
}
