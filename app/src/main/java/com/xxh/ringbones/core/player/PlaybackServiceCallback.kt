package com.xxh.ringbones.core.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.xxh.ringbones.core.player.model.ABLoop
import com.xxh.ringbones.core.player.model.EqPreset
import com.xxh.ringbones.core.player.visualizer.FFTProcessor
import com.xxh.ringbones.core.player.visualizer.VisualizerCapture
import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Duration threshold for "restart current" vs "go to previous" (3 seconds). */
private const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L

/** Visualizer frame rate target in milliseconds (~30fps). */
private const val VISUALIZER_INTERVAL_MS = 33L

/** Retry delay when audio session ID is not yet available (100ms). */
private const val RETRY_SESSION_ID_DELAY_MS = 100L

/** How often to poll for A–B loop boundaries. */
private const val AB_LOOP_POLL_MS = 50L

/** Allowed playback speed values. */
private val ALLOWED_SPEEDS = setOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

/** Custom command: set A or B point for A–B loop. Bundle keys: "isStart" (Boolean). */
private val CMD_SET_AB_POINT = SessionCommand("SET_AB_POINT", Bundle::class.java)

/** Custom command: clear active A–B loop. */
private val CMD_CLEAR_AB_LOOP = SessionCommand("CLEAR_AB_LOOP", Bundle::class.java)

/** Custom command: set sleep timer. Bundle keys: "minutes" (Int, nullable). */
private val CMD_SET_SLEEP_TIMER = SessionCommand("SET_SLEEP_TIMER", Bundle::class.java)

/** Custom command: set EQ preset. Bundle keys: "preset" (String). */
private val CMD_SET_EQ_PRESET = SessionCommand("SET_EQ_PRESET", Bundle::class.java)

/**
 * [MediaSession.Callback] implementation that handles both standard Media3
 * playback commands and custom app-specific commands (AB loop, sleep timer, EQ).
 *
 * Owns coroutine-scoped features that are not natively modeled by MediaSession:
 * - A–B loop boundary monitor
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

    // ── Standard MediaSession Callbacks ──

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val customCommands = MediaSession.CustomCommand.Builder()
            .addCustomCommand(CMD_SET_AB_POINT)
            .addCustomCommand(CMD_CLEAR_AB_LOOP)
            .addCustomCommand(CMD_SET_SLEEP_TIMER)
            .addCustomCommand(CMD_SET_EQ_PRESET)
            .build()

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .addCustomCommand(CMD_SET_AB_POINT)
                    .addCustomCommand(CMD_CLEAR_AB_LOOP)
                    .addCustomCommand(CMD_SET_SLEEP_TIMER)
                    .addCustomCommand(CMD_SET_EQ_PRESET)
                    .build()
            )
            .setAvailablePlayerCommands(
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                    .add(Player.COMMAND_SET_SPEED_AND_PITCH)
                    .build()
            )
            .setCustomLayout(emptyList())
            .build()
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> {
        val startIndex = exoPlayer.mediaItemCount
        exoPlayer.addMediaItems(mediaItems)
        exoPlayer.prepare()
        if (startIndex == 0) {
            exoPlayer.playWhenReady = true
        }
        return androidx.media3.common.util.Util.getFuture(mediaItems)
    }

    override fun onPostConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ) {
        // No-op: keep existing queue and state
    }

    // ── Player events ──

    override fun onPlayerAvailableCommandsChanged(
        session: MediaSession,
        availableCommands: Player.Commands,
    ) {
        // No-op
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
                val presetName = args.getString("preset") ?: return@onCustomCommand
                val preset = EqPreset.entries.find { it.name == presetName } ?: EqPreset.FLAT
                playerStateBridge.update { it.copy(eqPreset = preset) }
            }
        }
        return androidx.media3.common.util.Util.getFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    // ── MediaSession lifecycle ──

    override fun onPlayerRemoved(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ) {
        // Clean up if all controllers have disconnected
        if (session.connectedControllers.isEmpty()) {
            stopAllCoroutines()
        }
    }

    override fun onPostDisconnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ) {
        // If no connected controllers remain, stop coroutines but keep playback
        if (session.connectedControllers.isEmpty()) {
            stopAllCoroutines()
        }
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

    private fun startVisualizer() {
        if (visualizerJob?.isActive == true) return

        val sessionId = exoPlayer.audioSessionId
        if (sessionId == 0) {
            visualizerJob = scope.launch {
                delay(RETRY_SESSION_ID_DELAY_MS)
                startVisualizer()
            }
            return
        }

        val capture = visualizerCapture ?: run {
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
                playerStateBridge.update { it.copy(visualizerData = magnitudes) }
            }
        }
    }

    private fun stopVisualizer() {
        visualizerJob?.cancel()
        visualizerJob = null
    }

    private fun startDummyVisualizer() {
        fftProcessor = FFTProcessor()
        visualizerJob = scope.launch(Dispatchers.Default) {
            val processor = fftProcessor!!
            while (isActive) {
                val isPlaying = exoPlayer.playWhenReady
                val dummySamples = ShortArray(256) { i ->
                    val t = i.toFloat() / 256f
                    val envelope = if (isPlaying) {
                        0.5f + 0.5f * kotlin.math.sin(
                            (System.nanoTime() / 1_000_000_000.0 * 2.0 * Math.PI).toFloat()
                        ).toFloat()
                    } else {
                        0.1f
                    }
                    ((kotlin.math.sin(t * 2 * Math.PI * 8).toFloat() * envelope * 32767)).toInt().toShort()
                }
                val magnitudes = processor.process(dummySamples)
                playerStateBridge.update { it.copy(visualizerData = magnitudes) }
                delay(VISUALIZER_INTERVAL_MS)
            }
        }
    }

    // ── A–B Loop ──

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

    // ── Lifecycle helpers ──

    /** Start progress + visualizer when playback begins. */
    fun onPlaybackStarted() {
        startProgressPolling()
        startVisualizer()
    }

    /** Stop progress + visualizer when playback pauses. */
    fun onPlaybackPaused() {
        stopProgressPolling()
        stopVisualizer()
    }

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