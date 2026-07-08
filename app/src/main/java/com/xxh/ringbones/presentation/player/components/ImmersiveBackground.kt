package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Immersive player background with full-screen blurred album cover,
 * glassmorphism dark overlay, and ambient glow. Follows the
 * Apple Music × Tidal design language.
 *
 * @param coverImageUrl Album art URL for the blurred background
 * @param accentColor    Dynamic accent from album art for ambient glow
 * @param modifier       External modifier
 */
@Composable
fun ImmersiveBackground(
    coverImageUrl: String?,
    accentColor: Color = Color(0xFFB4A0FF),
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // ── Layer 1: Full-screen blurred album cover ──
        if (coverImageUrl != null) {
            AsyncImage(
                model = coverImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .blur(80.dp),
                contentScale = ContentScale.Crop,
            )
        }

        // ── Layer 2: Dark glass overlay (gradient from top to bottom) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Deep dark gradient with subtle color from accent
            val topColor = accentColor.copy(alpha = 0.15f)
            val midColor = Color(0xFF080812).copy(alpha = 0.75f)
            val bottomColor = Color(0xFF060610).copy(alpha = 0.95f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, midColor, bottomColor),
                ),
            )
        }

        // ── Layer 3: Ambient glow behind cover area ──
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .size(360.dp)
                .blur(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.30f),
                            accentColor.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // ── Layer 4: Subtle noise texture ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color.White.copy(alpha = 0.008f)
            val step = 24f
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = 0.8f, center = Offset(x, y))
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
    ImmersiveBackground(
        coverImageUrl = null,
        accentColor = Color(0xFFB4A0FF),
    )
}
