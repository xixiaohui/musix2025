package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Album cover size — large and immersive. */
private val ALBUM_COVER_SIZE = 340.dp

/** Album cover corner radius. */
private val ALBUM_COVER_RADIUS = 28.dp

/** Glow shadow elevation for depth. */
private val ALBUM_COVER_ELEVATION = 32.dp

/** Glow blur radius behind the cover. */
private val GLOW_SIZE = 300.dp

/** Gradient palette for fallback album art. */
private val FALLBACK_GRADIENTS = listOf(
    listOf(Color(0xFF7F4D7A), Color(0xFFD4A0D0)),
    listOf(Color(0xFF3A608F), Color(0xFF7AACF0)),
    listOf(Color(0xFF825246), Color(0xFFD49480)),
    listOf(Color(0xFF2E5A3E), Color(0xFF6EBE82)),
    listOf(Color(0xFF8E4A2C), Color(0xFFE08860)),
    listOf(Color(0xFF4A4080), Color(0xFF8E80E0)),
    listOf(Color(0xFF2E6B5E), Color(0xFF5CC8B0)),
    listOf(Color(0xFF8C3A4A), Color(0xFFE06880)),
)

/**
 * Large album cover with an ambient glow halo behind it for depth
 * and a premium glassmorphism feel.
 *
 * @param coverImageUrl Remote or local image URL, or null for fallback
 * @param category      Category for deterministic gradient fallback
 * @param modifier      External modifier (use for SharedTransition)
 * @param size          Total cover size including glow
 */
@Composable
fun AlbumCover(
    coverImageUrl: String?,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = ALBUM_COVER_SIZE,
) {
    val gradientIndex = remember(category) {
        kotlin.math.abs(category.hashCode()) % FALLBACK_GRADIENTS.size
    }
    val gradient = remember(gradientIndex) {
        val colors = FALLBACK_GRADIENTS[gradientIndex]
        Brush.linearGradient(colors)
    }
    val glowColor = Color(0xFFB4A0FF).copy(alpha = 0.12f)
    val shape = RoundedCornerShape(ALBUM_COVER_RADIUS)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient glow halo behind the cover
        Box(
            modifier = Modifier
                .size(GLOW_SIZE)
                .offset(y = (-8).dp)
                .blur(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor,
                            glowColor.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                    shape = RoundedCornerShape(50),
                ),
        )

        // Main cover with shadow
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    ALBUM_COVER_ELEVATION,
                    shape,
                    ambientColor = glowColor.copy(alpha = 0.3f),
                    spotColor = glowColor.copy(alpha = 0.15f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (coverImageUrl != null) {
                AsyncImage(
                    model = coverImageUrl,
                    contentDescription = "Album cover",
                    modifier = Modifier
                        .size(size)
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(shape)
                        .background(brush = gradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(108.dp),
                        tint = Color.White.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAlbumCover() {
    androidx.compose.material3.MaterialTheme {
        AlbumCover(
            coverImageUrl = null,
            category = "Electronic",
        )
    }
}
