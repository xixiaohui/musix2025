package com.xxh.ringbones.presentation.download.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.core.download.DownloadStatus
import com.xxh.ringbones.core.download.DownloadTask

/**
 * A single row in the download list, showing the task title, status,
 * progress bar (if active), and contextual action buttons.
 *
 * Completed items show play and delete buttons.
 */
@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit = {},
    onPlay: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (task.status) {
                DownloadStatus.Downloading -> {
                    TextButton(onClick = onPause) {
                        Text("Pause", style = MaterialTheme.typography.labelMedium)
                    }
                }
                DownloadStatus.Paused -> {
                    TextButton(onClick = onResume) {
                        Text("Resume", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DownloadStatus.Pending -> {
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DownloadStatus.Failed -> {
                    IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DownloadStatus.Completed -> {
                    IconButton(onClick = onPlay, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        if (task.status == DownloadStatus.Downloading) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            if (task.totalBytes > 0) {
                Text(
                    text = "${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }

        if (task.status == DownloadStatus.Failed && task.errorMessage != null) {
            Text(
                text = task.errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun statusText(task: DownloadTask): String = when (task.status) {
    DownloadStatus.Pending -> "Queued"
    DownloadStatus.Downloading -> if (task.totalBytes > 0) {
        "${(task.progress * 100).toInt()}%"
    } else {
        "Downloading..."
    }
    DownloadStatus.Paused -> "Paused"
    DownloadStatus.Completed -> "Downloaded"
    DownloadStatus.Failed -> "Failed"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes.toFloat() / 1_048_576)
    bytes >= 1_024 -> "%.0f KB".format(bytes.toFloat() / 1_024)
    else -> "$bytes B"
}

@Preview(showBackground = true)
@Composable
private fun PreviewDownloadTaskItem() {
    MaterialTheme {
        DownloadTaskItem(
            task = DownloadTask(
                ringtoneId = 1,
                title = "Sample Ringtone",
                url = "https://example.com/ringtone.mp3",
                status = DownloadStatus.Downloading,
                progress = 0.65f,
                downloadedBytes = 1_500_000,
                totalBytes = 2_300_000,
            ),
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
        )
    }
}
