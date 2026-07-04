package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass card wrapping [TrackInfo] and [CustomSeekBar].
 *
 * Provides a unified rounded container with light glass surface
 * that sits above the always-dark immersive background.
 *
 * @param title Track title
 * @param artist Artist/author name
 * @param trackKey Unique key for crossfade animation on track change
 * @param progress Current playback position in milliseconds
 * @param duration Total track duration in milliseconds
 * @param onSeek Callback with absolute seek position in milliseconds
 * @param modifier External modifier
 */
@Composable
fun TrackInfoCard(
    title: String,
    artist: String,
    trackKey: Long,
    progress: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 0.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PlayerColors.glassSurface)
            .padding(20.dp, 16.dp, 20.dp, 16.dp),
    ) {
        TrackInfo(
            title = title,
            artist = artist,
            trackKey = trackKey,
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomSeekBar(
            progress = progress,
            duration = duration,
            onSeek = onSeek,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrackInfoCard() {
    MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
        TrackInfoCard(
            title = "Bohemian Rhapsody",
            artist = "Queen",
            trackKey = 1L,
            progress = 75_000L,
            duration = 240_000L,
            onSeek = {},
        )
    }
}