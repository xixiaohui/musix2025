package com.xxh.ringbones.presentation.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ReplayCircleFilled
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Collapsible panel with extra playback controls: repeat mode, shuffle,
 * playback speed, sleep timer, EQ preset, and A–B loop.
 *
 * Each control displays its current value as a small label below the icon.
 * The panel uses a frosted-glass surface with rounded corners.
 * All colors use explicit light-on-dark values for readability on the
 * always-dark immersive background.
 *
 * @param visible Whether the panel is expanded
 * @param repeatMode Current repeat mode
 * @param shuffleMode Whether shuffle is active
 * @param playbackSpeed Current speed multiplier
 * @param sleepTimerMinutes Current sleep timer minutes, or null
 * @param eqPreset Current EQ preset name
 * @param onRepeatClick Cycle repeat mode (OFF → ONE → ALL → OFF)
 * @param onShuffleClick Toggle shuffle
 * @param onSpeedClick Open speed selector
 * @param onTimerClick Open sleep timer dialog
 * @param onEqClick Open EQ preset selector
 * @param onAbLoopClick Start/continue A–B loop point setting
 * @param modifier External modifier
 */
@Composable
fun ExtraControls(
    visible: Boolean,
    repeatMode: String,
    shuffleMode: Boolean,
    playbackSpeed: Float,
    sleepTimerMinutes: Int?,
    eqPreset: String,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onTimerClick: () -> Unit,
    onEqClick: () -> Unit,
    onAbLoopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PlayerColors.glassSurface)
                .padding(16.dp, 12.dp, 16.dp, 12.dp),
        ) {
            // Row 1: Repeat, Shuffle, Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExtraControlItem(
                    icon = when (repeatMode) {
                        "ONE" -> Icons.Default.Replay
                        "ALL" -> Icons.Default.ReplayCircleFilled
                        else -> Icons.Default.Loop
                    },
                    label = repeatMode,
                    onClick = onRepeatClick,
                )
                ExtraControlItem(
                    icon = if (shuffleMode) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
                    label = "Shuffle",
                    onClick = onShuffleClick,
                    active = shuffleMode,
                )
                ExtraControlItem(
                    icon = Icons.Default.Speed,
                    label = "${playbackSpeed}x",
                    onClick = onSpeedClick,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Timer, EQ, A–B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExtraControlItem(
                    icon = Icons.Default.Timer,
                    label = sleepTimerMinutes?.let { "${it}min" } ?: "Timer",
                    onClick = onTimerClick,
                )
                ExtraControlItem(
                    icon = Icons.Default.Tune,
                    label = eqPreset,
                    onClick = onEqClick,
                )
                ExtraControlItem(
                    icon = Icons.Default.ContentCut,
                    label = "A-B",
                    onClick = onAbLoopClick,
                )
            }
        }
    }
}

/** Accent color used for active/primary-highlighted controls. */
private val accentColor = Color(0xFF7C85F5)

/**
 * Single extra control: icon button + small label.
 */
@Composable
private fun ExtraControlItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val tint = if (active) accentColor else PlayerColors.textSecondary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = tint,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (active) accentColor else PlayerColors.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewExtraControls() {
    MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
        ExtraControls(
            visible = true,
            repeatMode = "OFF",
            shuffleMode = false,
            playbackSpeed = 1.0f,
            sleepTimerMinutes = null,
            eqPreset = "FLAT",
            onRepeatClick = {},
            onShuffleClick = {},
            onSpeedClick = {},
            onTimerClick = {},
            onEqClick = {},
            onAbLoopClick = {},
        )
    }
}