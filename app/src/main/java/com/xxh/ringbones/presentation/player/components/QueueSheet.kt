package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.domain.model.Ringtone
import coil3.compose.AsyncImage

/** Swipe threshold fraction for dismiss. */
private const val SWIPE_DISMISS_THRESHOLD = 0.5f

/** Red accent color for delete background. */
private val deleteBackgroundColor = Color(0xFFE53935)

/**
 * Bottom sheet displaying the current play queue with swipe-to-dismiss support.
 *
 * Shows each track with its cover thumbnail (or music icon fallback),
 * title, and artist. The currently playing track is highlighted with
 * a primary color accent and a play icon. Tapping a track skips to it.
 * Swiping a track from right to left removes it from the queue with
 * haptic feedback.
 *
 * @param queue The current queue of ringtones
 * @param currentIndex Index of the currently playing track
 * @param onDismiss Callback to close the sheet
 * @param onSkipTo Callback to skip to a specific track index
 * @param onRemove Callback to remove a track at a specific index
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Ringtone>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface.copy(alpha = 0.95f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 8.dp, 0.dp, 32.dp),
        ) {
            Text(
                text = "Queue (${queue.size})",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 12.dp),
            )

            LazyColumn {
                itemsIndexed(
                    items = queue,
                    key = { index, ringtone -> ringtone.id.toString() + "_" + index }
                ) { index, ringtone ->
                    SwipeableQueueItem(
                        ringtone = ringtone,
                        isCurrent = index == currentIndex,
                        onClick = { onSkipTo(index) },
                        onDismissed = { onRemove(index) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableQueueItem(
    ringtone: Ringtone,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDismissed: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismissed()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * SWIPE_DISMISS_THRESHOLD },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(deleteBackgroundColor),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from queue",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        QueueItemContent(
            ringtone = ringtone,
            isCurrent = isCurrent,
            onClick = onClick,
        )
    }
}

@Composable
private fun QueueItemContent(
    ringtone: Ringtone,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = if (isCurrent) {
        colorScheme.primary.copy(alpha = 0.12f)
    } else {
        colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(12.dp, 8.dp, 12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surface.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            if (ringtone.coverImageUrl != null) {
                AsyncImage(
                    model = ringtone.coverImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp, 0.dp, 8.dp, 0.dp),
        ) {
            Text(
                text = ringtone.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ringtone.author,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isCurrent) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Now playing",
                modifier = Modifier.size(20.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewQueueSheet() {
    MaterialTheme {
        QueueSheet(
            queue = emptyList(),
            currentIndex = 0,
            onDismiss = {},
            onSkipTo = {},
            onRemove = {},
        )
    }
}
