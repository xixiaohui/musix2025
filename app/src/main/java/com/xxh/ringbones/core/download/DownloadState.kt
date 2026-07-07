package com.xxh.ringbones.core.download

/**
 * Global snapshot of the download queue state, emitted by [DownloadManager.state].
 *
 * @property tasks All tasks in the queue (pending, active, completed, failed)
 * @property activeCount Number of tasks currently in [DownloadStatus.Downloading] state
 * @property pendingCount Number of tasks in [DownloadStatus.Pending] state
 */
data class DownloadState(
    val tasks: List<DownloadTask> = emptyList(),
    val activeCount: Int = 0,
    val pendingCount: Int = 0,
)