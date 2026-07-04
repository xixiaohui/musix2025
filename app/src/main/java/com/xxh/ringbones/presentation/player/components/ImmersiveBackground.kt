package com.xxh.ringbones.presentation.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/** Duration for color transition animation in milliseconds. */
private const val COLOR_TRANSITION_MS = 1500

/** Predefined deep-dark color palettes for the animated background. */
private val BG_PALETTES = listOf(
    listOf(Color(0xFF0A0A0F), Color(0xFF1A1040), Color(0xFF0D1B2A)),
    listOf(Color(0xFF0A0A0F), Color(0xFF1F0A2E), Color(0xFF0A1628)),
    listOf(Color(0xFF0A0A0F), Color(0xFF16203E), Color(0xFF1A0A1A)),
    listOf(Color(0xFF0A0A0F), Color(0xFF1A2A2E), Color(0xFF0A1A2A)),
    listOf(Color(0xFF0A0A0F), Color(0xFF2A0A1E), Color(0xFF1A1A1A)),
)

/**
 * Deep-dark immersive background with an animated radial gradient glow
 * and subtle canvas-drawn texture.
 *
 * Colors transition smoothly when the [paletteIndex] changes, creating
 * a dynamic living-background effect that complements the album art.
 *
 * @param paletteIndex Index into preset color palettes (wraps around)
 * @param modifier External modifier
 */
@Composable
fun ImmersiveBackground(
    paletteIndex: Int,
    modifier: Modifier = Modifier,
) {
    val palette = BG_PALETTES[paletteIndex % BG_PALETTES.size]

    val baseColor by animateColorAsState(
        targetValue = palette[0],
        animationSpec = tween(COLOR_TRANSITION_MS),
        label = "bgBase",
    )
    val glowColor by animateColorAsState(
        targetValue = palette[1],
        animationSpec = tween(COLOR_TRANSITION_MS),
        label = "bgGlow",
    )
    val accentColor by animateColorAsState(
        targetValue = palette[2],
        animationSpec = tween(COLOR_TRANSITION_MS),
        label = "bgAccent",
    )

    val gradientBrush = Brush.radialGradient(
        colors = listOf(glowColor, baseColor, accentColor),
        center = Offset(600f, 300f),
        radius = 1200f,
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Animated gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = gradientBrush)
        }

        // Subtle noise-like dot pattern for texture
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color.White.copy(alpha = 0.015f)
            val step = 32f
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(
                        color = dotColor,
                        radius = 1f,
                        center = Offset(x, y),
                    )
                    y += step
                }
                x += step
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewImmersiveBackground() {
    androidx.compose.material3.MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
    }
}