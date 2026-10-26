package com.example.githubmovil.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.example.githubmovil.R
import com.example.githubmovil.data.DeviceCode
import com.example.githubmovil.data.PullRequest
import com.example.githubmovil.presentation.AiState
import com.example.githubmovil.presentation.components.AnimatedCheckmark
import com.example.githubmovil.presentation.components.Avatar
import com.example.githubmovil.presentation.components.BezelProgress
import com.example.githubmovil.presentation.components.GlassButton
import com.example.githubmovil.presentation.components.GradientButton
import com.example.githubmovil.presentation.components.GradientSpinner
import com.example.githubmovil.presentation.components.GradientText
import com.example.githubmovil.presentation.components.HoldToConfirmButton
import com.example.githubmovil.presentation.components.PulsingRings
import com.example.githubmovil.presentation.components.bounceClick
import com.example.githubmovil.presentation.components.colorsFor
import com.example.githubmovil.presentation.components.glass
import com.example.githubmovil.presentation.components.relativeTime
import com.example.githubmovil.presentation.components.staggeredEntrance
import com.example.githubmovil.presentation.theme.AppColors
import kotlinx.coroutines.delay
import kotlin.math.sin

// ============================ Login ============================

@Composable
fun LoginScreen(onLogin: () -> Unit, onDemo: () -> Unit) {
    val float by rememberInfiniteTransition(label = "float").animateFloat(
        0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "floatT"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(62.dp)
                .staggeredEntrance(0)
                .graphicsLayer { translationY = sin(float) * 3.dp.toPx() }
        ) {
            PulsingRings(AppColors.Purple, Modifier.fillMaxSize())
            LogoBadge(size = 44.dp)
        }
        GradientText("PR Watch", AppColors.Brand, 17.sp, Modifier.staggeredEntrance(1))
        Text(
            "Tus PRs en la muñeca",
            color = AppColors.TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.staggeredEntrance(2)
        )
        Spacer(Modifier.height(8.dp))
        GradientButton(
            text = "Conectar GitHub",
            colors = AppColors.Brand,
            onClick = onLogin,
            modifier = Modifier.staggeredEntrance(3)
        )
        Text(
            "Ver demo",
            color = AppColors.TextMuted,
            fontSize = 10.sp,
            modifier = Modifier
                .staggeredEntrance(4)
                .bounceClick(onDemo)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        )
    }
}

/** Logo de la app (los mismos vectores que el icono del launcher), recortado en círculo. */
@Composable
fun LogoBadge(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }
    ) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
    }
}

// ============================ Device login ============================

@Composable
fun DeviceLoginScreen(code: DeviceCode) {
    val remaining = remember(code) { Animatable(1f) }
    LaunchedEffect(code) {
        remaining.animateTo(0f, tween(code.expiresInSeconds * 1000, easing = LinearEasing))
    }
    val borderAngle by rememberInfiniteTransition(label = "border").animateFloat(
        0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "borderA"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Cuenta atrás en el bisel: cuánto tiempo queda para introducir el código.
        BezelProgress(remaining.value, listOf(AppColors.Cyan, AppColors.Purple, AppColors.Pink))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(18.dp)) {
            Text("Abre en tu móvil", color = AppColors.TextMuted, fontSize = 11.sp)
            Text(
                code.verificationUri.removePrefix("https://"),
                color = AppColors.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            // Caja del código con borde degradado giratorio.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .drawBehind {
                        val brush = Brush.sweepGradient(
                            listOf(AppColors.Cyan, AppColors.Purple, AppColors.Pink, AppColors.Cyan),
                            center = center
                        )
                        // Giramos el degradado dibujándolo sobre un lienzo rotado.
                        rotate(borderAngle) { drawCircle(brush, radius = size.maxDimension) }
                    }
                    .padding(2.dp)
                    .background(AppColors.Background, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row {
                    code.userCode.forEachIndexed { i, ch -> PopChar(ch, i) }
                }
            }

            Spacer(Modifier.height(10.dp))
            AnimatedDotsText("Esperando autorización", AppColors.TextMuted)
        }
    }
}

/** Cada carácter del código aparece con un "pop" escalonado. */
@Composable
private fun PopChar(ch: Char, index: Int) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150L + index * 60L)
        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f))
    }
    Text(
        ch.toString(),
        color = if (ch == '-') AppColors.TextMuted else Color.White,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = scale.value.coerceIn(0f, 1f)
        }
    )
}

@Composable
fun AnimatedDotsText(text: String, color: Color, fontSize: Int = 11) {
    val dots by rememberInfiniteTransition(label = "dots").animateFloat(
        0f, 3.99f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "dotsN"
    )
    Row {
        Text(text, color = color, fontSize = fontSize.sp, textAlign = TextAlign.Center)
        Text(".".repeat(dots.toInt()).padEnd(3), color = color, fontSize = fontSize.sp)
    }
}

// ============================ Cargando ============================

@Composable
fun LoadingScreen(text: String, colors: List<Color>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
            PulsingRings(colors.first(), Modifier.fillMaxSize())
            GradientSpinner(colors, 54.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            color = AppColors.TextMain,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp
        )
    }
}

// ============================ Lista de PRs ============================

@Composable
fun PrListScreen(
    user: String,
    prs: List<PullRequest>,
    onOpen: (PullRequest) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    ScalingLazyColumn(
        state = rememberScalingLazyListState(initialCenterItemIndex = 0),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            if (prs.isEmpty()) AllClearHeader(user) else CountHeader(user, prs.size)
        }
        itemsIndexed(prs, key = { _, pr -> "${pr.repoFullName}#${pr.number}" }) { index, pr ->
            PrCard(pr, Modifier.staggeredEntrance(index + 1)) { onOpen(pr) }
        }
        item {
            GlassButton("Actualizar", Icons.Default.Refresh, onRefresh, Modifier.padding(top = 6.dp))
        }
        item {
            Text(
                "Cerrar sesión",
                color = AppColors.TextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .bounceClick(onLogout)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun UserChip(user: String) {
    val pulse by rememberInfiniteTransition(label = "online").animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "onlineA"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .glass(RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .graphicsLayer { alpha = pulse }
                .background(AppColors.Teal, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text("@$user", color = AppColors.TextMain, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun CountHeader(user: String, count: Int) {
    val animatedCount by animateIntAsState(count, tween(900, easing = FastOutSlowInEasing), label = "count")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.staggeredEntrance(0)) {
        UserChip(user)
        GradientText("$animatedCount", AppColors.Brand, 34.sp)
        Text(
            if (count == 1) "PR esperando tu revisión" else "PRs esperando tu revisión",
            color = AppColors.TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun AllClearHeader(user: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.staggeredEntrance(0)) {
        UserChip(user)
        Spacer(Modifier.height(6.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
            PulsingRings(AppColors.Teal, Modifier.fillMaxSize())
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(AppColors.Success)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
        GradientText("Todo al día", AppColors.Success, 16.sp)
        Text("No hay PRs pendientes", color = AppColors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun PrCard(pr: PullRequest, modifier: Modifier, onClick: () -> Unit) {
    val repoColors = colorsFor(pr.repoFullName)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick)
            .clip(shape)
            .glass(shape)
            .drawBehind {
                // Barra de acento a la izquierda con el color del repo.
                drawRect(
                    Brush.verticalGradient(repoColors),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                )
            }
            .padding(start = 13.dp, end = 12.dp, top = 8.dp, bottom = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(pr.author, 16.dp)
            Spacer(Modifier.width(5.dp))
            Text(
                pr.repoFullName.substringAfter('/'),
                color = repoColors.first(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(relativeTime(pr.updatedAt), color = AppColors.TextMuted, fontSize = 9.sp, maxLines = 1)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            pr.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#${pr.number} · ${pr.author}", color = AppColors.TextMuted, fontSize = 10.sp, maxLines = 1)
            if (pr.draft) {
                Spacer(Modifier.width(5.dp))
                Text(
                    "BORRADOR",
                    color = AppColors.Orange,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, AppColors.Orange.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

// ============================ Detalle ============================

@Composable
fun PrDetailScreen(
    pr: PullRequest,
    ai: AiState,
    onReview: () -> Unit,
    onBack: () -> Unit,
    onRetryAi: () -> Unit = {},
) {
    val repoColors = colorsFor(pr.repoFullName)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .staggeredEntrance(0)
                .glass(RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Box(Modifier.size(7.dp).background(Brush.linearGradient(repoColors), CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(pr.repoFullName, color = AppColors.TextMain, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            pr.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.staggeredEntrance(1)
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.staggeredEntrance(2)) {
            Avatar(pr.author, 20.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                "${pr.author} · ${relativeTime(pr.updatedAt)}",
                color = AppColors.TextMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(10.dp))
        BranchFlow(pr.headBranch, pr.baseBranch, Modifier.staggeredEntrance(3))
        Spacer(Modifier.height(10.dp))
        if (ai !is AiState.Off) {
            AiSummaryCard(ai, onRetryAi, Modifier.staggeredEntrance(4))
            Spacer(Modifier.height(8.dp))
            Text(
                "DESCRIPCIÓN",
                color = AppColors.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .staggeredEntrance(4)
                .fillMaxWidth()
                .glass(RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(pr.body, color = AppColors.TextMain, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(14.dp))
        GradientButton(
            text = "Revisar",
            colors = AppColors.Brand,
            onClick = onReview,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.staggeredEntrance(5)
        )
        Text(
            "Volver",
            color = AppColors.TextMuted,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(top = 4.dp)
                .bounceClick(onBack)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(30.dp))
    }
}

/** Rama origen → rama destino, con puntos de luz que "fluyen" de una a otra. */
@Composable
private fun BranchFlow(head: String, base: String, modifier: Modifier = Modifier) {
    val flow by rememberInfiniteTransition(label = "flow").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "flowP"
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        BranchChip(head, AppColors.Cyan, Modifier.weight(1f, fill = false))
        Canvas(Modifier.width(26.dp).height(10.dp)) {
            val y = size.height / 2
            repeat(3) { i ->
                val p = (flow + i / 3f) % 1f
                drawCircle(
                    Brush.linearGradient(listOf(AppColors.Cyan, AppColors.Purple)),
                    radius = 2.dp.toPx(),
                    center = Offset(size.width * p, y),
                    alpha = sin(p * Math.PI).toFloat()
                )
            }
        }
        BranchChip(base, AppColors.Purple, Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun BranchChip(name: String, color: Color, modifier: Modifier) {
    Text(
        name,
        color = color,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

// ============================ Confirmar ============================

@Composable
fun ConfirmScreen(pr: PullRequest, ai: AiState, onMerge: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "MANTÉN PULSADO",
            color = AppColors.Purple,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.staggeredEntrance(0)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            pr.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            modifier = Modifier.staggeredEntrance(1)
        )
        Text(
            "${pr.repoFullName.substringAfter('/')} #${pr.number}",
            color = AppColors.TextMuted,
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.staggeredEntrance(1)
        )
        if (ai is AiState.Ready) {
            // Aviso de la IA justo antes de decidir.
            val colors = riskColors(ai.summary.riesgo)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .staggeredEntrance(2)
                    .background(colors.first().copy(alpha = 0.14f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Sparkle(9.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    "IA: riesgo ${ai.summary.riesgo}",
                    color = colors.first(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.staggeredEntrance(2)
        ) {
            HoldToConfirmButton(Icons.Default.Close, "Cerrar", AppColors.Danger, onClose, size = 54.dp)
            HoldToConfirmButton(Icons.Default.Check, "Merge", AppColors.Success, onMerge, size = 54.dp)
        }
    }
}

// ============================ Resultado ============================

@Composable
fun DoneScreen(merged: Boolean) {
    val colors = if (merged) AppColors.Success else AppColors.Danger
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AnimatedCheckmark(colors, 70.dp)
        GradientText(
            if (merged) "¡Merge completado!" else "PR cerrado",
            colors,
            15.sp,
            Modifier.staggeredEntrance(5)
        )
    }
}

@Composable
fun FailureScreen(message: String, actionLabel: String, onAction: () -> Unit) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(200)
        repeat(3) {
            shake.animateTo(9f, tween(45))
            shake.animateTo(-9f, tween(45))
        }
        shake.animateTo(0f, tween(45))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .clip(CircleShape)
                .background(Brush.linearGradient(AppColors.Danger)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        GradientText("Algo falló", AppColors.Danger, 15.sp)
        Text(
            message,
            color = AppColors.TextMuted,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(actionLabel, null, onAction)
    }
}
