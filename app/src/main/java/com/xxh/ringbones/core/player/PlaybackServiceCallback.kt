package com.xxh.ringbones.core.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.xxh.ringbones.core.player.model.ABLoop
import com.xxh.ringbones.core.player.model.EqPreset
import com.xxh.ringbones.core.player.visualizer.FFTProcessor
import com.xxh.ringbones.core.player.visualizer.VisualizerCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Visualizer frame rate target in milliseconds (~30fps). */
private const val VISUALIZER_INTERVAL_MS = 33L

/** How often to poll for A-B loop boundaries. */
private const val AB_LOOP_POLL_MS = 50L

/** Allowed playback speed values. */
private val ALLOWED_SPEEDS = setOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

/** Custom command: set A or B point for A-B loop. Bundle keys: "isStart" (Boolean). */
private val CMD_SET_AB_POINT = SessionCommand("SET_AB_POINT", Bundle.EMPTY)

/** Custom command: clear active A-B loop. */
private val CMD_CLEAR_AB_LOOP = SessionCommand("CLEAR_AB_LOOP", Bundle.EMPTY)

/** Custom command: set sleep timer. Bundle keys: "minutes" (Int, nullable). */
private val CMD_SET_SLEEP_TIMER = SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY)

/** Custom command: set EQ preset. Bundle keys: "preset" (String). */
private val CMD_SET_EQ_PRESET = SessionCommand("SET_EQ_PRESET", Bundle.EMPTY)

/**
 * [MediaSession.Callback] implementation that handles both standard Media3
 * playback commands and custom app-specific commands (AB loop, sleep timer, EQ).
 *
 * Owns coroutine-scoped features that are not natively modeled by MediaSession:
 * - A-B loop boundary monitor
 * - Sleep timer countdown
 * - FFT audio visualizer capture
 * - Progress polling at ~30fps
 *
 * All custom state is published to [playerStateBridge] so the ViewModel can
 * observe it via [PlayerStateBridge.customState].
 */
class PlaybackServiceCallback(
    private val service: PlaybackService,
    private val exoPlayer: Player,
    private val playerStateBridge: PlayerStateBridge,
) : MediaSession.Callback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var progressJob: Job? = null
    private var visualizerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var abLoopJob: Job? = null
    private var visualizerCapture: VisualizerCapture? = null
    private var fftProcessor: FFTProcessor? = null

    /** Previous frame magnitudes for exponential moving average smoothing. */
    private var smoothedMagnitudes: List<Float>? = null

    // ── Standard MediaSession Callbacks ──

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(CMD_SET_AB_POINT)
                    .add(CMD_CLEAR_AB_LOOP)
                    .add(CMD_SET_SLEEP_TIMER)
                    .add(CMD_SET_EQ_PRESET)
                    .build()
            )
            .setAvailablePlayerCommands(
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                    .add(Player.COMMAND_SET_MEDIA_ITEM)
                    .add(Player.COMMAND_SET_SPEED_AND_PITCH)
                    .build()
            )
            .build()
    }

    /**
     * Handles adding media items to the player queue.
     *
     * Always clears the previous queue before adding new items so that
     * navigating back and re-entering the player with a different track
     * replaces the content rather than appending to the stale queue.
     * Playback is always started for the newly added items.
     */
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> {
        // Clear any leftover queue from a previous player session so the new
        // items become the sole content rather than appending to stale data.
        if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.clearMediaItems()
        }
        exoPlayer.addMediaItems(mediaItems)
        exoPlayer.prepare()
        // playWhenReady is controlled by the ViewModel via MediaController,
        // not forced here — this lets the user start/pause playback freely.

        // Start progress polling + spectrum visualizer when media is loaded.
        startProgressPolling()
        startVisualizer()

        val future: SettableFuture<List<MediaItem>> = SettableFuture.create()
        future.set(mediaItems)
        return future
    }

    override fun onPostConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ) {
        // No-op: keep existing queue and state
    }

    // ── Custom commands ──

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            "SET_AB_POINT" -> {
                val isStart = args.getBoolean("isStart", true)
                setABPoint(isStart)
            }
            "CLEAR_AB_LOOP" -> clearABLoop()
            "SET_SLEEP_TIMER" -> {
                val minutes = if (args.containsKey("minutes")) args.getInt("minutes") else null
                setSleepTimer(if (minutes != null && minutes > 0) minutes else null)
            }
            "SET_EQ_PRESET" -> {
                val presetName = args.getString("preset") ?: return resultFuture(SessionResult.RESULT_SUCCESS)
                val preset = EqPreset.entries.find { it.name == presetName } ?: EqPreset.FLAT
                playerStateBridge.update { it.copy(eqPreset = preset) }
            }
        }
        return resultFuture(SessionResult.RESULT_SUCCESS)
    }

    private fun resultFuture(code: Int): ListenableFuture<SessionResult> {
        val future: SettableFuture<SessionResult> = SettableFuture.create()
        future.set(SessionResult(code))
        return future
    }

    // ── Progress polling ──

    private fun startProgressPolling() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                playerStateBridge.update { it.copy(progress = exoPlayer.currentPosition) }
                delay(VISUALIZER_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    // ── Visualizer ──

    /**
     * Starts the visualizer: real capture via [VisualizerCapture] if available,
     * falling back to a breathing multi-tone synthetic generator.
     */
    private fun startVisualizer() {
        fftProcessor = FFTProcessor()
        smoothedMagnitudes = null

        val sessionId = exoPlayer.audioSessionId
        if (sessionId == 0) {
            // Audio session not ready — start dummy immediately
            startDummyGenerator()
            return
        }

        val capture = VisualizerCapture(audioSessionId = sessionId)
        visualizerCapture = capture

        if (!capture.isAvailable) {
            startDummyGenerator()
            return
        }

        visualizerJob = scope.launch(Dispatchers.Default) {
            val processor = fftProcessor!!
            capture.pcmFlow.collect { samples ->
                val raw = processor.process(samples)
                smoothedMagnitudes = blendMagnitudes(smoothedMagnitudes, raw)
                playerStateBridge.update { it.copy(visualizerData = smoothedMagnitudes!!) }
            }
        }
    }

    /** Breathing multi-tone synthetic generator for when real capture is unavailable. */
    private fun startDummyGenerator() {
        visualizerJob?.cancel()
        visualizerCapture = null
        visualizerJob = scope.launch {
            val processor = fftProcessor!!
            val random = java.util.Random()
            while (isActive) {
                val t = System.nanoTime() / 1_000_000_000.0
                val breath = 0.55f + 0.45f * kotlin.math.sin(t * 0.6 * Math.PI).toFloat()
                val samples = ShortArray(256) { i ->
                    val u = i.toFloat() / 256f
                    val f1 = kotlin.math.sin(u * 2.0 * Math.PI * 8 + t * 2.5).toFloat()
                    val f2 = kotlin.math.sin(u * 2.0 * Math.PI * 22 + t * 5.0).toFloat()
                    val f3 = kotlin.math.sin(u * 2.0 * Math.PI * 48 + t * 8.0).toFloat()
                    val f4 = kotlin.math.sin(u * 2.0 * Math.PI * 85 + t * 13.0).toFloat()
                    val noise = (random.nextFloat() - 0.5f) * 0.55f
                    ((f1 * 0.25f + f2 * 0.20f + f3 * 0.15f + f4 * 0.10f + noise * 0.30f)
                        * breath * 16384f).toInt().toShort()
                }
                val raw = processor.process(samples)
                smoothedMagnitudes = blendMagnitudes(smoothedMagnitudes, raw)
                playerStateBridge.update { it.copy(visualizerData = smoothedMagnitudes!!) }
                delay(VISUALIZER_INTERVAL_MS)
            }
        }
    }

    /**
     * Blends the new frame's magnitudes with the previous frame using
     * exponential moving average for fluid, flicker-free animation.
     *
     * @param prev Previous smoothed magnitudes, or null on first frame
     * @param raw  New raw magnitudes from the current FFT frame
     * @return Blended magnitudes (always non-null if prev is non-null or raw is non-empty)
     */
    private fun blendMagnitudes(
        prev: List<Float>?,
        raw: List<Float>,
    ): List<Float> {
        if (prev == null || prev.size != raw.size) return raw
        // EMA: 70 % new + 30 % old — responsive but smooth
        val alpha = 0.7f
        val beta = 1f - alpha
        return raw.mapIndexed { i, v -> v * alpha + prev[i] * beta }
    }

    private fun stopVisualizer() {
        visualizerJob?.cancel()
        visualizerJob = null
    }

    // ── A-B Loop ──

    private fun setABPoint(isStart: Boolean) {
        val pos = exoPlayer.currentPosition
        val current = playerStateBridge.customState.value
        val loop = current.abLoop

        if (isStart) {
            val endMs = loop?.endMs ?: (exoPlayer.duration.coerceAtLeast(pos + 1))
            playerStateBridge.update { it.copy(abLoop = ABLoop(startMs = pos, endMs = endMs)) }
        } else {
            val startMs = loop?.startMs ?: 0
            if (pos <= startMs) return
            playerStateBridge.update { it.copy(abLoop = ABLoop(startMs = startMs, endMs = pos)) }
        }
        startABLoopMonitor()
    }

    private fun clearABLoop() {
        abLoopJob?.cancel()
        abLoopJob = null
        playerStateBridge.update { it.copy(abLoop = null) }
    }

    private fun startABLoopMonitor() {
        abLoopJob?.cancel()
        abLoopJob = scope.launch {
            while (isActive) {
                val loop = playerStateBridge.customState.value.abLoop
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

    // ── Sleep timer ──

    private fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()

        if (minutes == null || minutes <= 0) {
            playerStateBridge.update { it.copy(sleepTimerMinutes = null) }
            return
        }

        playerStateBridge.update { it.copy(sleepTimerMinutes = minutes) }
        sleepTimerJob = scope.launch {
            var remaining = minutes * 60
            while (isActive && remaining > 0) {
                delay(60_000L)
                remaining--
                playerStateBridge.update { it.copy(sleepTimerMinutes = (remaining / 60).coerceAtLeast(0)) }
            }
            if (remaining <= 0) {
                exoPlayer.pause()
                playerStateBridge.update { it.copy(sleepTimerMinutes = null) }
            }
        }
    }

    // ── Lifecycle ──

    private fun stopAllCoroutines() {
        stopProgressPolling()
        stopVisualizer()
        abLoopJob?.cancel()
        abLoopJob = null
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        visualizerCapture?.release()
        visualizerCapture = null
    }

    /** Release all resources. Called when Service is destroyed. */
    fun release() {
        stopAllCoroutines()
    }
}