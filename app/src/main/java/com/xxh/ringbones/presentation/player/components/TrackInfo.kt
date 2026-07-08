package com.xxh.ringbones.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview

/**
 * Displays current track title and artist name with explicit light-on-dark
 * colors for readability on the always-dark immersive background.
 *
 * The title uses marquee-style ellipsis when overflowing (single line).
 * Both title and artist animate with a crossfade on track change via
 * [AnimatedVisibility] keyed by [trackKey].
 *
 * @param title Track title
 * @param artist Artist/author name
 * @param trackKey Unique key that triggers crossfade on change (e.g., ringtone ID)
 * @param modifier External modifier
 */
@Composable
fun TrackInfo(
    title: String,
    artist: String,
    trackKey: Long,
    modifier: Modifier = Modifier,
) {
    key(trackKey) {
        AnimatedVisibility(
            visible = title.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PlayerColors.textPrimary,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PlayerColors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrackInfo() {
    MaterialTheme {
        ImmersiveBackground(coverImageUrl = null)
        TrackInfo(
            title = "Bohemian Rhapsody",
            artist = "Queen",
            trackKey = 1,
        )
    }
}