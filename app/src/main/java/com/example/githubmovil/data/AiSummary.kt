package com.example.githubmovil.data

import android.content.Context
import com.example.githubmovil.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Resumen generado por la IA (Gemini) de lo que hace un PR al hacer merge. */
data class AiSummary(
    val resumen: String,
    val cambios: List<String>,
    /** "bajo", "medio" o "alto". */
    val riesgo: String,
    val motivoRiesgo: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("resumen", resumen)
        .put("cambios", JSONArray(cambios))
        .put("riesgo", riesgo)
        .put("motivo_riesgo", motivoRiesgo)

    companion object {
        fun fromJson(json: JSONObject): AiSummary {
            val items = json.optJSONArray("cambios") ?: JSONArray()
            return AiSummary(
                resumen = json.getString("resumen"),
                cambios = (0 until items.length()).map { items.getString(it) },
                riesgo = json.optString("riesgo", "medio").lowercase(),
                motivoRiesgo = json.optString("motivo_riesgo"),
            )
        }
    }
}

/**
 * Llama a la Edge Function de Supabase "pr-summary", que tiene guardada la clave de Gemini.
 * La clave de la IA nunca está en el reloj; el reloj solo envía su token de GitHub
 * para demostrar quién es.
 */
object AiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // la IA puede tardar unos segundos con PRs grandes
        .build()

    val isConfigured: Boolean get() = BuildConfig.AI_SUMMARY_URL.isNotBlank()

    suspend fun summarize(githubToken: String, pr: PullRequest): AiSummary = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("repo", pr.repoFullName)
            .put("number", pr.number)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(BuildConfig.AI_SUMMARY_URL)
            .header("x-github-token", githubToken)
            .header("apikey", BuildConfig.SUPABASE_KEY)
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (!response.isSuccessful || json == null || json.has("error")) {
                val message = json?.optString("error")?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}"
                throw GitHubException(response.code, message)
            }
            AiSummary.fromJson(json)
        }
    }
}

/** Caché local de resúmenes: si el PR no ha cambiado, no se vuelve a llamar a la IA. */
class SummaryCache(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ai_summaries", Context.MODE_PRIVATE)

    fun get(pr: PullRequest): AiSummary? {
        val raw = prefs.getString(pr.key, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            if (json.getString("updated_at") != pr.updatedAt) null
            else AiSummary.fromJson(json.getJSONObject("summary"))
        }.getOrNull()
    }

    fun put(pr: PullRequest, summary: AiSummary) {
        val json = JSONObject().put("updated_at", pr.updatedAt).put("summary", summary.toJson())
        prefs.edit().putString(pr.key, json.toString()).apply()
    }

    fun clear() = prefs.edit().clear().apply()
}
