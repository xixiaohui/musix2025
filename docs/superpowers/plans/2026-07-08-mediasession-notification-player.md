# MediaSession + 通知栏播放器 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将播放器架构从 ViewModel 本地持有 ExoPlayer 迁移到 MediaSessionService，实现后台播放 + 通知栏控制

**Architecture:** PlaybackService (MediaSessionService) 持有 ExoPlayer + MediaSession，PlayerViewModel 通过 MediaController 发送命令，通过 PlayerStateBridge 接收自定义状态（频谱、AB Loop、Sleep Timer），UI 层保持不变

**Tech Stack:** Media3 ExoPlayer, Media3 MediaSession, Media3 MediaController, Hilt, Kotlin Coroutines & Flow

## Global Constraints

- minSdk 24, targetSdk 35, compileSdk 37
- Kotlin 2.3.21
- Media3 version per libs.versions.toml (already dependency)
- 必须保留现有所有播放功能: Queue, Favorite, Download, SetRingtone, SleepTimer, ABLoop, Speed, Shuffle, Repeat, PiP
- 所有 Composable 必须保持现有 API 签名不变
- AndroidManifest.xml 中 service 必须声明 foregroundServiceType="mediaPlayback"

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `core/player/PlayerStateBridge.kt` | **新建** | 单例 StateFlow，Service→ViewModel 传递自定义状态 |
| `core/player/PlaybackService.kt` | **新建** | MediaSessionService，持有 ExoPlayer + MediaSession |
| `core/player/PlaybackServiceCallback.kt` | **新建** | MediaSession.Callback，处理播放命令 + 自定义命令 |
| `core/player/ExoPlayerEngine.kt` | **删除** | 逻辑迁移至 PlaybackService + Callback |
| `core/player/PlayerEngine.kt` | **删除** | 接口不再需要 |
| `presentation/player/PlayerViewModel.kt` | **重写** | 改用 MediaController + PlayerStateBridge |
| `presentation/player/PlayerScreen.kt` | **小幅修改** | 传递 context 给 ViewModel (如需) |
| `core/player/model/PlayerState.kt` | **保留** | 新增 `isLoading` 来源适配 |
| `core/player/visualizer/VisualizerCapture.kt` | **保留** | 移至 Service 中实例化 |
| `core/player/visualizer/FFTProcessor.kt` | **保留** | 不变 |
| `AndroidManifest.xml` | **修改** | 添加 service + uses-permission |
| `core/di/AppModule.kt` | **修改** | 可选，若需手动提供依赖 |

---

### Task 1: Manifest + Gradle 配置

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts:52-53`

**Interfaces:**
- Produces: `PlaybackService` 声明 + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 权限

- [ ] **Step 1: 添加 FOREGROUND_SERVICE 权限**

在 `AndroidManifest.xml` 的 `<manifest>` 块内，其他 `<uses-permission>` 之后添加：

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- [ ] **Step 2: 声明 PlaybackService**

在 `AndroidManifest.xml` 的 `<application>` 块内，`</activity>` 之后添加：

```xml
<service
    android:name=".core.player.PlaybackService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

- [ ] **Step 3: 开启 shrinkResources**

修改 `app/build.gradle.kts` 第 54 行:

```kotlin
// 旧:
isShrinkResources = false
// 新:
isShrinkResources = true
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/build.gradle.kts
git commit -m "chore: add MediaSessionService manifest entry, foreground service perms, enable shrinkResources"
```

---

### Task 2: 创建 PlayerStateBridge

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/player/PlayerStateBridge.kt`

**Interfaces:**
- Produces: `PlayerStateBridge.customState: StateFlow<CustomPlayerState>`, `PlayerStateBridge.update((CustomPlayerState) -> CustomPlayerState)`

- [ ] **Step 1: 创建文件**

```kotlin
package com.xxh.ringbones.core.player

import com.xxh.ringbones.core.player.model.ABLoop
import com.xxh.ringbones.core.player.model.EqPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge for custom playback state that flows from [PlaybackService] to
 * [com.xxh.ringbones.presentation.player.PlayerViewModel].
 *
 * Standard playback state (isPlaying, progress, repeatMode, etc.) is obtained
 * directly from [androidx.media3.session.MediaController]. This bridge carries
 * the non-standard state that MediaSession doesn't model:
 * - Spectrum visualizer FFT magnitudes
 * - A–B loop region
 * - Sleep timer remaining minutes
 * - Active EQ preset
 * - Progress polling (at higher frequency than MediaController)
 */
data class CustomPlayerState(
    val progress: Long = 0,
    val visualizerData: List<Float> = emptyList(),
    val abLoop: ABLoop? = null,
    val sleepTimerMinutes: Int? = null,
    val eqPreset: EqPreset = EqPreset.FLAT,
)

@Singleton
class PlayerStateBridge @Inject constructor() {
    private val _customState = MutableStateFlow(CustomPlayerState())
    val customState: StateFlow<CustomPlayerState> = _customState.asStateFlow()

    /** Atomically update the custom state from the Service thread. */
    fun update(transform: (CustomPlayerState) -> CustomPlayerState) {
        _customState.update(transform)
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/PlayerStateBridge.kt
git commit -m "feat: add PlayerStateBridge for Service-to-ViewModel custom state"
```

---

### Task 3: 创建 PlaybackService

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/player/PlaybackService.kt`

**Interfaces:**
- Consumes: `PlayerStateBridge` (Hilt injected), `PlaybackServiceCallback` (Task 4)
- Produces: `PlaybackService` — Android Service, MediaSession 持有者

- [ ] **Step 1: 创建 PlaybackService**

```kotlin
package com.xxh.ringbones.core.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.xxh.ringbones.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Notification channel ID used for the playback notification. */
private const val PLAYBACK_CHANNEL_ID = "playback_channel"

/** Notification channel display name shown in system settings. */
private const val PLAYBACK_CHANNEL_NAME = "Playback"

/** Notification ID for the foreground service. */
private const val NOTIFICATION_ID = 1001

/**
 * Foreground service that owns the ExoPlayer instance and MediaSession.
 *
 * Implements the standard Media3 MediaSessionService pattern:
 * - [onCreate] creates ExoPlayer + MediaSession + notification channel
 * - [onGetSession] returns the session to connecting MediaControllers
 * - [onTaskRemoved] stops the service when the app is swiped away and no
 *   content is playing
 * - [onDestroy] releases player and session resources
 *
 * Custom playback state (AB loop, sleep timer, visualizer, EQ) is published
 * to [PlayerStateBridge] so the ViewModel can observe it.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var playerStateBridge: PlayerStateBridge

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer = player

        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notificationProvider = DefaultMediaNotificationProvider(this)
        notificationProvider.setSmallIcon(com.xxh.ringbones.R.mipmap.musicology)

        setMediaNotificationProvider(notificationProvider)

        val sessionCallback = PlaybackServiceCallback(
            service = this,
            exoPlayer = player,
            playerStateBridge = playerStateBridge,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
    }

    @UnstableApi
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer ?: run {
            stopSelf()
            return
        }
        if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player?.stop()
            release()
        }
        exoPlayer?.release()
        mediaSession = null
        exoPlayer = null
        super.onDestroy()
    }

    /** Creates the notification channel required for foreground service. */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            PLAYBACK_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Playback controls"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
```

- [ ] **Step 2: 验证编译（会因 PlaybackServiceCallback 不存在而失败，这是预期的）**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/PlaybackService.kt
git commit -m "feat: add PlaybackService with MediaSession and notification provider"
```

---

### Task 4: 创建 PlaybackServiceCallback（标准命令）

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/player/PlaybackServiceCallback.kt`

**Interfaces:**
- Consumes: `ExoPlayer`, `PlayerStateBridge`
- Produces: `MediaSession.Callback` — 处理标准播放命令 + 自定义命令 placeholder

- [ ] **Step 1: 创建 PlaybackServiceCallback**

```kotlin
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
```

- [ ] **Step 2: 验证编译**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/PlaybackServiceCallback.kt
git commit -m "feat: add PlaybackServiceCallback with standard + custom MediaSession commands"
```

---

### Task 5: 添加 PlayerListener 到 PlaybackService

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/player/PlaybackService.kt` — 在 `onCreate()` 中添加 listener

**Interfaces:**
- Consumes: `PlaybackServiceCallback.onPlaybackStarted()`, `.onPlaybackPaused()`

- [ ] **Step 1: 在 PlaybackService 的 `onCreate()` 末尾添加 Player.Listener**

在 `PlaybackService.kt` 的 `onCreate()` 中，`mediaSession = MediaSession.Builder(...)` 之后、方法结束之前添加：

```kotlin
        // Wire ExoPlayer events to the callback for progress/visualizer lifecycle
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    sessionCallback.onPlaybackStarted()
                } else {
                    sessionCallback.onPlaybackPaused()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Error handling: the ViewModel will observe via MediaController listener
            }
        })
```

- [ ] **Step 2: 验证编译**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/PlaybackService.kt
git commit -m "feat: wire ExoPlayer events to PlaybackServiceCallback"
```

---

### Task 6: 重构 PlayerViewModel

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerViewModel.kt` — **完整重写**

**Interfaces:**
- Consumes: `RingtoneRepository`, `DownloadManager`, `ToggleFavoriteUseCase`, `RecordPlayHistoryUseCase`, `PlayerStateBridge`
- Produces: `state: StateFlow<PlayerState>`, `effects: SharedFlow<PlayerEffect>`, `onEvent(PlayerEvent)`

- [ ] **Step 1: 重写 PlayerViewModel**

```kotlin
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
import com.xxh.ringbones.core.download.DownloadManager
import com.xxh.ringbones.core.download.DownloadStatus
import com.xxh.ringbones.core.player.CustomPlayerState
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
import com.google.common.util.concurrent.MoreExecutors
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

/**
 * ViewModel for the full-screen immersive player.
 *
 * Communicates with [PlaybackService] via [MediaController] for standard
 * playback commands, and observes [PlayerStateBridge] for custom state
 * (visualizer, AB loop, sleep timer, EQ).
 *
 * Side-effects not modeled by MediaSession (download, favorite, set ringtone)
 * remain directly managed here.
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

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PlayerEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<PlayerEffect> = _effects.asSharedFlow()

    private val _isDownloaded = MutableStateFlow(false)

    private var mediaController: MediaController? = null

    /** Ringtone queue in display order, parallel to the MediaController queue. */
    private var ringtoneQueue: List<Ringtone> = emptyList()

    private val ringtoneId: Long = savedStateHandle.get<Long>("ringtoneId") ?: 0L
    private val queueIds: List<Long> = savedStateHandle.get<LongArray>("queueIds")?.toList() ?: emptyList()

    init {
        if (ringtoneId <= 0) {
            _effects.tryEmit(PlayerEffect.NavigateBack)
        } else {
            initialize()
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                // 1. Load queue from Room
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

                // 2. Connect to PlaybackService via MediaController
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, PlaybackService::class.java),
                )
                val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                controllerFuture.addListener(
                    {
                        try {
                            val controller = controllerFuture.get()
                            mediaController = controller
                            setupController(controller, queue)
                        } catch (e: Exception) {
                            _effects.emit(PlayerEffect.ShowSnackbar("Failed to connect: ${e.message}"))
                            _effects.emit(PlayerEffect.NavigateBack)
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
     * Sets up the MediaController listener and loads the initial queue.
     */
    private fun setupController(controller: MediaController, queue: List<Ringtone>) {
        // Convert Ringtone list to MediaItems
        val mediaItems = queue.map { ringtone ->
            MediaItem.Builder()
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

        // Add items to controller queue
        controller.addMediaItems(mediaItems)

        // Start observing controller state
        val controllerListener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                updateStateFromController(player)
            }
        }
        controller.addListener(controllerListener)

        // Initial state
        updateStateFromController(controller)

        // Observe custom state from bridge
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

        // Observe download status
        viewModelScope.launch {
            combine(
                _state,
                _isDownloaded,
            ) { engineState, downloaded ->
                engineState.copy(isDownloaded = downloaded)
            }.collect { merged ->
                // Apply merged state without overwriting custom fields
                _state.update { current ->
                    current.copy(isDownloaded = merged.isDownloaded)
                }
            }
        }

        // Observe download completion
        observeDownloadCompletion()
    }

    /** Reads current state from the MediaController and updates [_state]. */
    private fun updateStateFromController(player: Player) {
        val currentIndex = player.currentMediaItemIndex
        val currentRingtone = ringtoneQueue.getOrNull(currentIndex)

        _state.update { current ->
            current.copy(
                currentRingtone = currentRingtone,
                queue = ringtoneQueue,
                currentIndex = currentIndex,
                isPlaying = player.playWhenReady
                    && player.playbackState == Player.STATE_READY,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
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

    /** Dispatch a user action. */
    fun onEvent(event: PlayerEvent) {
        val controller = mediaController ?: return

        when (event) {
            is PlayerEvent.PlayPause -> {
                if (controller.playWhenReady) controller.pause() else controller.play()
            }
            is PlayerEvent.Seek -> controller.seekTo(event.positionMs)
            is PlayerEvent.Next -> controller.seekToNextMediaItem()
            is PlayerEvent.Previous -> {
                if (controller.currentPosition > 3000) {
                    controller.seekTo(0)
                } else {
                    controller.seekToPreviousMediaItem()
                }
            }
            is PlayerEvent.SkipTo -> {
                controller.seekToDefaultPosition(event.index)
                updateStateFromController(controller)
                recordPlayback()
                _state.value.currentRingtone?.let { checkDownloadStatus(it) }
            }
            is PlayerEvent.ToggleFavorite -> toggleFavorite()
            is PlayerEvent.Download -> downloadCurrentRingtone()
            is PlayerEvent.SetRingtone -> setCurrentAsRingtone()
            is PlayerEvent.ToggleShuffle -> controller.shuffleModeEnabled = !controller.shuffleModeEnabled
            is PlayerEvent.SetRepeatMode -> {
                controller.repeatMode = when (event.mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            }
            is PlayerEvent.SetPlaybackSpeed -> {
                if (event.speed in setOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)) {
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
                    SessionCommand("SET_SLEEP_TIMER", Bundle::class.java),
                    bundle,
                )
            }
            is PlayerEvent.SetABPoint -> {
                val bundle = Bundle().apply { putBoolean("isStart", event.isStart) }
                controller.sendCustomCommand(
                    SessionCommand("SET_AB_POINT", Bundle::class.java),
                    bundle,
                )
            }
            is PlayerEvent.ClearABLoop -> {
                controller.sendCustomCommand(
                    SessionCommand("CLEAR_AB_LOOP", Bundle::class.java),
                    Bundle(),
                )
            }
            is PlayerEvent.SetEqPreset -> {
                val bundle = Bundle().apply { putString("preset", event.preset.name) }
                controller.sendCustomCommand(
                    SessionCommand("SET_EQ_PRESET", Bundle::class.java),
                    bundle,
                )
            }
            is PlayerEvent.DismissError -> _state.update { it.copy(error = null) }
            is PlayerEvent.RemoveFromQueue -> {
                controller.removeMediaItem(event.index)
                ringtoneQueue = ringtoneQueue.toMutableList().also { it.removeAt(event.index) }
                _state.update { it.copy(queue = ringtoneQueue) }
            }
        }

        // Record play history for navigation events
        when (event) {
            is PlayerEvent.Next, is PlayerEvent.Previous -> recordPlayback()
            is PlayerEvent.ToggleFavorite -> { /* handled above */ }
            else -> {}
        }
    }

    // ── Download (unchanged from original) ──

    private fun downloadCurrentRingtone() {
        val ringtone = _state.value.currentRingtone ?: return
        viewModelScope.launch {
            val alreadyExists = withContext(Dispatchers.IO) { isFileDownloaded(ringtone) }
            if (alreadyExists) {
                _effects.emit(PlayerEffect.ShowSnackbar("Already downloaded"))
                return@launch
            }
            downloadManager.enqueue(ringtone)
            _effects.emit(PlayerEffect.ShowSnackbar("Added to download queue"))
        }
    }

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

    private fun checkDownloadStatus(ringtone: Ringtone) {
        viewModelScope.launch {
            val downloaded = withContext(Dispatchers.IO) { isFileDownloaded(ringtone) }
            _isDownloaded.value = downloaded
        }
    }

    private fun isFileDownloaded(ringtone: Ringtone): Boolean {
        val storedPath = ringtone.downloadPath
        if (!storedPath.isNullOrEmpty() && File(storedPath).exists()) return true
        val fileName = ringtone.url.split("/").lastOrNull() ?: return false
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES) ?: context.filesDir
        return File(dir, fileName).exists()
    }

    // ── Set Ringtone (unchanged from original) ──

    private fun setCurrentAsRingtone() {
        val ringtone = _state.value.currentRingtone ?: return
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
                    val existingPath = ringtone.downloadPath
                    if (existingPath != null && File(existingPath).exists()) {
                        existingPath
                    } else {
                        // Use downloadManager to get the file
                        val fileName = ringtone.url.split("/").lastOrNull() ?: "ringtone_${ringtone.id}.mp3"
                        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES) ?: context.filesDir
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
                _effects.emit(PlayerEffect.ShowSnackbar("Permission needed: enable \"Modify system settings\""))
                _effects.emit(PlayerEffect.OpenWriteSettings)
            } catch (e: Exception) {
                _effects.emit(PlayerEffect.ShowSnackbar("Failed to set ringtone: ${e.message}"))
            }
        }
    }

    // ── Favorite (unchanged from original) ──

    private fun toggleFavorite() {
        viewModelScope.launch {
            _state.value.currentRingtone?.let { ringtone ->
                toggleFavoriteUseCase(ringtone.id)
                val newFav = !ringtone.isFavorite
                _state.update { it.copy(isFavorite = newFav) }
            }
        }
    }

    // ── Play History (unchanged from original) ──

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
```

- [ ] **Step 2: 验证编译**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/PlayerViewModel.kt
git commit -m "refactor: rewrite PlayerViewModel to use MediaController + PlayerStateBridge"
```

---

### Task 7: 更新 PlayerScreen

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt` — 仅步骤3

**Interfaces:**
- 无 API 变化 — PlayerScreen 的 `viewModel.onEvent()` 调用保持不变

- [ ] **Step 1: 确认 PlayerScreen 无需修改**

PlayerScreen 的所有 `viewModel.onEvent(PlayerEvent.Xxx)` 调用保持不变。
`state` 的 `StateFlow<PlayerState>` 类型不变。
`effects` 的 `SharedFlow<PlayerEffect>` 类型不变。

**无需修改任何代码** — PlayerScreen 的接口完全保持不变。

- [ ] **Step 2: Commit** (空提交，仅记录验证)

```bash
git commit --allow-empty -m "chore: verify PlayerScreen API unchanged after ViewModel refactor"
```

---

### Task 8: 删除 ExoPlayerEngine 和 PlayerEngine

**Files:**
- Delete: `app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt`
- Delete: `app/src/main/java/com/xxh/ringbones/core/player/PlayerEngine.kt`

**Interfaces:**
- 这些文件已无任何引用

- [ ] **Step 1: 验证无引用**

```bash
grep -r "ExoPlayerEngine\|PlayerEngine" app/src/main/java/ --include="*.kt" 2>/dev/null
```
Expected: no matches (或仅在被删除文件中)

- [ ] **Step 2: 删除文件**

```bash
rm app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt
rm app/src/main/java/com/xxh/ringbones/core/player/PlayerEngine.kt
```

- [ ] **Step 3: 验证编译**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git rm app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt
git rm app/src/main/java/com/xxh/ringbones/core/player/PlayerEngine.kt
git commit -m "refactor: remove ExoPlayerEngine and PlayerEngine, replaced by PlaybackService"
```

---

### Task 9: 检查并修复 Hilt 注入

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/di/AppModule.kt` — 若需要

- [ ] **Step 1: 验证 Hilt 编译**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 如果 Hilt 报错缺少 Provider（针对 PlayerStateBridge）**

检查 `PlayerStateBridge` 是否被 Hilt 自动发现（因为有 `@Singleton` + `@Inject constructor`，应该自动注册）。

`PlaybackService` 使用 `@AndroidEntryPoint` + `@Inject lateinit var` 做字段注入，`PlayerStateBridge` 有 `@Singleton` + `@Inject constructor`，Hilt 应该自动提供。

如果编译通过，此步骤无需修改任何文件。

- [ ] **Step 3: Commit**

```bash
git commit --allow-empty -m "chore: verify Hilt injection works for PlaybackService"
```

---

### Task 10: 端到端验证 + 功能测试清单

- [ ] **Step 1: 编译 debug APK**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -10
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 手动验证清单**

在所有测试设备上验证以下场景：

| # | 测试场景 | 预期行为 |
|---|---------|---------|
| 1 | 从 Home 点击铃声 → PlayerScreen 打开并自动播放 | 播放开始，通知栏出现 |
| 2 | 按下 Home 键回到桌面 | 播放继续，通知栏显示播放状态 |
| 3 | 点击通知暂停按钮 | 播放暂停，通知状态更新 |
| 4 | 点击通知回到应用 | 打开 PlayerScreen，状态同步 |
| 5 | 从通知滑动关闭 | Service 停止，播放停止 |
| 6 | 播放队列中 Next/Previous | 切换到正确曲目 |
| 7 | Seek 拖拽 | 进度跳转正确 |
| 8 | Toggle Favorite | 收藏状态切换 |
| 9 | Download | 文件下载到本地 |
| 10 | Set Ringtone | 铃声设置成功 |
| 11 | Sleep Timer | 计时结束后自动暂停 |
| 12 | A-B Loop | 区间循环播放 |
| 13 | Playback Speed | 变速播放 |
| 14 | Shuffle / Repeat | 随机/循环模式正确 |
| 15 | PiP 模式 | PiP 进入/退出正常 |
| 16 | Queue Sheet 滑动删除 | 队列正确更新 |
| 17 | 锁屏显示 | 锁屏显示封面 + 标题 |

- [ ] **Step 3: Commit** (如无问题)

```bash
git commit --allow-empty -m "qa: end-to-end verification for MediaSession migration passed"
```

---

## 完成检查清单

- [ ] Manifest 包含 PlaybackService 声明 + foregroundServiceType
- [ ] FOREGROUND_SERVICE / POST_NOTIFICATIONS 权限声明
- [ ] PlaybackService 正确创建 MediaSession + ExoPlayer
- [ ] 通知渠道已注册
- [ ] DefaultMediaNotificationProvider 已配置
- [ ] PlaybackServiceCallback 处理所有标准命令
- [ ] 自定义命令 (AB Loop, Sleep Timer, EQ) 正常工作
- [ ] PlayerStateBridge 正确传递自定义状态
- [ ] PlayerViewModel 通过 MediaController 发送命令
- [ ] PlayerViewModel 保留 Download/Favorite/SetRingtone 逻辑
- [ ] PlayerScreen API 不变
- [ ] ExoPlayerEngine 和 PlayerEngine 已删除
- [ ] AppModule 无需修改（Hilt 自动管理新依赖）
- [ ] isShrinkResources = true
- [ ] 所有现有功能在手动测试中正常
- [ ] 退出 Activity 后播放继续