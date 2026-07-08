package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

private val VISUALIZER_HEIGHT = 64.dp
private const val BAR_HALF_COUNT = 64
private val BAR_WIDTH = 2.5.dp
private val BAR_GAP = 2.dp
private const val MIN_BAR_FRACTION = 0.01f
private const val PEAK_DECAY = 0.92f

/**
 * Mirror-capsule spectrum with peak-hold glow dots.
 *
 * Bars: left-right mirrored, center = high freqs (warm), edges = low freqs (cool).
 * Peaks: slowly-decaying white dots at each bar's historical maximum.
 *
 * Data smoothing (EMA) and noise-injected dummy signal are handled upstream
 * in [com.xxh.ringbones.core.player.PlaybackServiceCallback].
 */
@Composable
fun SpectrumVisualizer(
    magnitudes: List<Float>,
    accentColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = VISUALIZER_HEIGHT,
) {
    // Build mirrored target values
    val targets: List<Float> = if (magnitudes.isNotEmpty()) {
        val half = magnitudes.size.coerceAtMost(BAR_HALF_COUNT)
        magnitudes.take(half).reversed() + magnitudes.take(half)
    } else {
        // Fallback: static center-weighted hump so the area is never blank
        List(BAR_HALF_COUNT * 2) { i ->
            val d = abs(i - BAR_HALF_COUNT + 0.5f) / BAR_HALF_COUNT
            ((1f - d) * 0.5f).coerceIn(0f, 1f)
        }
    }.map { it.coerceIn(0f, 1f) }

    val amplitude = if (isPlaying) 1f else 0.03f
    val totalBars = targets.size

    // Per-bar peak tracker — persists across frames.
    // Always 2 * BAR_HALF_COUNT since the fallback guarantees this size.
    val peaks = remember { FloatArray(BAR_HALF_COUNT * 2) }

    // Update peaks
    for (i in 0 until totalBars.coerceAtMost(peaks.size)) {
        val h = targets[i] * amplitude
        if (h > peaks[i]) peaks[i] = h else peaks[i] *= PEAK_DECAY
    }

    val centerColor = Color.White
    val edgeColor = accentColor.copy(alpha = 0.35f)

    Canvas(
        modifier = modifier.fillMaxWidth().height(height),
    ) {
        if (totalBars == 0) return@Canvas

        val cw = size.width; val ch = size.height
        val bw = BAR_WIDTH.toPx(); val gap = BAR_GAP.toPx()
        val blockW = totalBars * (bw + gap) - gap
        val sx = (cw - blockW) / 2f
        val r = CornerRadius(bw * 0.5f, bw * 0.5f)

        // Ambient glow
        drawCircle(accentColor.copy(alpha = 0.10f), cw * 0.20f, Offset(cw / 2f, ch / 2f))

        for (i in 0 until totalBars) {
            val barH = (targets[i] * amplitude).coerceAtLeast(MIN_BAR_FRACTION)
            val peakH = peaks[i].coerceAtLeast(MIN_BAR_FRACTION)
            if (barH <= 0f && peakH <= 0f) continue

            val x = sx + i * (bw + gap)
            val dist = abs(i - totalBars / 2f) / (totalBars / 2f)
            val c = lerpColor(centerColor, edgeColor, sqrt(dist))

            // Bar gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(c.copy(alpha = 0.04f), c.copy(alpha = 0.85f)),
                    startY = ch, endY = ch * (1f - barH),
                ),
                topLeft = Offset(x, ch * (1f - barH)),
                size = Size(bw, ch * barH),
                cornerRadius = r,
            )

            // Peak glow dot
            if (peakH > barH + 0.02f) {
                val py = ch * (1f - peakH)
                drawCircle(c.copy(alpha = 0.30f), bw * 1.5f, Offset(x + bw / 2f, py))
                drawCircle(Color.White.copy(alpha = 0.65f), bw * 0.5f, Offset(x + bw / 2f, py))
            }
        }
    }
}

private fun lerpColor(a: Color, b: Color, f: Float): Color {
    val t = f.coerceIn(0f, 1f)
    return Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t,
        a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t)
}

@Preview(showBackground = true)
@Composable
private fun PreviewSpectrumVisualizer() {
    val d = List(64) { (1f - abs(it - 31.5f) / 31.5f) * 0.7f + 0.1f }
    androidx.compose.material3.MaterialTheme {
        SpectrumVisualizer(d, Color(0xFFB4A0FF), true)
    }
}
