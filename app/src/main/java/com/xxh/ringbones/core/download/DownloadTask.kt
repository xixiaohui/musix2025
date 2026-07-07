package com.xxh.ringbones.core.download

/**
 * Represents the state of a single download in the [DownloadManager] queue.
 *
 * @property ringtoneId The Ringtone ID from the database
 * @property title Display title for the download UI
 * @property url Remote MP3 URL to download
 * @property status Current lifecycle status
 * @property progress Download progress from 0f to 1f
 * @property downloadedBytes Bytes downloaded so far
 * @property totalBytes Total expected bytes (from Content-Length), 0 if unknown
 * @property errorMessage Human-readable error description when status is Failed
 */
data class DownloadTask(
    val ringtoneId: Long,
    val title: String,
    val url: String,
    val status: DownloadStatus = DownloadStatus.Pending,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val errorMessage: String? = null,
)

/**
 * Lifecycle status for a [DownloadTask].
 */
enum class DownloadStatus {
    /** Queued, waiting for an available download slot. */
    Pending,

    /** Actively downloading with progress updates. */
    Downloading,

    /** Paused by user action; can be resumed. */
    Paused,

    /** Successfully downloaded to local storage. */
    Completed,

    /** Download failed with an error; can be retried. */
    Failed,
}