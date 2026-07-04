package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Allowed playback speed values. */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

/**
 * Playback speed selector grid.
 *
 * Displays speed options as selectable chips/buttons. The currently active
 * speed is highlighted with the primary color.
 *
 * @param currentSpeed Currently selected speed multiplier
 * @param onSpeedSelected Callback with the selected speed
 * @param modifier External modifier
 */
@Composable
fun SpeedSelector(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.padding(16.dp, 0.dp, 16.dp, 0.dp)) {
        Text(
            text = "Playback Speed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Speed buttons in two rows
        val rows = PLAYBACK_SPEEDS.chunked(4)
        for (rowSpeeds in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (speed in rowSpeeds) {
                    val isSelected = speed == currentSpeed
                    FilledTonalButton(
                        onClick = { onSpeedSelected(speed) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isSelected) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary,
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.surface.copy(alpha = 0.12f),
                                contentColor = colorScheme.onSurface,
                            )
                        },
                    ) {
                        Text(text = "${speed}x")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSpeedSelector() {
    MaterialTheme {
        SpeedSelector(currentSpeed = 1.0f, onSpeedSelected = {})
    }
}