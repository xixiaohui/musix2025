package com.xxh.ringbones.presentation.player

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.core.network.HttpClient
import com.xxh.ringbones.core.player.ExoPlayerEngine
import com.xxh.ringbones.core.player.PlayerEngine
import com.xxh.ringbones.core.player.model.PlayerEffect
import com.xxh.ringbones.core.player.model.PlayerEvent
import com.xxh.ringbones.core.player.model.PlayerState
import com.xxh.ringbones.core.util.RingtoneHelper
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import com.xxh.ringbones.domain.usecase.RecordPlayHistoryUseCase
import com.xxh.ringbones.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * ViewModel for the full-screen immersive player.
 *
 * Creates and owns a [PlayerEngine] that manages ExoPlayer lifecycle,
 * queue navigation, and all playback state. Routes [PlayerEvent] actions
 * from the UI to the engine, and exposes [state] + [effects] for the
 * Composable screen to observe.
 *
 * Also handles side-effects that require data-layer access:
 * - Downloading ringtones to local storage
 * - Setting ringtones as device ringtone via [RingtoneHelper]
 * - Toggling favorites via [ToggleFavoriteUseCase]
 *
 * Download state ([isDownloaded]) is tracked as a separate flow and
 * merged into the engine's [PlayerState] so the UI always reflects
 * whether the current track exists locally.
 *
 * Navigation args (from [SavedStateHandle]):
 * - `ringtoneId: Long` — the initial track to play
 * - `queueIds: LongArray?` — optional list of IDs forming the play queue
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
    private val recordPlayHistoryUseCase: RecordPlayHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {

    /** Combined player + download state, exposed to the UI. */
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** One-shot side-effects consumed by the UI. */
    private val _effects = MutableSharedFlow<PlayerEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<PlayerEffect> = _effects.asSharedFlow()

    /** Tracks download status for the current track, updated independently. */
    private val _isDownloaded = MutableStateFlow(false)

    /** Playback engine — initialized once data is loaded. Null until ready. */
    private var engine: PlayerEngine? = null

    /** The initial ringtone ID from navigation. */
    private val ringtoneId: Long = savedStateHandle.get<Long>("ringtoneId") ?: 0L

    /** Optional queue IDs for building the play queue. */
    private val queueIds: List<Long> = savedStateHandle.get<ArrayList<Long>>("queueIds") ?: emptyList()

    init {
        if (ringtoneId <= 0) {
            _effects.tryEmit(PlayerEffect.NavigateBack)
        } else {
            initializePlayer()
        }
    }

    /**
     * Loads the initial track and queue from the repository, then creates
     * the [ExoPlayerEngine] and connects its state/effects streams to the
     * ViewModel's public outputs. Also merges the download-status flow
     * into the emitted state.
     */
    private fun initializePlayer() {
        viewModelScope.launch {
            try {
                val initialRingtone = ringtoneRepository.getById(ringtoneId).first()
                    ?: run {
                        _effects.emit(PlayerEffect.ShowSnackbar("Track not found"))
                        _effects.emit(PlayerEffect.NavigateBack)
                        return@launch
                    }

                // Build queue: if queueIds provided, load those; otherwise single-track queue
                val queue = if (queueIds.isNotEmpty()) {
                    ringtoneRepository.getByIds(queueIds).first()
                } else {
                    listOf(initialRingtone)
                }

                val engineInstance = ExoPlayerEngine(
                    context = context,
                    initialRingtone = initialRingtone,
                    initialQueue = queue,
                    scope = viewModelScope,
                )

                // Forward engine state merged with download status
                viewModelScope.launch {
                    combine(
                        engineInstance.state,
                        _isDownloaded,
                    ) { engineState, downloaded ->
                        engineState.copy(isDownloaded = downloaded)
                    }.collect { mergedState ->
                        _state.value = mergedState
                    }
                }

                // Forward engine effects to ViewModel effects
                viewModelScope.launch {
                    engineInstance.effects.collect { effect ->
                        _effects.emit(effect)
                    }
                }

                engine = engineInstance

                // Check initial download status
                checkDownloadStatus(initialRingtone)
            } catch (e: Exception) {
                _effects.emit(PlayerEffect.ShowSnackbar("Failed to load player: ${e.message}"))
                _effects.emit(PlayerEffect.NavigateBack)
            }
        }
    }

    /**
     * Dispatch a user action to the playback engine, with additional
     * side-effects for actions that require data-layer or system integration.
     */
    fun onEvent(event: PlayerEvent) {
        engine?.handleEvent(event)

        when (event) {
            is PlayerEvent.ToggleFavorite -> toggleFavorite()
            is PlayerEvent.Next, is PlayerEvent.Previous -> recordPlayback()
            is PlayerEvent.SkipTo -> {
                recordPlayback()
                // Refresh download status for the new track
                _state.value.currentRingtone?.let { checkDownloadStatus(it) }
            }
            is PlayerEvent.Download -> downloadCurrentRingtone()
            is PlayerEvent.SetRingtone -> setCurrentAsRingtone()
            else -> { /* no additional side-effect */ }
        }
    }

    // ── Download ──

    /**
     * Downloads the current ringtone to local storage on [Dispatchers.IO],
     * then updates the database with the local file path and refreshes
     * the download-state UI.
     */
    private fun downloadCurrentRingtone() {
        val ringtone = _state.value.currentRingtone ?: return

        // Skip if already downloaded
        if (isFileDownloaded(ringtone)) {
            viewModelScope.launch {
                _effects.emit(PlayerEffect.ShowSnackbar("Already downloaded"))
            }
            return
        }

        viewModelScope.launch {
            _effects.emit(PlayerEffect.ShowSnackbar("Downloading..."))
            try {
                val localPath = withContext(Dispatchers.IO) {
                    downloadToFile(ringtone)
                }
                ringtoneRepository.updateDownloadPath(ringtone.id, localPath)
                _isDownloaded.value = true
                _effects.emit(PlayerEffect.ShowSnackbar("Download complete"))
            } catch (e: Exception) {
                _effects.emit(PlayerEffect.ShowSnackbar("Download failed: ${e.message}"))
            }
        }
    }

    /**
     * Downloads the ringtone URL to the app's ringtones directory using
     * the shared [HttpClient] instance.
     *
     * If a file with the same name already exists, returns its path immediately.
     *
     * @return The absolute path of the downloaded file.
     */
    private suspend fun downloadToFile(ringtone: Ringtone): String {
        val fileName = ringtone.url.split("/").lastOrNull() ?: "ringtone_${ringtone.id}.mp3"
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
            ?: context.filesDir
        val file = File(dir, fileName)

        // Skip if already downloaded
        if (file.exists()) return file.absolutePath

        val request = Request.Builder().url(ringtone.url).build()
        val response = HttpClient.instance.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException("HTTP ${response.code}")
        }

        response.body?.byteStream()?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        } ?: throw RuntimeException("Empty response body")

        return file.absolutePath
    }

    /**
     * Checks whether the given ringtone is already downloaded to local
     * storage and updates [_isDownloaded] accordingly.
     */
    private fun checkDownloadStatus(ringtone: Ringtone) {
        viewModelScope.launch {
            val downloaded = withContext(Dispatchers.IO) {
                isFileDownloaded(ringtone)
            }
            _isDownloaded.value = downloaded
        }
    }

    /**
     * Returns true if the ringtone's file exists locally, either via
     * the stored [Ringtone.downloadPath] or by deriving the expected
     * filename from the URL.
     */
    private fun isFileDownloaded(ringtone: Ringtone): Boolean {
        // Check stored path first
        val storedPath = ringtone.downloadPath
        if (storedPath != null && File(storedPath).exists()) return true

        // Fallback: check the default download location
        val fileName = ringtone.url.split("/").lastOrNull() ?: return false
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
            ?: context.filesDir
        return File(dir, fileName).exists()
    }

    // ── Set Ringtone ──

    /**
     * Sets the current ringtone as the device ringtone.
     *
     * If the ringtone is already downloaded locally, uses the local file.
     * Otherwise downloads it first, then applies the ringtone.
     */
    private fun setCurrentAsRingtone() {
        val ringtone = _state.value.currentRingtone ?: return

        // Check WRITE_SETTINGS permission on Android 6+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            viewModelScope.launch {
                _effects.emit(PlayerEffect.ShowSnackbar("Permission needed: enable \"Modify system settings\""))
                _effects.emit(PlayerEffect.OpenWriteSettings)
            }
            return
        }

        viewModelScope.launch {
            _effects.emit(PlayerEffect.ShowSnackbar("Setting ringtone..."))
            try {
                val localPath = withContext(Dispatchers.IO) {
                    // Use existing download path if available and file exists
                    val existingPath = ringtone.downloadPath
                    if (existingPath != null && File(existingPath).exists()) {
                        existingPath
                    } else {
                        downloadToFile(ringtone).also { path ->
                            ringtoneRepository.updateDownloadPath(ringtone.id, path)
                        }
                    }
                }
                withContext(Dispatchers.IO) {
                    RingtoneHelper.setRingtone(context, localPath)
                }
                _effects.emit(PlayerEffect.ShowSnackbar("Ringtone set successfully"))
            } catch (e: SecurityException) {
                _effects.emit(PlayerEffect.ShowSnackbar("Permission needed: enable \"Modify system settings\""))
                _effects.emit(PlayerEffect.OpenWriteSettings)
            } catch (e: Exception) {
                _effects.emit(PlayerEffect.ShowSnackbar("Failed to set ringtone: ${e.message}"))
            }
        }
    }

    // ── Favorite ──

    /**
     * Toggles the favorite status for the current ringtone via [ToggleFavoriteUseCase],
     * which writes to both the [favorites] join table and [ringtones.isFavorite] column.
     */
    private fun toggleFavorite() {
        viewModelScope.launch {
            _state.value.currentRingtone?.let { ringtone ->
                toggleFavoriteUseCase(ringtone.id)
            }
        }
    }

    // ── Play History ──

    /** Records a play event for the current track via the use case. */
    private fun recordPlayback() {
        viewModelScope.launch {
            _state.value.currentRingtone?.let { ringtone ->
                recordPlayHistoryUseCase(ringtone.id)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine?.release()
        engine = null
    }
}
