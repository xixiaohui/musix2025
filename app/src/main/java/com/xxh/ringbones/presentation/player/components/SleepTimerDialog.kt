package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Sleep timer options in minutes. */
private val TIMER_OPTIONS = listOf(
    15 to "15 minutes",
    30 to "30 minutes",
    60 to "1 hour",
    -1 to "Turn off",
)

/**
 * Sleep timer picker dialog.
 *
 * Offers preset durations (15min, 30min, 60min) and a "Turn off" option.
 * Selecting a duration starts the countdown; selecting "Turn off" cancels it.
 *
 * @param currentMinutes Current timer value in minutes, or null if inactive
 * @param onDismiss Callback to close the dialog
 * @param onSelected Callback with the selected duration in minutes, or null to cancel
 */
@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onSelected: (Int?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = if (currentMinutes != null) {
                        "Timer active: ${currentMinutes} min remaining"
                    } else {
                        "Select duration:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                for ((minutes, label) in TIMER_OPTIONS) {
                    val isActive = if (minutes == -1) currentMinutes == null
                    else currentMinutes == minutes

                    TextButton(
                        onClick = {
                            onSelected(if (minutes == -1) null else minutes)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewSleepTimerDialog() {
    MaterialTheme {
        SleepTimerDialog(
            currentMinutes = null,
            onDismiss = {},
            onSelected = {},
        )
    }
}