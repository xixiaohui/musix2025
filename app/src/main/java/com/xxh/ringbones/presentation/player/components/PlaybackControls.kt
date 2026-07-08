package com.xxh.ringbones.presentation.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Play/pause button size. */
private val PLAY_BUTTON_SIZE = 64.dp
/** Previous/next button size. */
private val SIDE_BUTTON_SIZE = 48.dp
/** Previous/next icon size. */
private val SIDE_ICON_SIZE = 32.dp
/** Play/pause icon size. */
private val PLAY_ICON_SIZE = 36.dp

/** Accent color for the play/pause button background. */
private val accentColor = Color(0xFF7C85F5)

/**
 * Circular playback control buttons: previous, play/pause (largest), and next.
 *
 * The play/pause icon crossfades via [AnimatedContent] for smooth transition.
 * All colors use explicit light-on-dark values for readability on the
 * always-dark immersive background.
 *
 * @param isPlaying Whether the player is currently playing
 * @param onPrevious Callback for previous track / restart current
 * @param onPlayPause Callback for play/pause toggle
 * @param onNext Callback for next track
 * @param modifier External modifier
 */
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Previous button
        FilledIconButton(
            onClick = onPrevious,
            modifier = Modifier
                .size(SIDE_BUTTON_SIZE)
                .clip(CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = PlayerColors.glassSurface,
                contentColor = PlayerColors.textPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(SIDE_ICON_SIZE),
            )
        }

        // Play/Pause button (largest)
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(PLAY_BUTTON_SIZE)
                .clip(CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = accentColor,
                contentColor = Color.White,
            ),
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "playPauseIcon",
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(PLAY_ICON_SIZE),
                )
            }
        }

        // Next button
        FilledIconButton(
            onClick = onNext,
            modifier = Modifier
                .size(SIDE_BUTTON_SIZE)
                .clip(CircleShape),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = PlayerColors.glassSurface,
                contentColor = PlayerColors.textPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(SIDE_ICON_SIZE),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPlaybackControlsPlaying() {
    MaterialTheme {
        ImmersiveBackground(coverImageUrl = null)
        PlaybackControls(
            isPlaying = true,
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPlaybackControlsPaused() {
    MaterialTheme {
        ImmersiveBackground(coverImageUrl = null)
        PlaybackControls(
            isPlaying = false,
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
        )
    }
}