package com.example.githubmovil.presentation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.githubmovil.data.DeviceCode
import com.example.githubmovil.data.PullRequest
import com.example.githubmovil.notify.PrNotifier
import com.example.githubmovil.presentation.components.AuroraBackground
import com.example.githubmovil.presentation.screens.ConfirmScreen
import com.example.githubmovil.presentation.screens.DeviceLoginScreen
import com.example.githubmovil.presentation.screens.DoneScreen
import com.example.githubmovil.presentation.screens.FailureScreen
import com.example.githubmovil.presentation.screens.LoadingScreen
import com.example.githubmovil.presentation.screens.LoginScreen
import com.example.githubmovil.presentation.screens.PrDetailScreen
import com.example.githubmovil.presentation.screens.PrListScreen
import com.example.githubmovil.presentation.theme.AppColors
import com.example.githubmovil.presentation.theme.GithubMovilTheme

/** Petición de abrir un PR concreto (viene de tocar una notificación). */
data class OpenRequest(val prKey: String, val nonce: Long = System.nanoTime())

class MainActivity : ComponentActivity() {
    private val openRequest = mutableStateOf<OpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openRequest.value = intent.toOpenRequest()
        setContent {
            GithubMovilTheme {
                DeploymentManagerApp(
                    openRequest = openRequest.value,
                    onOpenRequestHandled = { openRequest.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toOpenRequest()?.let { openRequest.value = it }
    }

    private fun Intent?.toOpenRequest(): OpenRequest? =
        this?.getStringExtra(PrNotifier.EXTRA_PR_KEY)?.let { OpenRequest(it) }
}

@Composable
fun DeploymentManagerApp(openRequest: OpenRequest? = null, onOpenRequestHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { AppController(context, scope) }

    LaunchedEffect(Unit) { controller.start() }
    LaunchedEffect(openRequest) {
        openRequest?.let {
            controller.openFromNotification(it.prKey)
            onOpenRequestHandled()
        }
    }

    val screen = controller.screen

    // Pedimos permiso de notificaciones la primera vez que se ve la lista real de PRs.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var askedPermission by remember { mutableStateOf(false) }
    LaunchedEffect(screen is Screen.PrList) {
        if (screen is Screen.PrList && !controller.isDemo && !askedPermission &&
            Build.VERSION.SDK_INT >= 33 && !PrNotifier.canNotify(context)
        ) {
            askedPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = screen is Screen.Detail || screen is Screen.Confirm || screen is Screen.DeviceLogin) {
        controller.back()
    }

    AppScreen(screen, controller)
}

/** Profundidad de navegación: decide si la transición entra desde la derecha o la izquierda. */
private fun Screen.depth(): Int = when (this) {
    Screen.Login -> 0
    is Screen.DeviceLogin, is Screen.Loading -> 1
    is Screen.PrList -> 2
    is Screen.Detail -> 3
    is Screen.Confirm -> 4
    is Screen.Working -> 5
    is Screen.Done, is Screen.Failure -> 6
}

/** Color principal de la aurora del fondo en cada pantalla. */
private fun Screen.accent(): Color = when (this) {
    Screen.Login, is Screen.Loading, is Screen.PrList -> AppColors.Blue
    is Screen.DeviceLogin, is Screen.Detail, is Screen.Confirm -> AppColors.Purple
    is Screen.Working -> if (merging) AppColors.Green else AppColors.Red
    is Screen.Done -> if (merged) AppColors.Teal else AppColors.Orange
    is Screen.Failure -> AppColors.Red
}

@Composable
private fun AppScreen(screen: Screen, controller: AppController?) {
    AuroraBackground(accent = screen.accent()) {
        AnimatedContent(
            targetState = screen,
            contentKey = { it::class },
            transitionSpec = {
                val dir = if (targetState.depth() >= initialState.depth()) 1 else -1
                val enter = tween<Float>(420, easing = FastOutSlowInEasing)
                (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { dir * it / 3 } +
                    fadeIn(enter) + scaleIn(enter, initialScale = 0.9f)) togetherWith
                    (slideOutHorizontally(tween(300)) { -dir * it / 4 } +
                        fadeOut(tween(220)) + scaleOut(tween(300), targetScale = 0.94f))
            },
            label = "screens",
            modifier = Modifier.fillMaxSize()
        ) { target ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (target) {
                    Screen.Login -> LoginScreen(onLogin = { controller?.login() }, onDemo = { controller?.startDemo() })
                    is Screen.DeviceLogin -> DeviceLoginScreen(target.code)
                    is Screen.Loading -> LoadingScreen(target.text, AppColors.Brand)
                    is Screen.PrList -> PrListScreen(
                        user = target.user,
                        prs = target.prs,
                        onOpen = { controller?.open(it) },
                        onRefresh = { controller?.refresh() },
                        onLogout = { controller?.logout() }
                    )
                    is Screen.Detail -> PrDetailScreen(
                        pr = target.pr,
                        ai = target.ai,
                        onReview = { controller?.review(target.pr) },
                        onBack = { controller?.back() },
                        onRetryAi = { controller?.retrySummary(target.pr) }
                    )
                    is Screen.Confirm -> ConfirmScreen(
                        pr = target.pr,
                        ai = target.ai,
                        onMerge = { controller?.decide(target.pr, merge = true) },
                        onClose = { controller?.decide(target.pr, merge = false) }
                    )
                    is Screen.Working -> LoadingScreen(
                        text = if (target.merging) "Fusionando\n${target.pr.headBranch}" else "Cerrando PR",
                        colors = if (target.merging) AppColors.Success else AppColors.Danger
                    )
                    is Screen.Done -> DoneScreen(target.merged)
                    is Screen.Failure -> FailureScreen(target.message, target.actionLabel, target.action)
                }
            }
        }
    }
}

// ---------------- Previsualizaciones ----------------

private val samplePr = PullRequest(
    repoFullName = "Vasquezjuan7/tienda-online",
    number = 12,
    title = "Ajuste UI Carrito y pasarela de pago",
    author = "Vasquezjuan7",
    headBranch = "feature/carrito",
    baseBranch = "main",
    body = "Se ajustó el padding del carrito de compras y se solucionó el desbordamiento en pantallas pequeñas.",
    updatedAt = "2026-11-05T08:00:00Z",
    draft = false
)

@WearPreviewDevices
@Composable
fun Preview_Login() {
    GithubMovilTheme { AppScreen(Screen.Login, null) }
}

@WearPreviewDevices
@Composable
fun Preview_DeviceLogin() {
    GithubMovilTheme {
        AppScreen(Screen.DeviceLogin(DeviceCode("", "WDJB-MJHT", "https://github.com/login/device", 5, 900)), null)
    }
}

@WearPreviewDevices
@Composable
fun Preview_List() {
    GithubMovilTheme {
        AppScreen(
            Screen.PrList(
                "Vasquezjuan7",
                listOf(
                    samplePr,
                    samplePr.copy(repoFullName = "acme/api", number = 3, title = "Fix login con OAuth", author = "maria", draft = true)
                )
            ),
            null
        )
    }
}

@WearPreviewDevices
@Composable
fun Preview_Empty() {
    GithubMovilTheme { AppScreen(Screen.PrList("Vasquezjuan7", emptyList()), null) }
}

@WearPreviewDevices
@Composable
fun Preview_Detail() {
    GithubMovilTheme {
        AppScreen(
            Screen.Detail(samplePr, AiState.Ready(com.example.githubmovil.data.DemoData.summaryFor(samplePr.copy(number = 42)))),
            null
        )
    }
}

@WearPreviewDevices
@Composable
fun Preview_Confirm() {
    GithubMovilTheme { AppScreen(Screen.Confirm(samplePr), null) }
}

@WearPreviewDevices
@Composable
fun Preview_Done() {
    GithubMovilTheme { AppScreen(Screen.Done(merged = true), null) }
}

@WearPreviewDevices
@Composable
fun Preview_Failure() {
    GithubMovilTheme { AppScreen(Screen.Failure("Pull Request is not mergeable", "Volver") {}, null) }
}
