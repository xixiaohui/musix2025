package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Track line thickness. */
private val TRACK_HEIGHT = 3.dp
/** Thumb circle radius. */
private val THUMB_RADIUS = 6.dp
/** Touchable area height for drag and tap gestures. */
private val TOUCH_AREA_HEIGHT = 48.dp
/** Thumb outline stroke width. */
private const val THUMB_STROKE_WIDTH = 2f

/** Accent color for the played track portion and thumb. */
private val accentColor = Color(0xFF7C85F5)

/**
 * Custom seek bar drawn on Canvas with draggable thumb and tap-to-seek.
 *
 * Uses [detectHorizontalDragGestures] for precise seeking and
 * [detectTapGestures] for instant position jumps. A local [dragFraction]
 * tracks the thumb position during drag so the UI stays responsive
 * without waiting for ViewModel recomposition.
 *
 * All colors use explicit light-on-dark values for readability on the
 * always-dark immersive background.
 *
 * @param progress Current playback position in milliseconds
 * @param duration Total track duration in milliseconds
 * @param onSeek Callback with the absolute seek position in milliseconds
 * @param modifier External modifier
 */
@Composable
fun CustomSeekBar(
    progress: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Actual playback fraction (0..1) from the engine
    val playbackFraction = if (duration > 0) {
        (progress.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Local drag fraction — set to -1f when not dragging (means "use playbackFraction")
    var dragFraction by remember { mutableFloatStateOf(-1f) }

    // Which fraction to render: local during drag, playback otherwise
    val displayFraction = if (dragFraction >= 0f) dragFraction else playbackFraction

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TOUCH_AREA_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TOUCH_AREA_HEIGHT)
                    .pointerInput(duration) {
                        if (duration <= 0L) return@pointerInput

                        // Tap to seek to absolute position
                        detectTapGestures { tapOffset ->
                            val tappedFraction = (tapOffset.x / size.width.toFloat())
                                .coerceIn(0f, 1f)
                            val seekMs = (tappedFraction * duration).toLong()
                            onSeek(seekMs)
                        }
                    }
                    .pointerInput(duration) {
                        if (duration <= 0L) return@pointerInput

                        // Drag to scrub through the track
                        detectHorizontalDragGestures(
                            onDragStart = { startOffset ->
                                dragFraction = (startOffset.x / size.width.toFloat())
                                    .coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                val finalMs = (dragFraction * duration).toLong()
                                onSeek(finalMs)
                                dragFraction = -1f
                            },
                            onDragCancel = {
                                dragFraction = -1f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val deltaFraction = dragAmount / size.width.toFloat()
                                dragFraction = (dragFraction + deltaFraction).coerceIn(0f, 1f)
                            },
                        )
                    },
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val trackY = canvasHeight / 2f
                val trackStartX = THUMB_RADIUS.toPx()
                val trackEndX = canvasWidth - THUMB_RADIUS.toPx()
                val trackLength = trackEndX - trackStartX

                // Background (unplayed) track
                drawRoundRect(
                    color = PlayerColors.trackBackground,
                    topLeft = Offset(trackStartX, trackY - TRACK_HEIGHT.toPx() / 2f),
                    size = Size(trackLength, TRACK_HEIGHT.toPx()),
                    cornerRadius = CornerRadius(TRACK_HEIGHT.toPx() / 2f),
                )

                // Played portion
                val playedWidth = trackLength * displayFraction
                if (playedWidth > 0f) {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(trackStartX, trackY - TRACK_HEIGHT.toPx() / 2f),
                        size = Size(playedWidth, TRACK_HEIGHT.toPx()),
                        cornerRadius = CornerRadius(TRACK_HEIGHT.toPx() / 2f),
                    )
                }

                // Thumb circle
                val thumbX = trackStartX + playedWidth
                drawCircle(
                    color = accentColor,
                    radius = THUMB_RADIUS.toPx(),
                    center = Offset(thumbX, trackY),
                )
                drawCircle(
                    color = Color.White,
                    radius = THUMB_RADIUS.toPx(),
                    center = Offset(thumbX, trackY),
                    style = Stroke(width = THUMB_STROKE_WIDTH),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Time labels
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatTimeMs(progress),
                style = MaterialTheme.typography.labelSmall,
                color = PlayerColors.textMuted,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatTimeMs(duration),
                style = MaterialTheme.typography.labelSmall,
                color = PlayerColors.textMuted,
            )
        }
    }
}

/**
 * Formats milliseconds to `mm:ss` or `hh:mm:ss`.
 */
private fun formatTimeMs(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSeekBar() {
    MaterialTheme {
        ImmersiveBackground(coverImageUrl = null)
        CustomSeekBar(
            progress = 75_000,
            duration = 240_000,
            onSeek = {},
            modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 0.dp),
        )
    }
}