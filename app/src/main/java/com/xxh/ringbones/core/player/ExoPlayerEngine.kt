package com.xxh.ringbones.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.xxh.ringbones.core.player.model.ABLoop
import com.xxh.ringbones.core.player.model.EqPreset
import com.xxh.ringbones.core.player.model.PlayerEffect
import com.xxh.ringbones.core.player.model.PlayerError
import com.xxh.ringbones.core.player.model.PlayerEvent
import com.xxh.ringbones.core.player.model.PlayerState
import com.xxh.ringbones.core.player.model.RepeatMode
import com.xxh.ringbones.core.player.visualizer.FFTProcessor
import com.xxh.ringbones.core.player.visualizer.VisualizerCapture
import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** Duration threshold for "restart current" vs "go to previous" (3 seconds). */
private const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L

/** Default playback speed. */
private const val DEFAULT_SPEED = 1.0f

/** Visualizer frame rate target in milliseconds (~30fps). */
private const val VISUALIZER_INTERVAL_MS = 33L

/** How often to poll for A–B loop boundaries (fast enough for responsive seeking). */
private const val AB_LOOP_POLL_MS = 50L

/** Allowed playback speed values. */
private val ALLOWED_SPEEDS = setOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

/**
 * ExoPlayer-backed implementation of [PlayerEngine].
 *
 * Manages the full playback lifecycle including queue navigation, repeat/shuffle
 * modes, playback speed, sleep timer, A–B loop, and FFT-based audio visualization.
 *
 * The engine is designed to be instantiated once per player session and released
 * via [release] when the player screen is destroyed.
 *
 * @param context Application context (safe to hold)
 * @param initialRingtone The ringtone to play first
 * @param initialQueue The full queue (must contain [initialRingtone])
 * @param scope Coroutine scope for engine-internal jobs (visualizer, timer, AB-loop)
 */
class ExoPlayerEngine(
    context: Context,
    initialRingtone: Ringtone,
    initialQueue: List<Ringtone>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : PlayerEngine {

    // ── Internal state ──

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PlayerEffect>(extraBufferCapacity = 16)
    /** One-shot side-effects that the ViewModel collects. */
    val effects: SharedFlow<PlayerEffect> = _effects.asSharedFlow()

    /** ExoPlayer instance — created in init, released in [release]. */
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    /** FFT processor for audio visualization. Created lazily on first use. */
    private var fftProcessor: FFTProcessor? = null

    /** Original queue order (before shuffle). Used to unshuffle. */
    private val originalQueue: List<Ringtone> = initialQueue.toList()

    /** Shuffled queue indices, or null if not in shuffle mode. */
    private var shuffledIndices: IntArray? = null

    /** Active visualizer polling job. */
    private var visualizerJob: Job? = null

    /** Active progress polling job (~30fps on main thread). */
    private var progressJob: Job? = null

    /** Active sleep timer countdown job. */
    private var sleepTimerJob: Job? = null

    /** Active A–B loop boundary-monitoring job. */
    private var abLoopJob: Job? = null

    /** VisualizerCapture instance — created lazily on first use. */
    private var visualizerCapture: VisualizerCapture? = null

    init {
        require(initialQueue.isNotEmpty()) { "Queue must not be empty" }
        val startIndex = initialQueue.indexOf(initialRingtone).coerceAtLeast(0)

        _state.update { current ->
            current.copy(
                currentRingtone = initialRingtone,
                queue = initialQueue,
                currentIndex = startIndex,
                isFavorite = initialRingtone.isFavorite,
            )
        }

        attachPlayerListener()
        loadTrack(initialRingtone)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // ── Public API ──

    override fun handleEvent(event: PlayerEvent) {
        val current = _state.value
        when (event) {
            PlayerEvent.PlayPause -> togglePlayPause()
            is PlayerEvent.Seek -> seekTo(event.positionMs)
            PlayerEvent.Next -> skipToNext()
            PlayerEvent.Previous -> handlePrevious()
            is PlayerEvent.SkipTo -> skipToIndex(event.index)
            PlayerEvent.ToggleFavorite -> toggleFavorite(current)
            PlayerEvent.Download -> { /* handled by PlayerViewModel */ }
            PlayerEvent.SetRingtone -> { /* handled by PlayerViewModel */ }
            PlayerEvent.ToggleShuffle -> toggleShuffle(current)
            is PlayerEvent.SetRepeatMode -> setRepeatMode(event.mode)
            is PlayerEvent.SetPlaybackSpeed -> setSpeed(event.speed)
            is PlayerEvent.SetSleepTimer -> setSleepTimer(event.minutes)
            is PlayerEvent.SetABPoint -> setABPoint(event.isStart, current)
            PlayerEvent.ClearABLoop -> clearABLoop()
            is PlayerEvent.RemoveFromQueue -> handleRemoveFromQueue(event.index)
            is PlayerEvent.SetEqPreset -> setEqPreset(event.preset)
            PlayerEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    override fun release() {
        progressJob?.cancel()
        visualizerJob?.cancel()
        sleepTimerJob?.cancel()
        abLoopJob?.cancel()
        visualizerCapture?.release()
        exoPlayer.release()
    }

    // ── Playback control ──

    private fun togglePlayPause() {
        val isPlaying = exoPlayer.isPlaying
        if (isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
        _state.update { it.copy(isPlaying = !isPlaying) }
    }

    private fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0, exoPlayer.duration.coerceAtLeast(0))
        exoPlayer.seekTo(clamped)
    }

    private fun handlePrevious() {
        if (exoPlayer.currentPosition > PREVIOUS_RESTART_THRESHOLD_MS) {
            exoPlayer.seekTo(0)
        } else {
            skipToPrevious()
        }
    }

    private fun skipToNext() {
        val current = _state.value
        val nextIndex = getNextIndex(current)
        if (nextIndex == current.currentIndex && current.repeatMode != RepeatMode.ALL) {
            // End of queue with no repeat — stop
            exoPlayer.pause()
            exoPlayer.seekTo(0)
            return
        }
        navigateToIndex(nextIndex)
    }

    private fun skipToPrevious() {
        val current = _state.value
        val prevIndex = getPreviousIndex(current)
        navigateToIndex(prevIndex)
    }

    private fun skipToIndex(index: Int) {
        val current = _state.value
        if (index in current.queue.indices) {
            navigateToIndex(index)
        }
    }

    /**
     * Removes a track from the queue at [index]. Adjusts [currentIndex] to
     * stay on the same logical track when possible, or stop playback and
     * emit [PlayerEffect.NavigateBack] if the last track was removed.
     *
     * Edge cases:
     * - Removing current track when others exist: keep currentIndex, next item slides in
     * - Removing a track before current: decrement currentIndex
     * - Removing the only remaining track: stop playback, emit NavigateBack
     */
    private fun handleRemoveFromQueue(index: Int) {
        val current = _state.value
        val queue = current.queue.toMutableList()
        if (index !in queue.indices) return

        queue.removeAt(index)

        when {
            // Last track removed — stop and navigate back
            queue.isEmpty() -> {
                clearABLoop()
                exoPlayer.stop()
                _state.update {
                    it.copy(
                        queue = emptyList(),
                        currentIndex = 0,
                        currentRingtone = null,
                        isPlaying = false,
                    )
                }
                emitEffect(PlayerEffect.NavigateBack)
            }

            // Removed current track — next track (at same index) becomes current
            index == current.currentIndex -> {
                _state.update { it.copy(queue = queue) }
                val newRingtone = queue.getOrNull(index)
                if (newRingtone != null) {
                    clearABLoop()
                    _state.update {
                        it.copy(currentRingtone = newRingtone, isFavorite = newRingtone.isFavorite)
                    }
                    loadTrack(newRingtone)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            }

            // Removed track before current — decrement index
            index < current.currentIndex -> {
                _state.update {
                    it.copy(queue = queue, currentIndex = current.currentIndex - 1)
                }
            }

            // Removed track after current — index unchanged
            else -> {
                _state.update { it.copy(queue = queue) }
            }
        }
    }

    // ── Queue navigation ──

    private fun getNextIndex(state: PlayerState): Int {
        val queue = state.queue
        if (queue.isEmpty()) return 0
        if (state.shuffleMode && shuffledIndices != null) {
            val shuffled = shuffledIndices!!
            val posInShuffled = shuffled.indexOf(state.currentIndex)
            val nextShufflePos = posInShuffled + 1
            return if (nextShufflePos < shuffled.size) shuffled[nextShufflePos]
            else if (state.repeatMode == RepeatMode.ALL) shuffled[0]
            else state.currentIndex // End of shuffled queue
        }
        return when (state.repeatMode) {
            RepeatMode.ONE -> state.currentIndex
            RepeatMode.ALL -> (state.currentIndex + 1) % queue.size
            RepeatMode.OFF -> {
                val next = state.currentIndex + 1
                if (next < queue.size) next else state.currentIndex
            }
        }
    }

    private fun getPreviousIndex(state: PlayerState): Int {
        val queue = state.queue
        if (queue.isEmpty()) return 0
        if (state.shuffleMode && shuffledIndices != null) {
            val shuffled = shuffledIndices!!
            val posInShuffled = shuffled.indexOf(state.currentIndex)
            val prevShufflePos = posInShuffled - 1
            return if (prevShufflePos >= 0) shuffled[prevShufflePos]
            else state.currentIndex
        }
        return when {
            state.currentIndex > 0 -> state.currentIndex - 1
            state.repeatMode == RepeatMode.ALL -> queue.size - 1
            else -> 0
        }
    }

    private fun navigateToIndex(index: Int) {
        val current = _state.value
        val ringtone = current.queue.getOrNull(index) ?: return

        // Stop current A–B loop
        clearABLoop()

        _state.update { it.copy(currentIndex = index, currentRingtone = ringtone, isFavorite = ringtone.isFavorite) }
        loadTrack(ringtone)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun loadTrack(ringtone: Ringtone) {
        val uri = resolveUri(ringtone)
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
    }

    // ── Shuffle ──

    private fun toggleShuffle(current: PlayerState) {
        val newShuffle = !current.shuffleMode
        if (newShuffle) {
            // Fisher-Yates shuffle of indices
            val indices = (0 until current.queue.size).toMutableList()
            for (i in indices.size - 1 downTo 1) {
                val j = (0..i).random()
                val tmp = indices[i]
                indices[i] = indices[j]
                indices[j] = tmp
            }
            // Ensure current track is first in shuffled order
            indices.remove(current.currentIndex)
            indices.add(0, current.currentIndex)
            shuffledIndices = indices.toIntArray()
        } else {
            shuffledIndices = null
        }
        _state.update { it.copy(shuffleMode = newShuffle) }
    }

    // ── Repeat ──

    private fun setRepeatMode(mode: RepeatMode) {
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _state.update { it.copy(repeatMode = mode) }
    }

    // ── Playback speed ──

    private fun setSpeed(speed: Float) {
        if (speed !in ALLOWED_SPEEDS) return
        exoPlayer.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    // ── Sleep timer ──

    private fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()

        if (minutes == null || minutes <= 0) {
            _state.update { it.copy(sleepTimerMinutes = null) }
            return
        }

        _state.update { it.copy(sleepTimerMinutes = minutes) }
        sleepTimerJob = scope.launch {
            var remaining = minutes * 60 // convert to seconds
            while (isActive && remaining > 0) {
                delay(60_000L) // tick every minute
                remaining--
                _state.update { it.copy(sleepTimerMinutes = (remaining / 60).coerceAtLeast(0)) }
            }
            if (remaining <= 0) {
                exoPlayer.pause()
                _state.update { it.copy(isPlaying = false, sleepTimerMinutes = null) }
            }
        }
    }

    // ── A–B Loop ──

    private fun setABPoint(isStart: Boolean, current: PlayerState) {
        val pos = exoPlayer.currentPosition
        val loop = current.abLoop
        if (isStart) {
            val endMs = loop?.endMs ?: (exoPlayer.duration.coerceAtLeast(pos + 1))
            val newLoop = ABLoop(startMs = pos, endMs = endMs)
            _state.update { it.copy(abLoop = newLoop) }
        } else {
            val startMs = loop?.startMs ?: 0
            if (pos <= startMs) return // end must be after start
            val newLoop = ABLoop(startMs = startMs, endMs = pos)
            _state.update { it.copy(abLoop = newLoop) }
        }
        startABLoopMonitor()
    }

    private fun clearABLoop() {
        abLoopJob?.cancel()
        abLoopJob = null
        _state.update { it.copy(abLoop = null) }
    }

    private fun startABLoopMonitor() {
        abLoopJob?.cancel()
        abLoopJob = scope.launch {
            while (isActive) {
                val loop = _state.value.abLoop
                if (loop != null) {
                    val pos = exoPlayer.currentPosition
                    if (pos >= loop.endMs) {
                        exoPlayer.seekTo(loop.startMs)
                    }
                }
                delay(AB_LOOP_POLL_MS)
            }
        }
    }

    // ── EQ ──

    private fun setEqPreset(preset: EqPreset) {
        // EQ is applied as a metadata hint; actual audio processing would go through
        // Media3 AudioProcessor chain. For now, store the preset in state.
        _state.update { it.copy(eqPreset = preset) }
    }

    // ── Favorite ──

    private fun toggleFavorite(current: PlayerState) {
        val newFav = !current.isFavorite
        _state.update { it.copy(isFavorite = newFav) }
        // Emit effect for ViewModel to persist via repository
        emitEffect(PlayerEffect.ShowSnackbar(
            if (newFav) "Added to favorites" else "Removed from favorites"
        ))
    }

    // ── Progress polling ──

    /**
     * Polls [exoPlayer.currentPosition] and [exoPlayer.bufferedPercentage] at ~30fps
     * on the main thread, updating [PlayerState.progress] and [PlayerState.bufferedPercent].
     */
    private fun startProgressPolling() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                _state.update { it.copy(progress = exoPlayer.currentPosition) }
                delay(VISUALIZER_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    // ── Visualizer ──

    private fun startVisualizer() {
        if (visualizerJob?.isActive == true) return

        val capture = visualizerCapture ?: run {
            val sessionId = exoPlayer.audioSessionId
            if (sessionId == 0) return
            VisualizerCapture(audioSessionId = sessionId).also { visualizerCapture = it }
        }

        if (!capture.isAvailable) {
            startDummyVisualizer()
            return
        }

        fftProcessor = FFTProcessor()
        visualizerJob = scope.launch(Dispatchers.Default) {
            val processor = fftProcessor!!
            capture.pcmFlow.collect { samples ->
                val magnitudes = processor.process(samples)
                _state.update { it.copy(visualizerData = magnitudes) }
            }
        }
    }

    private fun stopVisualizer() {
        visualizerJob?.cancel()
        visualizerJob = null
    }

    /** Fallback visualizer when VisualizerCapture is not available. */
    private fun startDummyVisualizer() {
        fftProcessor = FFTProcessor()
        visualizerJob = scope.launch(Dispatchers.Default) {
            val processor = fftProcessor!!
            while (isActive) {
                val dummySamples = ShortArray(256) { i ->
                    val t = i.toFloat() / 256f
                    val envelope = if (_state.value.isPlaying) 0.5f + 0.5f * kotlin.math.sin(
                        (System.nanoTime() / 1_000_000_000.0 * 2.0 * Math.PI).toFloat()
                    ).toFloat() else 0.1f
                    ((kotlin.math.sin(t * 2 * Math.PI * 8).toFloat() * envelope * 32767)).toInt().toShort()
                }
                val magnitudes = processor.process(dummySamples)
                _state.update { it.copy(visualizerData = magnitudes) }
                delay(VISUALIZER_INTERVAL_MS)
            }
        }
    }

    // ── ExoPlayer listener ──

    private fun attachPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressPolling()
                    startVisualizer()
                } else {
                    stopProgressPolling()
                    stopVisualizer()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isLoading = playbackState == Player.STATE_BUFFERING
                val isReady = playbackState == Player.STATE_READY
                _state.update {
                    it.copy(
                        isLoading = isLoading,
                        duration = exoPlayer.duration.coerceAtLeast(0),
                    )
                }
                if (isReady) {
                    _state.update { current ->
                        current.copy(
                            progress = exoPlayer.currentPosition,
                            bufferedPercent = exoPlayer.bufferedPercentage,
                            duration = exoPlayer.duration.coerceAtLeast(0),
                            isLoading = false,
                        )
                    }
                    startProgressPolling()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                _state.update { it.copy(progress = exoPlayer.currentPosition) }
                // Auto-advance on completion (reason == DISCONTINUITY_REASON_AUTO_TRANSITION)
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    val current = _state.value
                    if (current.repeatMode == RepeatMode.ONE) return // ExoPlayer handles repeat-one
                    val nextIdx = getNextIndex(current)
                    if (nextIdx != current.currentIndex) {
                        navigateToIndex(nextIdx)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        error = PlayerError(
                            message = error.message ?: "Playback error",
                            recoverable = true,
                        ),
                        isLoading = false,
                        isPlaying = false,
                    )
                }
            }
        })
    }

    // ── Helpers ──

    private fun resolveUri(ringtone: Ringtone): String {
        return ringtone.downloadPath?.let { path ->
            val file = File(path)
            if (file.exists()) Uri.fromFile(file).toString()
            else ringtone.url
        } ?: ringtone.url
    }

    private fun emitEffect(effect: PlayerEffect) {
        scope.launch { _effects.emit(effect) }
    }
}