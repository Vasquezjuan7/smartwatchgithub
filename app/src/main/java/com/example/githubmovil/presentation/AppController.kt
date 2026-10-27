package com.example.githubmovil.presentation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.githubmovil.BuildConfig
import com.example.githubmovil.data.AiClient
import com.example.githubmovil.data.AiSummary
import com.example.githubmovil.data.DemoData
import com.example.githubmovil.data.DeviceCode
import com.example.githubmovil.data.GitHubClient
import com.example.githubmovil.data.GitHubException
import com.example.githubmovil.data.PullRequest
import com.example.githubmovil.data.SummaryCache
import com.example.githubmovil.data.TokenStore
import com.example.githubmovil.notify.PrCheckScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

/** Estado del resumen con IA de un PR. */
sealed interface AiState {
    data object Loading : AiState
    data class Ready(val summary: AiSummary) : AiState
    data class Error(val message: String) : AiState
    data object Off : AiState
}

sealed interface Screen {
    data object Login : Screen
    data class DeviceLogin(val code: DeviceCode) : Screen
    data class Loading(val text: String) : Screen
    data class PrList(val user: String, val prs: List<PullRequest>) : Screen
    data class Detail(val pr: PullRequest, val ai: AiState = AiState.Off) : Screen
    data class Confirm(val pr: PullRequest, val ai: AiState = AiState.Off) : Screen
    data class Working(val pr: PullRequest, val merging: Boolean) : Screen
    data class Done(val merged: Boolean) : Screen
    data class Failure(val message: String, val actionLabel: String, val action: () -> Unit) : Screen
}

/** Toda la lógica: login con GitHub, carga de PRs y merge/close directo contra la API. */
class AppController(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext
    private val store = TokenStore(appContext)
    private val summaryCache = SummaryCache(appContext)
    private val summaries = mutableMapOf<String, AiState>()

    var screen by mutableStateOf<Screen>(Screen.Login)
        private set

    private var user = ""
    private var prs = emptyList<PullRequest>()
    private var job: Job? = null
    private var demo = false

    /** PR que hay que abrir al terminar de cargar (cuando se toca una notificación). */
    private var pendingOpenKey: String? = null

    val isDemo: Boolean get() = demo

    fun start() {
        if (store.token != null) {
            PrCheckScheduler.schedule(appContext) // por si se reinstaló la app
            refresh()
        } else {
            screen = Screen.Login
        }
    }

    /** Llamado al tocar una notificación de "Nuevo PR". */
    fun openFromNotification(key: String) {
        if (demo || store.token == null) return
        pendingOpenKey = key
        refresh()
    }

    fun login() {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) {
            screen = Screen.Failure(
                "Falta github.clientId en local.properties (ver README).", "Reintentar", ::login
            )
            return
        }
        launch(onError = { msg -> Screen.Failure(msg, "Reintentar", ::login) }) {
            screen = Screen.Loading("Conectando\ncon GitHub...")
            val code = GitHubClient.requestDeviceCode(clientId)
            screen = Screen.DeviceLogin(code)
            store.token = GitHubClient.pollForToken(clientId, code)
            PrCheckScheduler.schedule(appContext)
            loadPrs()
        }
    }

    /** Modo demo: datos de ejemplo y merge simulado, sin tocar GitHub. */
    fun startDemo() {
        demo = true
        launch(onError = { msg -> Screen.Failure(msg, "Reintentar", ::startDemo) }) {
            screen = Screen.Loading("Buscando PRs en\ntodos tus repos...")
            delay(1600)
            user = DemoData.USER
            prs = DemoData.pullRequests()
            screen = Screen.PrList(user, prs)
        }
    }

    fun refresh() {
        if (demo) return startDemo()
        launch(onError = { msg -> Screen.Failure(msg, "Reintentar", ::refresh) }) { loadPrs() }
    }

    fun open(pr: PullRequest) {
        val ai = aiStateFor(pr)
        screen = Screen.Detail(pr, ai)
        if (ai !is AiState.Ready && ai !is AiState.Loading) requestSummary(pr)
    }

    fun review(pr: PullRequest) { screen = Screen.Confirm(pr, aiStateFor(pr)) }

    fun retrySummary(pr: PullRequest) = requestSummary(pr)

    private fun aiStateFor(pr: PullRequest): AiState =
        summaries[pr.key] ?: summaryCache.get(pr)?.let { AiState.Ready(it) } ?: AiState.Off

    /** Pide el resumen a la IA en paralelo, sin bloquear la navegación. */
    private fun requestSummary(pr: PullRequest) {
        val token = store.token
        if (!demo && (token == null || !AiClient.isConfigured)) return
        setAi(pr, AiState.Loading)
        scope.launch {
            val result = try {
                val summary = if (token == null) {
                    delay(1400) // demo: simulamos lo que tarda la IA
                    DemoData.summaryFor(pr)
                } else {
                    AiClient.summarize(token, pr).also { summaryCache.put(pr, it) }
                }
                AiState.Ready(summary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AiState.Error("Sin conexión con la IA")
            } catch (e: Exception) {
                AiState.Error(e.message ?: "La IA no respondió")
            }
            setAi(pr, result)
        }
    }

    /** Guarda el estado y, si el PR está en pantalla, la actualiza. */
    private fun setAi(pr: PullRequest, state: AiState) {
        summaries[pr.key] = state
        screen = when (val s = screen) {
            is Screen.Detail -> if (s.pr.key == pr.key) s.copy(ai = state) else s
            is Screen.Confirm -> if (s.pr.key == pr.key) s.copy(ai = state) else s
            else -> s
        }
    }

    fun decide(pr: PullRequest, merge: Boolean) {
        val token = store.token
        if (token == null && !demo) return logout()
        launch(onError = { msg -> Screen.Failure(msg, "Volver") { screen = Screen.Detail(pr, aiStateFor(pr)) } }) {
            screen = Screen.Working(pr, merge)
            when {
                token == null -> delay(1800) // demo
                merge -> GitHubClient.merge(token, pr)
                else -> GitHubClient.close(token, pr)
            }
            prs = prs - pr
            screen = Screen.Done(merge)
            delay(1800)
            screen = Screen.PrList(user, prs)
        }
    }

    /** Devuelve true si consumió el gesto "atrás". */
    fun back(): Boolean {
        screen = when (val s = screen) {
            is Screen.Detail -> Screen.PrList(user, prs)
            is Screen.Confirm -> Screen.Detail(s.pr, aiStateFor(s.pr))
            is Screen.DeviceLogin -> { job?.cancel(); Screen.Login }
            else -> return false
        }
        return true
    }

    fun logout() {
        job?.cancel()
        if (!demo) PrCheckScheduler.cancel(appContext)
        demo = false
        store.token = null
        user = ""
        prs = emptyList()
        summaries.clear()
        summaryCache.clear()
        screen = Screen.Login
    }

    private suspend fun loadPrs() {
        val token = store.token ?: return logout()
        screen = Screen.Loading("Buscando PRs en\ntodos tus repos...")
        if (user.isEmpty()) user = GitHubClient.currentUser(token)
        prs = GitHubClient.openPullRequests(token)
        // Lo que ves en la app ya no se notifica como "nuevo".
        store.seenPrs = prs.map { it.key }.toSet()
        val toOpen = pendingOpenKey?.let { key -> prs.find { it.key == key } }
        pendingOpenKey = null
        if (toOpen != null) open(toOpen) else screen = Screen.PrList(user, prs)
    }

    private fun launch(onError: (String) -> Screen, block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: GitHubException) {
                if (e.code == 401) {
                    logout() // token revocado o caducado: volver a iniciar sesión
                } else {
                    screen = onError(e.message ?: "Error de GitHub")
                }
            } catch (e: IOException) {
                screen = onError("Sin conexión. Revisa el Wi-Fi o el Bluetooth del reloj.")
            } catch (e: Exception) {
                e.printStackTrace()
                screen = onError(e.message ?: "Error inesperado")
            }
        }
    }
}

