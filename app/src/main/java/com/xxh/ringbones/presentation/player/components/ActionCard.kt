package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Button row height. */
private val ACTION_ROW_HEIGHT = 52.dp

/**
 * Frosted-glass card with two vertically-stacked action buttons:
 * "Set Ringtone" and "Download".
 *
 * Each row provides a ripple touch target with icon + label.
 * The download row shows a "Downloaded" state when [isDownloaded] is true.
 *
 * @param onSetRingtone Callback when the Set Ringtone row is tapped
 * @param onDownload Callback when the Download row is tapped
 * @param isDownloaded Whether the current track is already downloaded locally
 * @param modifier External modifier
 */
@Composable
fun ActionCard(
    onSetRingtone: () -> Unit,
    onDownload: () -> Unit,
    isDownloaded: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 0.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PlayerColors.glassSurface),
    ) {
        // Set Ringtone row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ACTION_ROW_HEIGHT)
                .clickable(onClick = onSetRingtone)
                .padding(16.dp, 0.dp, 16.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF7C85F5),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Set Ringtone",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = PlayerColors.textPrimary,
            )
        }

        HorizontalDivider(color = PlayerColors.glassLight)

        // Download row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ACTION_ROW_HEIGHT)
                .clickable(onClick = onDownload)
                .padding(16.dp, 0.dp, 16.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDownloaded) Color(0xFF4CAF50) else Color(0xFF7C85F5),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isDownloaded) "Downloaded" else "Download",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDownloaded) Color(0xFF4CAF50) else PlayerColors.textPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionCard() {
    MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
        ActionCard(
            onSetRingtone = {},
            onDownload = {},
            isDownloaded = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionCardDownloaded() {
    MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
        ActionCard(
            onSetRingtone = {},
            onDownload = {},
            isDownloaded = true,
        )
    }
}