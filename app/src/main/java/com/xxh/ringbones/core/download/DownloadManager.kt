package com.xxh.ringbones.core.download

import android.content.Context
import android.os.Environment
import com.xxh.ringbones.core.network.HttpClient
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Maximum number of concurrent downloads. */
private const val MAX_CONCURRENT_DOWNLOADS = 2

/** Buffer size for streaming download writes (8KB). */
private const val BUFFER_SIZE = 8192

/**
 * In-memory download queue manager with FIFO scheduling and max [MAX_CONCURRENT_DOWNLOADS]
 * concurrent OkHttp streaming downloads.
 *
 * State is exposed via [state] as a [StateFlow] for real-time UI updates. The queue
 * lives in process memory — if the app is killed, pending/active tasks are lost but
 * completed files remain on disk and their [Ringtone.downloadPath] is authoritative.
 *
 * Edge cases handled:
 * - Duplicate enqueue: skipped if already in queue with non-Failed status
 * - Already downloaded: skipped, task immediately marked Completed
 * - Network error: partial file deleted, task marked Failed with error message
 * - Disk full: IOException caught, task marked Failed
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
) {
    private val _state = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)

    /** Per-task download coroutine Jobs, keyed by ringtoneId. */
    private val activeJobs = mutableMapOf<Long, Job>()

    /** Collection point for OkHttp connections; reuse from AppModule. */
    private val httpClient = HttpClient.instance

    /**
     * Enqueue a ringtone for download. If already downloaded (per Room),
     * the task is immediately marked Completed. If already in queue with
     * a non-Failed status, this is a no-op.
     */
    fun enqueue(ringtone: Ringtone) {
        val current = _state.value.tasks

        // Skip if already downloaded
        if (ringtone.downloadPath != null && File(ringtone.downloadPath).exists()) {
            val completedTask = DownloadTask(
                ringtoneId = ringtone.id,
                title = ringtone.title,
                url = ringtone.url,
                status = DownloadStatus.Completed,
                progress = 1f,
            )
            _state.update { s ->
                s.copy(tasks = s.tasks + completedTask, activeCount = s.activeCount, pendingCount = s.pendingCount)
            }
            return
        }

        // Skip duplicates
        val existing = current.find { it.ringtoneId == ringtone.id }
        if (existing != null && existing.status != DownloadStatus.Failed) return

        // Remove old failed entry if retrying
        val filtered = current.filter { it.ringtoneId != ringtone.id }

        val task = DownloadTask(
            ringtoneId = ringtone.id,
            title = ringtone.title,
            url = ringtone.url,
        )
        _state.update { s ->
            s.copy(
                tasks = filtered + task,
                pendingCount = s.pendingCount + 1,
            )
        }

        // Kick off the scheduler
        scheduleNext()
    }

    /** Pause an actively downloading task. */
    fun pause(ringtoneId: Long) {
        activeJobs.remove(ringtoneId)?.cancel()
        updateTask(ringtoneId) { it.copy(status = DownloadStatus.Paused) }
        scheduleNext()
    }

    /** Resume a paused task. */
    fun resume(ringtoneId: Long) {
        updateTask(ringtoneId) { it.copy(status = DownloadStatus.Pending) }
        scheduleNext()
    }

    /** Cancel a pending or paused task (not active — use pause first). */
    fun cancel(ringtoneId: Long) {
        activeJobs.remove(ringtoneId)?.cancel()
        _state.update { s -> s.copy(tasks = s.tasks.filter { it.ringtoneId != ringtoneId }) }
        scheduleNext()
    }

    /** Retry a failed task. */
    fun retry(ringtoneId: Long) {
        updateTask(ringtoneId) { it.copy(status = DownloadStatus.Pending, errorMessage = null) }
        scheduleNext()
    }

    /** Remove all Completed and Failed tasks from the list. */
    fun clearCompleted() {
        _state.update { s ->
            s.copy(tasks = s.tasks.filter {
                it.status != DownloadStatus.Completed && it.status != DownloadStatus.Failed
            })
        }
    }

    // ── Internal ──

    private fun scheduleNext() {
        scope.launch {
            val next = _state.value.tasks.find { it.status == DownloadStatus.Pending }
                ?: return@launch
            startDownload(next)
        }
    }

    private fun startDownload(task: DownloadTask) {
        val job = scope.launch {
            semaphore.withPermit {
                updateTask(task.ringtoneId) { it.copy(status = DownloadStatus.Downloading) }

                try {
                    val localPath = withContext(Dispatchers.IO) {
                        downloadToFile(task)
                    }
                    ringtoneRepository.updateDownloadPath(task.ringtoneId, localPath)
                    updateTask(task.ringtoneId) {
                        it.copy(status = DownloadStatus.Completed, progress = 1f)
                    }
                } catch (e: Exception) {
                    // Delete partial file on failure
                    val partialFile = targetFile(task)
                    if (partialFile.exists()) partialFile.delete()

                    updateTask(task.ringtoneId) {
                        it.copy(
                            status = DownloadStatus.Failed,
                            errorMessage = e.message ?: "Unknown error",
                        )
                    }
                } finally {
                    activeJobs.remove(task.ringtoneId)
                    _state.update { s ->
                        val remainingTasks = s.tasks
                        s.copy(
                            activeCount = remainingTasks.count { it.status == DownloadStatus.Downloading },
                            pendingCount = remainingTasks.count { it.status == DownloadStatus.Pending },
                        )
                    }
                    // Try to schedule next pending task
                    scheduleNext()
                }
            }
        }
        activeJobs[task.ringtoneId] = job
    }

    /**
     * Streams the URL to a local file in the app's ringtones directory,
     * updating the download task's progress after each chunk.
     *
     * @return The absolute path of the downloaded file
     */
    private suspend fun downloadToFile(task: DownloadTask): String = withContext(Dispatchers.IO) {
        val file = targetFile(task)

        // If the file already exists, skip download
        if (file.exists()) return@withContext file.absolutePath

        val request = Request.Builder().url(task.url).build()
        val response = httpClient.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}")
            }

            val body = resp.body ?: throw IOException("Empty response body")
            val totalBytes = body.contentLength()
            val input = body.byteStream()
            val output = FileOutputStream(file)
            val buffer = ByteArray(BUFFER_SIZE)
            var downloaded = 0L
            var lastUpdateTime = 0L

            try {
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime >= 100) {
                        val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                        updateTask(task.ringtoneId) {
                            it.copy(
                                progress = progress.coerceIn(0f, 1f),
                                downloadedBytes = downloaded,
                                totalBytes = totalBytes.coerceAtLeast(0),
                            )
                        }
                        lastUpdateTime = now
                    }
                }
            } finally {
                output.close()
            }
        }

        file.absolutePath
    }

    /** Returns the target file for a download task. */
    private fun targetFile(task: DownloadTask): File {
        val fileName = task.url.split("/").lastOrNull()
            ?: "ringtone_${task.ringtoneId}.mp3"
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
            ?: context.filesDir
        return File(dir, fileName)
    }

    /**
     * Atomically updates a single task in the state list by ringtoneId.
     * If the task is not found, this is a no-op.
     */
    private fun updateTask(ringtoneId: Long, transform: (DownloadTask) -> DownloadTask) {
        _state.update { s ->
            val newTasks = s.tasks.map { task ->
                if (task.ringtoneId == ringtoneId) transform(task) else task
            }
            s.copy(
                tasks = newTasks,
                activeCount = newTasks.count { it.status == DownloadStatus.Downloading },
                pendingCount = newTasks.count { it.status == DownloadStatus.Pending },
            )
        }
    }
}
