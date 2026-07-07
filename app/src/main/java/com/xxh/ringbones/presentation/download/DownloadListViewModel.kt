package com.xxh.ringbones.presentation.download

import androidx.lifecycle.ViewModel
import com.xxh.ringbones.core.download.DownloadManager
import com.xxh.ringbones.core.download.DownloadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for the Download List screen.
 *
 * Exposes the [DownloadManager.state] directly — the manager is the single
 * source of truth for all download state. All actions delegate to the manager.
 */
@HiltViewModel
class DownloadListViewModel @Inject constructor(
    private val downloadManager: DownloadManager,
) : ViewModel() {

    /** Current download queue state, updated in real-time. */
    val state: StateFlow<DownloadState> = downloadManager.state

    /** Pause an active download. */
    fun pause(ringtoneId: Long) = downloadManager.pause(ringtoneId)

    /** Resume a paused download. */
    fun resume(ringtoneId: Long) = downloadManager.resume(ringtoneId)

    /** Cancel a pending or paused download. */
    fun cancel(ringtoneId: Long) = downloadManager.cancel(ringtoneId)

    /** Retry a failed download. */
    fun retry(ringtoneId: Long) = downloadManager.retry(ringtoneId)

    /** Remove all completed and failed entries from the list. */
    fun clearCompleted() = downloadManager.clearCompleted()

    /** Remove a single task from the queue regardless of its status. */
    fun remove(ringtoneId: Long) = downloadManager.remove(ringtoneId)
}
