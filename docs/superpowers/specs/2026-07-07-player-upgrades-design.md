# Player Upgrades Design

## Overview

Three independent player feature upgrades for the Musix2025 ringtone app:
1. **Real Audio Visualization** — Replace mock FFT data with real PCM from Android Visualizer API
2. **Queue SwipeToDismiss** — Swipe-to-remove tracks from the queue bottom sheet
3. **Offline Download Manager** — Download queue with progress tracking and management UI

All three are independent and can be implemented in parallel.

---

## 1. Real Audio Visualization

### Approach

Use Android `Visualizer` API (`android.media.audiofx.Visualizer`) to capture real PCM from ExoPlayer's audio session, then feed it through the existing `FFTProcessor`.

**Why not Media3 AudioProcessor:** Simpler, zero ExoPlayer config changes, negligible overhead. The fallback to mock data handles devices where the Visualizer API is unavailable.

### Architecture

```
ExoPlayer (audioSessionId)
    │
    ▼
VisualizerCapture (NEW)
    │  android.media.audiofx.Visualizer
    │  setDataCaptureListener(callback, rate=33ms, waveform=false, fft=true)
    │
    ▼  ShortArray (PCM)
FFTProcessor (EXISTING, unchanged)
    │  radix-2 FFT → Hann window → dB normalization
    ▼  List<Float> (magnitudes, 0f–1f)
SpectrumVisualizer (EXISTING, unchanged)
```

### Files

| File | Action | Description |
|------|--------|-------------|
| `core/player/visualizer/VisualizerCapture.kt` | **New** | Wraps `android.media.audiofx.Visualizer`. Creates with ExoPlayer's `audioSessionId`, registers `DataCaptureListener`, emits `Flow<ShortArray>` of PCM chunks |
| `core/player/ExoPlayerEngine.kt` | **Modify** | Replace dummy PCM generation (lines 416-421) with `VisualizerCapture` → `FFTProcessor` pipeline. Fall back to dummy data if Visualizer API fails |
| `AndroidManifest.xml` | **Modify** | Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />` |
| `PlayerScreen.kt` | **Modify** | Request `RECORD_AUDIO` runtime permission on first spectrum display. Handle denial gracefully (fallback already active) |

### VisualizerCapture API

```kotlin
class VisualizerCapture(
    audioSessionId: Int,
    captureRateMs: Long = 33L,  // ~30fps
    fftSize: Int = 256
) {
    val pcmFlow: Flow<ShortArray>  // emits PCM chunks at ~30fps
    val isAvailable: Boolean       // false → use fallback
    fun release()
}
```

### Fallback Strategy

If `Visualizer()` constructor throws (unsupported device, audio session not ready, permission denied):
- Set `isAvailable = false`
- ExoPlayerEngine continues using existing dummy PCM generator
- No feature gap visible to user

### Permissions

- `RECORD_AUDIO` — declared in manifest, requested at runtime before first visualization
- Denial → silent fallback, no error UX (visualizer still works with mock data)

---

## 2. Queue SwipeToDismiss

### Approach

Add `SwipedToDismissBox` (Material3) to each queue item. Left swipe reveals red delete background with trash icon. Passing 50% threshold triggers removal.

Last item in queue CAN be removed → stops playback and closes player.

### Files

| File | Action | Description |
|------|--------|-------------|
| `core/player/model/PlayerEvent.kt` | **Modify** | Add `data class RemoveFromQueue(val index: Int) : PlayerEvent` |
| `core/player/ExoPlayerEngine.kt` | **Modify** | Add `handleRemoveFromQueue(index)` — remove from queue, adjust `currentIndex`, handle last-item case (stop + emit `NavigateBack` effect) |
| `PlayerViewModel.kt` | **Modify** | Forward `RemoveFromQueue` event to engine |
| `presentation/player/components/QueueSheet.kt` | **Modify** | Wrap each item in `SwipeToDismissBox`, red background + trash icon, 50% threshold |

### Edge Cases

| Scenario | Behavior |
|----------|----------|
| Remove current track, queue has others | Remove current, `currentIndex` stays at same position (next track slides in) |
| Remove track before current | `currentIndex` decrements by 1 |
| Remove last remaining track | Stop playback, emit `NavigateBack`, queue is now empty |
| Empty queue after removal | QueueSheet auto-dismisses, PlayerScreen returns to previous screen |

### Interaction Spec

- Swipe left: reveal red background with trash icon
- Threshold: 50% of item width
- Spring-back if released before threshold
- Smooth dismiss animation after threshold crossed
- Haptic feedback on dismiss

---

## 3. Offline Download Manager

### Approach

In-memory `DownloadManager` singleton with StateFlow-driven progress. FIFO queue, concurrency = 2. No WorkManager — downloads run in app process. If app is killed, queued tasks are lost; completed files persist on disk.

### Module Structure

```
core/download/
├── DownloadManager.kt      # Queue scheduling, pause/cancel/retry
├── DownloadTask.kt         # Per-task state data class
└── DownloadState.kt        # Global download state
```

### Data Model

```kotlin
data class DownloadTask(
    val ringtoneId: Long,
    val title: String,
    val url: String,
    val status: DownloadStatus,
    val progress: Float,          // 0f–1f
    val downloadedBytes: Long,
    val totalBytes: Long,
)

enum class DownloadStatus {
    Pending, Downloading, Paused, Completed, Failed
}

data class DownloadState(
    val tasks: List<DownloadTask>,
    val activeCount: Int,   // currently downloading (max 2)
    val pendingCount: Int,  // queued
)
```

### DownloadManager API

```kotlin
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext context: Context,
    ringtoneRepository: RingtoneRepository
) {
    val state: StateFlow<DownloadState>

    fun enqueue(ringtone: Ringtone)
    fun pause(ringtoneId: Long)
    fun resume(ringtoneId: Long)
    fun cancel(ringtoneId: Long)
    fun retry(ringtoneId: Long)
    fun clearCompleted()
}
```

### Download Pipeline

```
enqueue(ringtone)
    │  status = Pending, added to queue
    ▼
scheduler loop (coroutine)
    │  picks next Pending task (FIFO)
    │  max 2 concurrent
    ▼
download coroutine
    │  OkHttp streaming GET
    │  reads Content-Length → totalBytes
    │  writes to FileOutputStream in chunks
    │  updates progress after each chunk
    ▼
on success
    │  close file
    │  ringtoneRepository.updateDownloadPath(id, path)
    │  status → Completed
on failure
    │  delete partial file
    │  status → Failed
    │  store error message for retry
```

### Files

| File | Action | Description |
|------|--------|-------------|
| `core/download/DownloadTask.kt` | **New** | Data class + enum |
| `core/download/DownloadState.kt` | **New** | Global state data class |
| `core/download/DownloadManager.kt` | **New** | Singleton, queue scheduling, OkHttp download, StateFlow state |
| `core/di/AppModule.kt` | **Modify** | Bind `DownloadManager` as `@Singleton` |
| `presentation/download/DownloadListScreen.kt` | **New** | Full download management UI |
| `presentation/download/DownloadListViewModel.kt` | **New** | Exposes `DownloadManager.state` to UI |
| `presentation/download/components/DownloadTaskItem.kt` | **New** | Single download row with progress bar + actions |
| `PlayerViewModel.kt` | **Modify** | `PlayerEvent.Download` now calls `downloadManager.enqueue()` |
| `HomeScreen.kt` | **Modify** | Add "Downloads" entry with active-count badge |
| `core/navigation/AppNavGraph.kt` | **Modify** | Add download list route |
| `core/navigation/Routes.kt` | **Modify** | Add `Downloads` route |

### DownloadListScreen UI

- Top section: "Downloading" — active tasks with progress bars, pause/resume buttons
- Middle section: "Pending" — queued tasks with cancel option
- Bottom section: "Completed" — successful downloads, "Clear All" action
- Failed tasks: shown with retry button

### Concurrency & Edge Cases

| Scenario | Behavior |
|----------|----------|
| Duplicate enqueue | Skip if already in queue with same ringtoneId + not Failed |
| Already downloaded | Skip, status → Completed immediately |
| App killed | In-memory queue lost; downloaded files survive on disk; `downloadPath` in Room is authoritative for "already downloaded" check |
| Network error | Status → Failed, partial file deleted, user can retry |
| Disk full | IOException caught, status → Failed with "Insufficient storage" message |

---

## Implementation Order

All three are independent. Recommended order if implementing sequentially:

1. **Real Audio Visualization** — smallest change surface, high visual reward
2. **Queue SwipeToDismiss** — medium, improves core UX
3. **Download Manager** — largest, new module + new screen

---

## Dependencies

- Android `Visualizer` API → available on API 24+ (matches project `minSdk = 24`)
- Material3 `SwipeToDismissBox` → available in Compose BOM `2026.06.01` (project already uses this)
- OkHttp → already a project dependency for download streaming
- No new library dependencies required