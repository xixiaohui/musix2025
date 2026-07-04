package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Accent color for favorited heart icon. */
private val favoriteColor = Color(0xFFFF6B9D)

/**
 * Small icon row with favorite toggle and queue access.
 *
 * All colors use explicit light-on-dark values for readability on the
 * always-dark immersive background.
 *
 * @param isFavorite Whether the current track is favorited
 * @param onToggleFavorite Callback for favorite toggle
 * @param onQueue Callback to open the queue bottom sheet
 * @param modifier External modifier
 */
@Composable
fun FavoriteRow(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Favorite toggle
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) favoriteColor else PlayerColors.textDisabled,
                modifier = Modifier.size(22.dp),
            )
        }

        // Queue
        @Suppress("DEPRECATION")
        IconButton(onClick = onQueue) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = PlayerColors.textDisabled,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavoriteRow() {
    MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
        FavoriteRow(
            isFavorite = true,
            onToggleFavorite = {},
            onQueue = {},
        )
    }
}
