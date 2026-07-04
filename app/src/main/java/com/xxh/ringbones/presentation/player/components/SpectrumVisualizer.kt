package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default visualizer height. */
private val VISUALIZER_HEIGHT = 48.dp
/** Gap between bars in dp. */
private val BAR_GAP = 1.dp
/** Minimum bar height fraction (prevents invisible bars). */
private const val MIN_BAR_FRACTION = 0.02f

/** Accent color for the upper portion of spectrum bars. */
private val barTopColor = Color(0xFF7C85F5).copy(alpha = 0.8f)

/** Muted color for the lower portion of spectrum bars. */
private val barBottomColor = Color.White.copy(alpha = 0.12f)

/**
 * Real-time FFT spectrum visualizer drawn on Canvas.
 *
 * Renders vertical bars from [magnitudes] data. Bars are colored with a
 * vertical gradient from muted white at the bottom to accent at the top.
 * Heights animate smoothly as data changes.
 *
 * Uses explicit light-on-dark colors for visibility on the always-dark
 * immersive background.
 *
 * @param magnitudes Normalized FFT magnitudes (0f–1f), typically 128 bins
 * @param modifier External modifier
 * @param height Height of the visualizer bar area
 */
@Composable
fun SpectrumVisualizer(
    magnitudes: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = VISUALIZER_HEIGHT,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (magnitudes.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val barGapPx = BAR_GAP.toPx()
        val totalBars = magnitudes.size
        val barWidth = (canvasWidth - barGapPx * (totalBars - 1)) / totalBars

        if (barWidth <= 0f) return@Canvas

        magnitudes.forEachIndexed { index, magnitude ->
            val barHeight = canvasHeight * magnitude.coerceIn(0f, 1f).coerceAtLeast(MIN_BAR_FRACTION)
            val x = index * (barWidth + barGapPx)
            val y = canvasHeight - barHeight

            // Vertical gradient per bar: bottom muted → top accent
            val barBrush = Brush.verticalGradient(
                colors = listOf(barBottomColor, barTopColor, barTopColor),
                startY = canvasHeight,
                endY = y,
            )

            drawRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSpectrumVisualizer() {
    val previewData = List(64) {
        kotlin.math.sin(it * 0.1f).coerceIn(0f, 1f) * (0.3f + 0.5f * kotlin.math.abs(kotlin.math.cos(it * 0.2f)))
    }

    androidx.compose.material3.MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
        SpectrumVisualizer(magnitudes = previewData)
    }
}
