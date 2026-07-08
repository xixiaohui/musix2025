# MediaSession + 通知栏播放器 — 设计规格

## 概述

将播放器架构从 `PlayerViewModel` 本地持有 `ExoPlayer` 迁移到标准的 `MediaSessionService` 架构，
实现播放器生命周期独立于 UI，并添加基础通知栏控制。

## 目标

- 播放器在后台持续运行（退出 Activity 不中断播放）
- 通知栏显示封面 + 标题 + 播放/暂停按钮
- 符合 Google Play 媒体应用最佳实践
- 保留现有所有功能：Queue、Favorite、Download、SetRingtone、SleepTimer、ABLoop、Speed、Shuffle、Repeat

## 架构

```
PlaybackService (MediaSessionService)
├── ExoPlayer
├── MediaSession (Media3)
│   ├── 自定义 MediaSession.Callback（处理播放命令）
│   ├── AudioFocus 自动管理
│   └── DefaultMediaNotificationProvider（通知生成）
└── PlayerState 流向 MediaController

PlayerViewModel
├── MediaController（与 Service IPC 通信）
├── 发送命令：play/pause/seek/next/previous/skipTo
├── 监听状态：isPlaying/progress/currentIndex/queue 等
├── 保留侧效：Download、Favorite、SetRingtone
└── 暴露 StateFlow<PlayerState> 给 UI

PlayerScreen（UI 层，基本无变化）
```

## 详细设计

### 1. PlaybackService（新建）

**文件**: `core/player/PlaybackService.kt`

```
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(PlaybackServiceCallback(...))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 没有活跃 UI 时停止播放
        if (!exoPlayer.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.release()
        exoPlayer?.release()
        super.onDestroy()
    }
}
```

### 2. MediaSession.Callback

**文件**: `core/player/PlaybackServiceCallback.kt`（新建）

处理所有播放命令，替换现有的 `ExoPlayerEngine.handleEvent()`：

- `onAddMediaItems` — 初始化和队列扩展
- `onSetPlayWhenReady` — 播放/暂停
- `onSeekTo` — 拖拽进度
- `onSeekForward/Back` — 快进快退（可选）
- `onSetRepeatMode` — 循环模式
- `onSetShuffleModeEnabled` — 随机播放
- `onSetPlaybackSpeed` — 变速

### 3. PlayerViewModel 重构

**文件**: `presentation/player/PlayerViewModel.kt`（重写）

```kotlin
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
    private val recordPlayHistoryUseCase: RecordPlayHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val downloadManager: DownloadManager,
) : ViewModel() {

    // 通过 MediaController 连接 PlaybackService
    private val mediaController: MediaController

    init {
        // 1. 构建 MediaController，设置 listener
        // 2. 加载初始 ringtoneId + queueIds 作为 MediaItems
        // 3. 通过 controller.addMediaItems() 注入队列
        // 4. 监听 controller 状态变化 → _state
    }

    fun onEvent(event: PlayerEvent) {
        // 将 PlayerEvent 映射为 MediaController 方法调用
    }
}
```

关键是 PlayerState 不再由 Engine 直接产出，而是从 MediaController 的回调映射过来。

### 4. 通知配置

由 Media3 的 `DefaultMediaNotificationProvider` 自动管理：
- `setMediaNotificationProvider()` 在 `onCreate()` 中调用
- 通知渠道 ID: `"playback_channel"`，名称 "播放控制"
- 前台 Service 类型: `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`
- 点击通知：`PendingIntent` 指向 MainActivity

### 5. 状态同步桥梁

在 `MediaSession.Callback` 中，每次命令执行后更新一个内部 StateFlow，
MediaController 通过 `Listener` 回调获取最新状态。

```
MediaSession.Callback                    PlayerViewModel
     │                                        │
     ├── onSetPlayWhenReady(true) ──→         │
     │   exoPlayer.play()                     │
     │   _state.update { copy(isPlaying) }    │
     │                                        │
     │   ──── via MediaController.Listener ──→ │
     │                                        ├── _state.emit(newState)
     │                                        ├── PlayerScreen recompose
```

### 6. 保留下载/收藏/设置铃声逻辑

这些与播放器核心无关的逻辑**保持在 `PlayerViewModel` 中**不变：

```kotlin
fun onEvent(event: PlayerEvent) {
    when (event) {
        PlayerEvent.PlayPause -> mediaController.playWhenReady = ...

        // 以下独立于播放核心，保留在 ViewModel
        PlayerEvent.ToggleFavorite -> toggleFavorite()
        PlayerEvent.Download -> downloadCurrentRingtone()
        PlayerEvent.SetRingtone -> setCurrentAsRingtone()
        PlayerEvent.SetSleepTimer -> setSleepTimer(event.minutes)
        PlayerEvent.SetABPoint -> // 通过自定义命令发送到 Service
        PlayerEvent.ClearABLoop -> // 通过自定义命令发送到 Service
    }
}
```

### 7. 自定义 Session 命令（AB Loop、Sleep Timer、EQ）

Media3 的 MediaSession 支持自定义 `SessionCommand`。对于标准命令覆盖不到的功能：

```kotlin
// 在 Service 中注册自定义命令
val customCommands = SessionCommand.Builder()
    .addCustomCommand("SET_AB_POINT", Bundle::class.java)
    .addCustomCommand("CLEAR_AB_LOOP", Bundle::class.java)
    .addCustomCommand("SET_SLEEP_TIMER", Bundle::class.java)
    .addCustomCommand("SET_EQ_PRESET", Bundle::class.java)
    .build()

mediaSession.setCustomCommands(customCommands)
```

## 文件影响清单

| 文件 | 操作 |
|------|------|
| `core/player/PlaybackService.kt` | 新建 |
| `core/player/PlaybackServiceCallback.kt` | 新建 |
| `core/player/ExoPlayerEngine.kt` | 删除（逻辑迁移到 Service + Callback） |
| `core/player/PlayerEngine.kt` | 删除（接口不再需要） |
| `core/player/model/PlayerState.kt` | 保留，适配 |
| `core/player/model/PlayerEvent.kt` | 保留，部分事件映射到 MediaController 命令 |
| `core/player/model/PlayerEffect.kt` | 保留不变 |
| `core/player/model/ABLoop.kt` | 保留不变 |
| `core/player/model/EqPreset.kt` | 保留不变 |
| `core/player/model/RepeatMode.kt` | 保留不变 |
| `core/player/visualizer/FFTProcessor.kt` | 保留不变 |
| `core/player/visualizer/VisualizerCapture.kt` | 保留不变 |
| `presentation/player/PlayerViewModel.kt` | 重写 |
| `presentation/player/PlayerScreen.kt` | 小幅调整 |
| `presentation/player/components/*.kt` | 不变 |
| `core/download/DownloadManager.kt` | 不变 |
| `core/di/AppModule.kt` | 可能微调（如需要） |
| `AndroidManifest.xml` | 添加 Service + foregroundServiceType |
| `app/build.gradle.kts` | 不变 |

## 风险点

1. **异步初始化时序** — MediaController 连接是异步的，ViewModel 需要在连接成功后才加载队列和播放
2. **自定义命令兼容性** — AB Loop 和 Sleep Timer 无法用标准 MediaSession 指令表达，需要自定义命令
3. **VisualizerCapture** — 依赖 ExoPlayer 的 `audioSessionId`，原本在 Engine 中管理；迁移后由 Service 管理，需确认跨进程 FFT 数据传递方案
4. **PiP 兼容** — 当前 `MainActivity` 有 PiP 逻辑，需要确认与 MediaSessionService 的交互不受影响

## 不变更范围

- 下载功能不变
- 收藏功能不变
- 设置铃声功能不变
- 所有 UI 组件保持原有设计
- 播放历史记录逻辑不变
- Room 数据库不变
- DataStore 不变
- 路由导航不变