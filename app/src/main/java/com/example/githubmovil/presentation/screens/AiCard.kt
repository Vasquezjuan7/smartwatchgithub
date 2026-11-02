package com.example.githubmovil.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.githubmovil.data.AiSummary
import com.example.githubmovil.presentation.AiState
import com.example.githubmovil.presentation.components.GradientText
import com.example.githubmovil.presentation.components.bounceClick
import com.example.githubmovil.presentation.theme.AppColors
import kotlinx.coroutines.delay

/** Colores del indicador de riesgo. */
fun riskColors(riesgo: String): List<Color> = when (riesgo) {
    "alto" -> AppColors.Danger
    "medio" -> listOf(AppColors.Orange, Color(0xFFFFC857))
    else -> AppColors.Success
}

/** Tarjeta "Resumen IA": borde degradado giratorio + contenido animado según el estado. */
@Composable
fun AiSummaryCard(state: AiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (state is AiState.Off) return
    val angle by rememberInfiniteTransition(label = "aiBorder").animateFloat(
        0f, 360f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "aiBorderA"
    )
    val outer = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(outer)
            .drawBehind {
                rotate(angle) {
                    drawCircle(
                        Brush.sweepGradient(
                            listOf(AppColors.Cyan, AppColors.Purple, AppColors.Pink, Color.Transparent, AppColors.Cyan)
                        ),
                        radius = size.maxDimension
                    )
                }
            }
            .padding(1.5.dp)
            .background(Color(0xF20A0D16), RoundedCornerShape(14.5.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sparkle(13.dp)
            Spacer(Modifier.width(5.dp))
            GradientText("Resumen IA", AppColors.Brand, 11.sp)
            Spacer(Modifier.weight(1f))
            if (state is AiState.Ready) RiskChip(state.summary.riesgo)
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label = "aiContent"
        ) { s ->
            when (s) {
                AiState.Loading -> ThinkingPlaceholder()
                is AiState.Ready -> SummaryContent(s.summary)
                is AiState.Error -> Column {
                    Text(s.message, color = AppColors.TextMuted, fontSize = 11.sp, lineHeight = 14.sp)
                    Text(
                        "Reintentar",
                        color = AppColors.Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .bounceClick(onRetry)
                            .padding(vertical = 4.dp)
                    )
                }
                AiState.Off -> Unit
            }
        }
    }
}

/** Estrella de 4 puntas que gira y late, el icono de "IA". */
@Composable
fun Sparkle(size: Dp) {
    val transition = rememberInfiniteTransition(label = "sparkle")
    val rotation by transition.animateFloat(0f, 90f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "sparkR")
    val pulse by transition.animateFloat(0.8f, 1.1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "sparkP")
    Canvas(
        Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation; scaleX = pulse; scaleY = pulse }
    ) {
        val c = center
        val r = this.size.minDimension / 2
        val k = r * 0.22f
        val star = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticTo(c.x + k, c.y - k, c.x + r, c.y)
            quadraticTo(c.x + k, c.y + k, c.x, c.y + r)
            quadraticTo(c.x - k, c.y + k, c.x - r, c.y)
            quadraticTo(c.x - k, c.y - k, c.x, c.y - r)
            close()
        }
        drawPath(star, Brush.linearGradient(listOf(AppColors.Cyan, AppColors.Pink), Offset.Zero, Offset(this.size.width, this.size.height)))
    }
}

@Composable
private fun RiskChip(riesgo: String) {
    val colors = riskColors(riesgo)
    val pulse by rememberInfiniteTransition(label = "risk").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "riskP"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(colors.first().copy(alpha = 0.16f), RoundedCornerShape(50))
            .border(1.dp, colors.first().copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            Modifier
                .size(5.dp)
                .graphicsLayer { alpha = if (riesgo == "alto") pulse else 1f }
                .background(Brush.linearGradient(colors), CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Riesgo $riesgo",
            color = colors.first(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Barras "esqueleto" con un brillo que las recorre mientras la IA analiza el código. */
@Composable
private fun ThinkingPlaceholder() {
    val shimmer by rememberInfiniteTransition(label = "skeleton").animateFloat(
        -1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "skeletonX"
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(1f, 0.86f, 0.6f).forEach { fraction ->
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.07f))
                    .drawBehind {
                        val x = size.width * shimmer
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AppColors.Purple.copy(alpha = 0.55f), Color.Transparent),
                                startX = x - size.width * 0.4f,
                                endX = x + size.width * 0.4f
                            )
                        )
                    }
            )
        }
        Spacer(Modifier.height(1.dp))
        AnimatedDotsText("Analizando el código", AppColors.TextMuted, 10)
    }
}

/** Resumen escrito letra a letra (efecto máquina de escribir) y luego los puntos clave. */
@Composable
private fun SummaryContent(summary: AiSummary) {
    val text = summary.resumen
    val typed = remember(summary) { Animatable(0f) }
    val msPerChar = 16
    LaunchedEffect(summary) {
        typed.animateTo(text.length.toFloat(), tween(text.length * msPerChar, easing = LinearEasing))
    }
    val typingDone = text.length * msPerChar

    Column {
        Text(
            text.take(typed.value.toInt()) + if (typed.value < text.length) "▍" else "",
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(6.dp))
        summary.cambios.forEachIndexed { i, change ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(vertical = 1.5.dp)
                    .delayedEntrance(typingDone + i * 140L)
            ) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .size(5.dp)
                        .background(Brush.linearGradient(AppColors.Brand), CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(change, color = AppColors.TextMain, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        if (summary.motivoRiesgo.isNotBlank()) {
            Text(
                summary.motivoRiesgo,
                color = riskColors(summary.riesgo).first().copy(alpha = 0.85f),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                modifier = Modifier
                    .padding(top = 5.dp)
                    .delayedEntrance(typingDone + summary.cambios.size * 140L)
            )
        }
    }
}

/** Aparece deslizándose desde la izquierda tras [delayMs]. */
@Composable
private fun Modifier.delayedEntrance(delayMs: Long): Modifier {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        anim.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 300f))
    }
    return graphicsLayer {
        alpha = anim.value.coerceIn(0f, 1f)
        translationX = (1f - anim.value) * -14.dp.toPx()
    }
}
