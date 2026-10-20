package com.example.githubmovil.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.githubmovil.presentation.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fondo vivo: tres "auroras" de color que orbitan lentamente detrás del contenido.
 * El color principal cambia con suavidad según la pantalla (verde al hacer merge, rojo al cerrar...).
 */
@Composable
fun AuroraBackground(accent: Color, content: @Composable BoxScope.() -> Unit) {
    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "auroraT"
    )
    val accentColor by animateColorAsState(accent, tween(900), label = "accent")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .drawBehind {
                val r = size.minDimension
                fun blob(color: Color, x: Float, y: Float, radius: Float, alpha: Float) {
                    val center = Offset(size.width * x, size.height * y)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(color.copy(alpha = alpha), Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
                blob(accentColor, 0.5f + 0.28f * cos(t), 0.28f + 0.14f * sin(t), r * 0.75f, 0.42f)
                blob(AppColors.Purple, 0.5f + 0.32f * cos(t + 2.1f), 0.72f + 0.12f * sin(2 * t), r * 0.6f, 0.30f)
                blob(AppColors.Pink, 0.5f + 0.30f * cos(t + 4.2f), 0.5f + 0.25f * sin(t + 1f), r * 0.45f, 0.18f)
                // Viñeta: oscurece los bordes para que encaje con la pantalla redonda.
                drawRect(
                    Brush.radialGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f),
                        center = center,
                        radius = r * 0.72f
                    )
                )
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Efecto cristal (glassmorphism): relleno translúcido + borde con brillo degradado. */
fun Modifier.glass(shape: Shape, tint: Color = AppColors.Glass): Modifier = this
    .background(tint, shape)
    .border(
        1.dp,
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.04f))),
        shape
    )

/** Entrada escalonada: cada elemento de una lista aparece deslizándose con un pequeño retraso. */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(6) * 70L)
        anim.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
    }
    return this.graphicsLayer {
        val v = anim.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 36.dp.toPx()
        scaleX = 0.88f + 0.12f * v
        scaleY = 0.88f + 0.12f * v
    }
}

/** Spinner doble con degradado: un arco gira en un sentido y otro en sentido contrario. */
@Composable
fun GradientSpinner(colors: List<Color>, size: Dp = 64.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "angle"
    )
    val sweep by transition.animateFloat(
        60f, 280f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sweep"
    )
    val pulse by transition.animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse"
    )
    Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.08f
        val gradient = Brush.sweepGradient(colors + colors.first())
        rotate(angle) {
            drawArc(gradient, 0f, sweep, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        val inset = stroke * 2.6f
        rotate(-angle * 1.4f) {
            drawArc(
                gradient,
                0f,
                120f,
                false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - inset * 2, this.size.height - inset * 2),
                style = Stroke(stroke * 0.6f, cap = StrokeCap.Round),
                alpha = 0.6f
            )
        }
        drawCircle(colors.first(), radius = stroke * 1.2f * pulse, alpha = pulse)
    }
}

/** Ondas tipo radar que se expanden y desvanecen detrás de un icono. */
@Composable
fun PulsingRings(color: Color, modifier: Modifier = Modifier, rings: Int = 3) {
    val progress by rememberInfiniteTransition(label = "rings").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "ringsP"
    )
    Canvas(modifier) {
        val maxR = size.minDimension / 2
        repeat(rings) { i ->
            val p = (progress + i / rings.toFloat()) % 1f
            drawCircle(
                color = color,
                radius = maxR * (0.35f + 0.65f * p),
                alpha = (1f - p) * 0.55f,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/** Anillo en el borde de la pantalla redonda (bisel) que muestra un progreso. */
@Composable
fun BezelProgress(progress: Float, colors: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(Color.White.copy(alpha = 0.06f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        rotate(-90f) {
            drawArc(
                Brush.sweepGradient(colors + colors.first()),
                0f,
                360f * progress.coerceIn(0f, 1f),
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

/** Check que se "dibuja" solo: primero el círculo y luego la palomita, con explosión de partículas. */
@Composable
fun AnimatedCheckmark(colors: List<Color>, size: Dp = 84.dp) {
    val circle = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        circle.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        launch { burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        check.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
    }
    val particleColors = colors + AppColors.Blue + AppColors.Pink
    Canvas(Modifier.size(size * 1.7f)) {
        val c = center
        val r = size.toPx() / 2
        // Partículas
        if (burst.value > 0f && burst.value < 1f) {
            repeat(16) { i ->
                val a = (i / 16f) * 2 * PI.toFloat()
                val dist = r * (1f + 0.65f * burst.value)
                drawCircle(
                    particleColors[i % particleColors.size],
                    radius = 3.dp.toPx() * (1f - burst.value),
                    center = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist),
                    alpha = 1f - burst.value
                )
            }
        }
        // Halo
        drawCircle(
            Brush.radialGradient(listOf(colors.first().copy(alpha = 0.35f), Color.Transparent), c, r * 1.4f),
            radius = r * 1.4f * circle.value
        )
        // Círculo
        val stroke = 5.dp.toPx()
        rotate(-90f) {
            drawArc(
                Brush.sweepGradient(colors + colors.first()),
                0f,
                360f * circle.value,
                false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        // Palomita
        val path = Path().apply {
            moveTo(c.x - r * 0.42f, c.y + r * 0.02f)
            lineTo(c.x - r * 0.10f, c.y + r * 0.32f)
            lineTo(c.x + r * 0.45f, c.y - r * 0.30f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * check.value, partial, true)
        drawPath(partial, Color.White, style = Stroke(stroke * 1.2f, cap = StrokeCap.Round))
    }
}
