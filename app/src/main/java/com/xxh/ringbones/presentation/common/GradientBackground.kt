package com.xxh.ringbones.presentation.common

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Duration for one full gradient cycle (milliseconds). */
private const val GRADIENT_CYCLE_MS = 8000

/**
 * Dynamic gradient background that slowly shifts colors over time.
 *
 * Uses [rememberInfiniteTransition] to animate the vertical gradient offsets,
 * creating a subtle breathing effect suitable for headers and backgrounds.
 *
 * @param modifier External modifier
 * @param colors List of gradient colors (defaults to 3)
 * @param content Optional content overlay
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460)
    ),
    content: @Composable () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "gradientBg")
    val offsetY by animateGradientOffset(transition)

    val animatedBrush = Brush.verticalGradient(
        colors = colors,
        startY = offsetY,
        endY = offsetY + 600f
    )

    Box(
        modifier = modifier.background(brush = animatedBrush)
    ) {
        content()
    }
}

/**
 * Creates an infinite repeating float animation for gradient Y-offset.
 */
@Composable
private fun animateGradientOffset(transition: InfiniteTransition) =
    transition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(GRADIENT_CYCLE_MS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )