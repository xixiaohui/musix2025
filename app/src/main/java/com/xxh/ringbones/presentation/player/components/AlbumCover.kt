package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Default album cover size. */
private val ALBUM_COVER_SIZE = 280.dp
/** Album cover corner radius. */
private val ALBUM_COVER_RADIUS = 24.dp
/** Glow shadow elevation. */
private val ALBUM_COVER_ELEVATION = 24.dp

/** Gradient palette for the fallback album art when no cover image is available. */
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
 * Album cover display with Coil image loading and gradient fallback.
 *
 * When [coverImageUrl] is non-null, loads the image via Coil with crossfade.
 * Otherwise renders a deterministic gradient background with a music note icon.
 *
 * Supports SharedTransition via the [modifier] parameter — pass
 * `Modifier.sharedElement()` or `Modifier.sharedBounds()` here.
 *
 * @param coverImageUrl Remote or local image URL, or null for fallback
 * @param category Category name for deterministic gradient color selection
 * @param modifier External modifier (use for SharedTransition)
 * @param size Total cover size including glow
 */
@Composable
fun AlbumCover(
    coverImageUrl: String?,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = ALBUM_COVER_SIZE,
) {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme

    val gradientIndex = remember(category) {
        kotlin.math.abs(category.hashCode()) % FALLBACK_GRADIENTS.size
    }
    val gradient = remember(gradientIndex) {
        val colors = FALLBACK_GRADIENTS[gradientIndex]
        Brush.linearGradient(colors)
    }

    val glowColor = colorScheme.primary.copy(alpha = 0.2f)
    val shape = RoundedCornerShape(ALBUM_COVER_RADIUS)

    Box(
        modifier = modifier
            .size(size)
            .shadow(ALBUM_COVER_ELEVATION, shape, ambientColor = glowColor, spotColor = glowColor),
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
            // Fallback gradient with music icon
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
                    modifier = Modifier.size(96.dp),
                    tint = Color.White.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAlbumCoverWithImage() {
    androidx.compose.material3.MaterialTheme {
        AlbumCover(
            coverImageUrl = null,
            category = "Rock",
        )
    }
}