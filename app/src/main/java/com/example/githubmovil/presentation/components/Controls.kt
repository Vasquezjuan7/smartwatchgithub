package com.example.githubmovil.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.example.githubmovil.presentation.theme.AppColors
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/** Clic con efecto "rebote": el elemento se encoge al pulsarlo y vuelve con un muelle. */
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "bounce"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Texto relleno con un degradado. */
@Composable
fun GradientText(
    text: String,
    colors: List<Color>,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = Brush.linearGradient(colors),
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    )
}

/** Botón píldora con degradado y un destello de luz que lo recorre en bucle. */
@Composable
fun GradientButton(
    text: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val haptic = LocalHapticFeedback.current
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        -1f, 2f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "shimmerX"
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(38.dp)
            .bounceClick {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .drawBehind {
                // Resplandor suave debajo del botón (capas cada vez más grandes y transparentes)
                repeat(3) { i ->
                    val grow = (i + 1) * 2.dp.toPx()
                    drawRoundRect(
                        Brush.horizontalGradient(colors.map { it.copy(alpha = 0.16f - i * 0.04f) }),
                        topLeft = Offset(-grow, -grow + 2.dp.toPx()),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height)
                    )
                }
            }
            .clip(shape)
            .background(Brush.horizontalGradient(colors))
            .drawWithContent {
                drawContent()
                val x = size.width * shimmer
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        start = Offset(x - size.height, 0f),
                        end = Offset(x + size.height, size.height)
                    )
                )
            }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (icon != null) {
                Spacer(Modifier.width(6.dp))
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Botón secundario de cristal. */
@Composable
fun GlassButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .height(34.dp)
            .bounceClick {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .glass(RoundedCornerShape(50), AppColors.GlassStrong)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = AppColors.TextMain, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = AppColors.TextMain, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Botón "mantén pulsado para confirmar": un anillo se llena mientras mantienes el dedo.
 * Evita merges accidentales y es un patrón muy natural en relojes.
 */
@Composable
fun HoldToConfirmButton(
    icon: ImageVector,
    label: String,
    colors: List<Color>,
    onConfirmed: () -> Unit,
    size: Dp = 62.dp,
    holdMillis: Int = 1100,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    val currentOnConfirmed by rememberUpdatedState(onConfirmed)
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f, spring(dampingRatio = 0.4f, stiffness = 400f), label = "holdScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val fill = scope.launch {
                            progress.animateTo(
                                1f,
                                tween(((1f - progress.value) * holdMillis).toInt(), easing = LinearEasing)
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnConfirmed()
                        }
                        tryAwaitRelease()
                        pressed = false
                        if (progress.value < 1f) {
                            fill.cancel()
                            scope.launch { progress.animateTo(0f, tween(250)) }
                        }
                    })
                }
                .drawBehind {
                    val stroke = 4.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                    // Halo que crece al mantener pulsado
                    drawCircle(
                        Brush.radialGradient(
                            listOf(colors.first().copy(alpha = 0.25f + 0.45f * progress.value), Color.Transparent)
                        ),
                        radius = this.size.minDimension * (0.6f + 0.25f * progress.value)
                    )
                    drawArc(
                        Color.White.copy(alpha = 0.12f), 0f, 360f, false,
                        Offset(inset, inset), arcSize, style = Stroke(stroke)
                    )
                    rotate(-90f) {
                        drawArc(
                            Brush.sweepGradient(colors + colors.first()),
                            0f, 360f * progress.value, false,
                            Offset(inset, inset), arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size - 14.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(size * 0.4f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = AppColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

/** Avatar con la inicial del usuario sobre un degradado único generado a partir de su nombre. */
@Composable
fun Avatar(name: String, size: Dp = 26.dp) {
    val colors = colorsFor(name)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Par de colores estable para un texto (mismo repo/usuario = mismo color). */
fun colorsFor(key: String): List<Color> {
    val hue = (abs(key.hashCode()) % 360).toFloat()
    return listOf(
        Color.hsv(hue, 0.65f, 0.95f),
        Color.hsv((hue + 40f) % 360f, 0.75f, 0.85f)
    )
}

/** "hace 5 min", "hace 3 h", "hace 2 d". */
fun relativeTime(iso: String): String = runCatching {
    val d = Duration.between(Instant.parse(iso), Instant.now())
    when {
        d.toMinutes() < 1 -> "ahora"
        d.toMinutes() < 60 -> "hace ${d.toMinutes()} min"
        d.toHours() < 24 -> "hace ${d.toHours()} h"
        d.toDays() < 30 -> "hace ${d.toDays()} d"
        d.toDays() < 60 -> "hace 1 mes"
        d.toDays() < 365 -> "hace ${d.toDays() / 30} meses"
        d.toDays() < 730 -> "hace 1 año"
        else -> "hace ${d.toDays() / 365} años"
    }
}.getOrDefault("")
