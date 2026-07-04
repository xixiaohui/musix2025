package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Action icon size. */
private val ACTION_ICON_SIZE = 28.dp

/**
 * Action icon row: download, toggle favorite, and set as ringtone.
 *
 * Each button uses an [IconButton] with the surface color scheme for
 * consistent touch feedback. The favorite icon toggles between filled
 * and outlined states.
 *
 * @param isFavorite Whether the current track is favorited
 * @param onDownload Callback for download action
 * @param onToggleFavorite Callback for favorite toggle
 * @param onSetRingtone Callback for set-as-ringtone action
 * @param modifier External modifier
 */
@Composable
fun ActionRow(
    isFavorite: Boolean,
    onDownload: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetRingtone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Download
        IconButton(onClick = onDownload) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                modifier = Modifier.size(ACTION_ICON_SIZE),
                tint = colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }

        // Favorite (toggle)
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                modifier = Modifier.size(ACTION_ICON_SIZE),
                tint = if (isFavorite) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }

        // Set ringtone
        IconButton(onClick = onSetRingtone) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Set as ringtone",
                modifier = Modifier.size(ACTION_ICON_SIZE),
                tint = colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionRow() {
    MaterialTheme {
        ActionRow(
            isFavorite = true,
            onDownload = {},
            onToggleFavorite = {},
            onSetRingtone = {},
        )
    }
}