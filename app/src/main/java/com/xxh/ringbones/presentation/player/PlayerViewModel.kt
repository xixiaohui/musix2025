package com.xxh.ringbones.presentation.player

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.xxh.ringbones.core.download.DownloadManager
import com.xxh.ringbones.core.download.DownloadStatus
import com.xxh.ringbones.core.player.PlaybackService
import com.xxh.ringbones.core.player.PlayerStateBridge
import com.xxh.ringbones.core.player.model.PlayerEffect
import com.xxh.ringbones.core.player.model.PlayerEvent
import com.xxh.ringbones.core.player.model.PlayerState
import com.xxh.ringbones.core.player.model.RepeatMode
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Duration threshold for "restart current" vs "go to previous" (3 seconds). */
private const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L

/** Allowed playback speed values. */
private val ALLOWED_SPEEDS = setOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

/**
 * ViewModel for the full-screen immersive player.
 *
 * Communicates with [PlaybackService] via [MediaController] for standard
 * playback commands, and observes [PlayerStateBridge] for custom state
 * (visualizer, AB loop, sleep timer, EQ, progress).
 *
 * Side-effects not modeled by MediaSession (download, favorite, set ringtone)
 * remain directly managed here.
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
    private val downloadManager: DownloadManager,
    private val playerStateBridge: PlayerStateBridge,
) : ViewModel() {

    /** Combined player + download state, exposed to the UI. */
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** One-shot side-effects consumed by the UI. */
    private val _effects = MutableSharedFlow<PlayerEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<PlayerEffect> = _effects.asSharedFlow()

    /** Tracks download status for the current track, updated independently. */
    private val _isDownloaded = MutableStateFlow(false)

    /** MediaController connection to PlaybackService. Null until connected. */
    private var mediaController: MediaController? = null

    /** Ringtone queue in display order for the QueueSheet UI.
     *  The controller only holds a single item to prevent auto-advancing. */
    private var ringtoneQueue: List<Ringtone> = emptyList()

    /**
     * Current index within [ringtoneQueue].
     *
     * Tracked independently from the MediaController because the controller
     * only holds a single item (to prevent auto-advance). This index drives
     * [PlayerState.currentIndex] and [PlayerState.currentRingtone] resolution.
     */
    private var currentQueueIndex: Int = 0

    /** The initial ringtone ID from navigation. */
    private val ringtoneId: Long = savedStateHandle.get<Long>("ringtoneId") ?: 0L

    /** Optional queue IDs for building the play queue. */
    private val queueIds: List<Long> =
        savedStateHandle.get<LongArray>("queueIds")?.toList() ?: emptyList()

    init {
        if (ringtoneId <= 0) {
            _effects.tryEmit(PlayerEffect.NavigateBack)
        } else {
            initialize()
        }
    }

    /**
     * Loads the initial track and queue from Room, then connects to
     * [PlaybackService] via [MediaController] and sets up state observation.
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                // Load queue from Room
                val queue = if (queueIds.isNotEmpty()) {
                    ringtoneRepository.getByIds(queueIds).first()
                } else {
                    val initial = ringtoneRepository.getById(ringtoneId).first()
                    if (initial != null) listOf(initial) else emptyList()
                }

                if (queue.isEmpty()) {
                    _effects.emit(PlayerEffect.ShowSnackbar("Track not found"))
                    _effects.emit(PlayerEffect.NavigateBack)
                    return@launch
                }

                ringtoneQueue = queue

                // Connect to PlaybackService via MediaController
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, PlaybackService::class.java),
                )
                val controllerFuture =
                    MediaController.Builder(context, sessionToken).buildAsync()

                controllerFuture.addListener(
                    {
                        try {
                            val controller = controllerFuture.get()
                            mediaController = controller
                            setupController(controller, queue)
                        } catch (e: Exception) {
                            _effects.tryEmit(
                                PlayerEffect.ShowSnackbar(
                                    "Failed to connect: ${e.message}"
                                )
                            )
                            _effects.tryEmit(PlayerEffect.NavigateBack)
                        }
                    },
                    MoreExecutors.directExecutor(),
                )
            } catch (e: Exception) {
                _effects.emit(PlayerEffect.ShowSnackbar("Failed to load: ${e.message}"))
                _effects.emit(PlayerEffect.NavigateBack)
            }
        }
    }

    /**
     * Converts a [Ringtone] domain model into a [MediaItem] for the player.
     */
    private fun ringtoneToMediaItem(ringtone: Ringtone): MediaItem {
        return MediaItem.Builder()
            .setMediaId(ringtone.id.toString())
            .setUri(ringtone.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(ringtone.title)
                    .setArtist(ringtone.author)
                    .setArtworkUri(
                        ringtone.coverImageUrl?.let { android.net.Uri.parse(it) }
                            ?: android.net.Uri.EMPTY
                    )
                    .build()
            )
            .build()
    }

    /**
     * Loads a single ringtone from [ringtoneQueue] into the MediaController
     * via [MediaController.addMediaItems], which reliably triggers the
     * [PlaybackServiceCallback.onAddMediaItems] path.
     *
     * Only one item is held by the controller at a time so that
     * [Player.REPEAT_MODE_OFF] naturally stops playback when the track ends
     * instead of auto-advancing to the next item.
     *
     * Callers are responsible for setting [Player.playWhenReady] after
     * this method returns, since [addMediaItems] is asynchronous.
     */
    private fun loadTrackIntoController(controller: MediaController, index: Int) {
        val ringtone = ringtoneQueue.getOrNull(index) ?: return
        controller.addMediaItems(listOf(ringtoneToMediaItem(ringtone)))
        currentQueueIndex = index
    }

    /**
     * Sets up the MediaController listener and loads only the selected
     * track into the PlaybackService (not the full queue) to prevent
     * automatic advancement when the current track finishes.
     */
    private fun setupController(controller: MediaController, queue: List<Ringtone>) {
        val targetIndex = queue.indexOfFirst { it.id == ringtoneId }.coerceAtLeast(0)

        // Load only the single track the user tapped on, not the full queue.
        // This ensures playback stops at the end instead of auto-advancing.
        loadTrackIntoController(controller, targetIndex)

        // Auto-start playback on initial load — the PlaybackServiceCallback
        // no longer forces playWhenReady, so we control it from here.
        controller.playWhenReady = true

        // Do not loop the current track — play once and stop.
        controller.repeatMode = Player.REPEAT_MODE_OFF

        // Start observing controller state
        controller.addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    updateStateFromController(player)
                }
            }
        )

        // Initial state snapshot
        updateStateFromController(controller)

        // Observe custom state from bridge (progress, visualizer, AB loop, sleep timer, EQ)
        viewModelScope.launch {
            playerStateBridge.customState.collect { custom ->
                _state.update { current ->
                    current.copy(
                        progress = custom.progress,
                        visualizerData = custom.visualizerData,
                        abLoop = custom.abLoop,
                        sleepTimerMinutes = custom.sleepTimerMinutes,
                        eqPreset = custom.eqPreset,
                    )
                }
            }
        }

        // Merge download status into state
        viewModelScope.launch {
            combine(
                _state,
                _isDownloaded,
            ) { _, downloaded ->
                downloaded
            }.collect { downloaded ->
                _state.update { current -> current.copy(isDownloaded = downloaded) }
            }
        }

        // Observe download completion for snackbar notifications
        observeDownloadCompletion()

        // Record play history for the initial track
        recordPlayback()
    }

    /** Reads current state from the MediaController and updates [_state].
     *
     *  Progress is read from both the controller (authoritative source, updated
     *  on every player event) and the bridge (high-frequency ~30fps polling for
     *  smooth seekbar animation during playback). Reading from the controller
     *  here ensures the seekbar reflects the correct position even when the
     *  bridge polling is stopped (e.g. after seeking while paused). */
    private fun updateStateFromController(player: Player) {
        // Use currentQueueIndex (tracked independently) instead of
        // player.currentMediaItemIndex because the controller only holds
        // a single item to prevent auto-advance.
        val currentRingtone = ringtoneQueue.getOrNull(currentQueueIndex)

        _state.update { current ->
            current.copy(
                currentRingtone = currentRingtone,
                queue = ringtoneQueue,
                currentIndex = currentQueueIndex,
                isPlaying = player.playWhenReady
                    && player.playbackState == Player.STATE_READY,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                progress = player.currentPosition.coerceAtLeast(0),
                duration = player.duration.coerceAtLeast(0),
                bufferedPercent = player.bufferedPercentage,
                repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                },
                shuffleMode = player.shuffleModeEnabled,
                playbackSpeed = player.playbackParameters.speed,
                error = if (player.playerError != null) {
                    com.xxh.ringbones.core.player.model.PlayerError(
                        message = player.playerError!!.message ?: "Playback error",
                        recoverable = true,
                    )
                } else null,
            )
        }
    }

    /**
     * Dispatch a user action to the playback engine, with additional
     * side-effects for actions that require data-layer or system integration.
     */
    fun onEvent(event: PlayerEvent) {
        val controller = mediaController ?: return

        when (event) {
            is PlayerEvent.PlayPause -> {
                if (controller.playWhenReady) controller.pause() else controller.play()
            }
            is PlayerEvent.Seek -> {
                controller.seekTo(event.positionMs)
                // Immediately update progress for instant seekbar feedback —
                // the bridge polling (if active) will keep it in sync afterwards
                _state.update { it.copy(progress = event.positionMs) }
            }
            is PlayerEvent.Next -> {
                val nextIndex = (currentQueueIndex + 1)
                    .coerceAtMost(ringtoneQueue.lastIndex)
                if (nextIndex != currentQueueIndex) {
                    val wasPlaying = controller.playWhenReady
                    loadTrackIntoController(controller, nextIndex)
                    controller.playWhenReady = wasPlaying
                }
                updateStateFromController(controller)
                recordPlayback()
            }
            is PlayerEvent.Previous -> {
                if (controller.currentPosition > PREVIOUS_RESTART_THRESHOLD_MS) {
                    controller.seekTo(0)
                } else {
                    val prevIndex = (currentQueueIndex - 1).coerceAtLeast(0)
                    if (prevIndex != currentQueueIndex) {
                        val wasPlaying = controller.playWhenReady
                        loadTrackIntoController(controller, prevIndex)
                        controller.playWhenReady = wasPlaying
                    }
                }
                updateStateFromController(controller)
                recordPlayback()
            }
            is PlayerEvent.SkipTo -> {
                if (event.index in ringtoneQueue.indices) {
                    val wasPlaying = controller.playWhenReady
                    loadTrackIntoController(controller, event.index)
                    controller.playWhenReady = wasPlaying
                }
                updateStateFromController(controller)
                recordPlayback()
                _state.value.currentRingtone?.let { checkDownloadStatus(it) }
            }
            is PlayerEvent.ToggleFavorite -> toggleFavorite()
            is PlayerEvent.Download -> downloadCurrentRingtone()
            is PlayerEvent.SetRingtone -> setCurrentAsRingtone()
            is PlayerEvent.ToggleShuffle ->
                controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            is PlayerEvent.SetRepeatMode -> {
                controller.repeatMode = when (event.mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            }
            is PlayerEvent.SetPlaybackSpeed -> {
                if (event.speed in ALLOWED_SPEEDS) {
                    controller.setPlaybackSpeed(event.speed)
                }
            }
            is PlayerEvent.SetSleepTimer -> {
                val bundle = Bundle().apply {
                    if (event.minutes != null && event.minutes > 0) {
                        putInt("minutes", event.minutes)
                    }
                }
                controller.sendCustomCommand(
                    SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY),
                    bundle,
                )
            }
            is PlayerEvent.SetABPoint -> {
                val bundle = Bundle().apply { putBoolean("isStart", event.isStart) }
                controller.sendCustomCommand(
                    SessionCommand("SET_AB_POINT", Bundle.EMPTY),
                    bundle,
                )
            }
            is PlayerEvent.ClearABLoop -> {
                controller.sendCustomCommand(
                    SessionCommand("CLEAR_AB_LOOP", Bundle.EMPTY),
                    Bundle.EMPTY,
                )
            }
            is PlayerEvent.SetEqPreset -> {
                val bundle = Bundle().apply { putString("preset", event.preset.name) }
                controller.sendCustomCommand(
                    SessionCommand("SET_EQ_PRESET", Bundle.EMPTY),
                    bundle,
                )
            }
            is PlayerEvent.DismissError -> _state.update { it.copy(error = null) }
            is PlayerEvent.RemoveFromQueue -> {
                // Adjust currentQueueIndex when removing items before/at it
                if (event.index < currentQueueIndex) {
                    currentQueueIndex--
                } else if (event.index == currentQueueIndex) {
                    // Removing the current track: play next, or stop if last
                    currentQueueIndex = currentQueueIndex.coerceAtMost(
                        ringtoneQueue.lastIndex - 1
                    )
                    if (currentQueueIndex < ringtoneQueue.size - 1) {
                        // A new track will exist at this index after removal
                        val nextRingtone = ringtoneQueue.getOrNull(
                            event.index + 1
                        ) ?: ringtoneQueue.getOrNull(0)
                        if (nextRingtone != null) {
                            val wasPlaying = controller.playWhenReady
                            controller.addMediaItems(listOf(ringtoneToMediaItem(nextRingtone)))
                            controller.playWhenReady = wasPlaying
                        }
                    }
                }
                controller.removeMediaItem(event.index)
                ringtoneQueue = ringtoneQueue.toMutableList().also {
                    if (event.index in it.indices) it.removeAt(event.index)
                }
                _state.update { it.copy(queue = ringtoneQueue) }
                updateStateFromController(controller)
            }
        }
    }

    // ── Download ──

    /**
     * Enqueues the current ringtone for download via [DownloadManager].
     * If already downloaded locally, shows a snackbar and skips the queue.
     */
    private fun downloadCurrentRingtone() {
        val ringtone = _state.value.currentRingtone ?: return
        viewModelScope.launch {
            val alreadyExists = withContext(Dispatchers.IO) {
                isFileDownloaded(ringtone)
            }
            if (alreadyExists) {
                _effects.emit(PlayerEffect.ShowSnackbar("Already downloaded"))
                return@launch
            }
            downloadManager.enqueue(ringtone)
            _effects.emit(PlayerEffect.ShowSnackbar("Added to download queue"))
        }
    }

    /**
     * Observes the [DownloadManager] state and keeps [_isDownloaded] in sync
     * with the current ringtone's download status in real-time.
     */
    private fun observeDownloadCompletion() {
        viewModelScope.launch {
            val seenCompleted = mutableSetOf<Long>()
            downloadManager.state.collect { downloadState ->
                val currentId = _state.value.currentRingtone?.id ?: return@collect
                val task = downloadState.tasks.find { it.ringtoneId == currentId }

                when {
                    task == null -> {
                        _isDownloaded.value = false
                        seenCompleted.remove(currentId)
                    }
                    task.status == DownloadStatus.Completed -> {
                        _isDownloaded.value = true
                        if (currentId !in seenCompleted) {
                            seenCompleted.add(currentId)
                            _effects.emit(PlayerEffect.ShowSnackbar("Download complete"))
                        }
                    }
                    else -> {
                        _isDownloaded.value = false
                        seenCompleted.remove(currentId)
                    }
                }
            }
        }
    }

    /** Checks whether the given ringtone is already downloaded to local storage. */
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
        val storedPath = ringtone.downloadPath
        if (!storedPath.isNullOrEmpty() && File(storedPath).exists()) return true

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
     * Otherwise requires the file to be downloaded first.
     */
    private fun setCurrentAsRingtone() {
        val ringtone = _state.value.currentRingtone ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.System.canWrite(context)
        ) {
            viewModelScope.launch {
                _effects.emit(
                    PlayerEffect.ShowSnackbar(
                        "Permission needed: enable \"Modify system settings\""
                    )
                )
                _effects.emit(PlayerEffect.OpenWriteSettings)
            }
            return
        }

        viewModelScope.launch {
            _effects.emit(PlayerEffect.ShowSnackbar("Setting ringtone..."))
            try {
                val localPath = withContext(Dispatchers.IO) {
                    val existingPath = ringtone.downloadPath
                    if (existingPath != null && File(existingPath).exists()) {
                        existingPath
                    } else {
                        val fileName = ringtone.url.split("/").lastOrNull()
                            ?: "ringtone_${ringtone.id}.mp3"
                        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
                            ?: context.filesDir
                        val file = File(dir, fileName)
                        if (file.exists()) {
                            file.absolutePath
                        } else {
                            throw IllegalStateException("File not downloaded")
                        }
                    }
                }
                withContext(Dispatchers.IO) {
                    RingtoneHelper.setRingtone(context, localPath)
                }
                _effects.emit(PlayerEffect.ShowSnackbar("Ringtone set successfully"))
            } catch (e: SecurityException) {
                _effects.emit(
                    PlayerEffect.ShowSnackbar(
                        "Permission needed: enable \"Modify system settings\""
                    )
                )
                _effects.emit(PlayerEffect.OpenWriteSettings)
            } catch (e: Exception) {
                _effects.emit(PlayerEffect.ShowSnackbar("Failed to set ringtone: ${e.message}"))
            }
        }
    }

    // ── Favorite ──

    /** Toggles the favorite status for the current ringtone. */
    private fun toggleFavorite() {
        viewModelScope.launch {
            _state.value.currentRingtone?.let { ringtone ->
                toggleFavoriteUseCase(ringtone.id)
                val newFav = !ringtone.isFavorite
                _state.update { it.copy(isFavorite = newFav) }
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
        mediaController?.release()
        mediaController = null
    }
}