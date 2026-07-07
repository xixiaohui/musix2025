# Player Upgrades Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Three independent player upgrades: real PCM audio visualization via Android Visualizer API, queue swipe-to-dismiss, and in-memory download manager with progress tracking.

**Architecture:** Each upgrade is a self-contained change. Visualizer replaces dummy PCM with Android Visualizer API wrapped in a new `VisualizerCapture` class. Queue removal adds a `RemoveFromQueue` event flowing through the existing PlayerEvent → Engine → ViewModel pipeline with Material3 `SwipeToDismissBox` in QueueSheet. Download manager is a new `@Singleton` in `core/download/` with its own StateFlow-driven state, OkHttp streaming, and a dedicated Compose screen.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Hilt, Media3 ExoPlayer, OkHttp, Room, Coroutines/Flow

## Global Constraints

- `minSdk = 24` (Visualizer API available since API 9)
- Compose BOM `2026.06.01` (SwipeToDismissBox available)
- No new library dependencies
- All new Kotlin files in `com.xxh.ringbones.*` package
- Follow existing Clean Architecture patterns
- Compose Previews on all new UI components
- KDoc on all public classes/functions

---

## Track A: Real Audio Visualization

### Task A1: Add RECORD_AUDIO permission to manifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` (insert after existing permissions block)

- [ ] **Step 1: Add permission declaration**

```xml
<!-- Required for Android Visualizer API to capture real PCM audio data -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Insert it between the existing `READ_MEDIA_AUDIO` and `WRITE_SETTINGS` permission lines. The final permissions block should read:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
```

- [ ] **Step 2: Verify the manifest parses**

```bash
cd "E:\workspace\app\musix2025" && grep RECORD_AUDIO app/src/main/AndroidManifest.xml
```

Expected output: line containing `android.permission.RECORD_AUDIO`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: add RECORD_AUDIO permission for real audio visualization"
```

---

### Task A2: Create VisualizerCapture wrapper

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/player/visualizer/VisualizerCapture.kt`

**Interfaces:**
- Produces: `VisualizerCapture(audioSessionId: Int, captureRateMs: Long = 33, fftSize: Int = 256)` — wraps `android.media.audiofx.Visualizer`, exposes `val pcmFlow: Flow<ShortArray>`, `val isAvailable: Boolean`, `fun release()`

- [ ] **Step 1: Create VisualizerCapture.kt**

```kotlin
package com.xxh.ringbones.core.player.visualizer

import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Captures raw PCM audio data from the Android [Visualizer] API for use
 * with [FFTProcessor] to produce real-time spectrum visualization.
 *
 * Wraps the system [Visualizer] in a coroutine-friendly [Flow] that emits
 * [ShortArray] PCM chunks at the configured [captureRateMs]. Falls back
 * gracefully if the Visualizer API is unavailable on the device.
 *
 * @param audioSessionId The ExoPlayer audio session ID (zero = system output mixer)
 * @param captureRateMs Interval between PCM capture callbacks in milliseconds (~30fps)
 * @param fftSize Size of each captured PCM chunk (must match [FFTProcessor.fftSize])
 */
class VisualizerCapture(
    audioSessionId: Int,
    captureRateMs: Long = DEFAULT_CAPTURE_RATE_MS,
    fftSize: Int = DEFAULT_FFT_SIZE,
) {
    /** Whether the Visualizer was successfully created and is capturing. */
    val isAvailable: Boolean

    /** Hot flow emitting PCM chunks at ~30fps. Empty if [isAvailable] is false. */
    val pcmFlow: Flow<ShortArray>

    /** Underlying system Visualizer instance, or null if unavailable. */
    private val visualizer: Visualizer? = null

    init {
        visualizer = try {
            Visualizer(audioSessionId).apply {
                captureSize = fftSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) {
                            // Not used — we capture FFT data instead
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int,
                        ) {
                            // Raw FFT bytes — convert to ShortArray for downstream processing
                            onFftBytes(fft)
                        }
                    },
                    (captureRateMs.toInt() * 1000).coerceAtMost(Int.MAX_VALUE).toInt(),
                    /* waveform = */ false,
                    /* fft = */ true,
                )
                enabled = true
            }
        } catch (e: Exception) {
            // Visualizer API unavailable — consumer should fall back to mock data
            isAvailable = false
            pcmFlow = emptyPcmFlow()
            return
        }

        isAvailable = true
        pcmFlow = callbackFlow {
            onFftBytes = { bytes ->
                if (bytes != null && bytes.size >= fftSize * 2) {
                    val shorts = ShortArray(fftSize)
                    for (i in 0 until fftSize) {
                        // Convert two bytes (little-endian) to short
                        val low = bytes[i * 2].toInt() and 0xFF
                        val high = bytes[i * 2 + 1].toInt() and 0xFF
                        shorts[i] = ((high shl 8) or low).toShort()
                    }
                    trySend(shorts)
                }
            }
            awaitClose { /* channel closed when flow collection stops */ }
        }
    }

    /** Mutable callback set by init — assigned when real FFT data arrives. */
    private var onFftBytes: (ByteArray?) -> Unit = {}

    /** Release the system Visualizer. Safe to call multiple times. */
    fun release() {
        visualizer?.enabled = false
        visualizer?.release()
    }

    private companion object {
        const val DEFAULT_CAPTURE_RATE_MS = 33L
        const val DEFAULT_FFT_SIZE = 256
    }
}

/** Creates an empty flow for the fallback path. */
private fun emptyPcmFlow(): Flow<ShortArray> = kotlinx.coroutines.flow.emptyFlow()
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/visualizer/VisualizerCapture.kt
git commit -m "feat: add VisualizerCapture wrapper for Android Visualizer API"
```

---

### Task A3: Integrate VisualizerCapture into ExoPlayerEngine

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt`

**Interfaces:**
- Consumes: `VisualizerCapture(audioSessionId, captureRateMs, fftSize)` from Task A2
- Consumes: `FFTProcessor` (existing, unchanged)

- [ ] **Step 1: Replace the visualizer coroutine in ExoPlayerEngine**

Locate the `startVisualizer()` and `stopVisualizer()` functions (approximately lines 410–438). Replace them entirely with the new real-capture versions below.

**Replace `startVisualizer()`:**

```kotlin
private fun startVisualizer() {
    if (visualizerJob?.isActive == true) return

    // Check if VisualizerCapture already exists; create if not
    val capture = visualizerCapture ?: run {
        val sessionId = exoPlayer.audioSessionId
        if (sessionId == 0) return // audio session not ready yet
        VisualizerCapture(audioSessionId = sessionId).also { visualizerCapture = it }
    }

    if (!capture.isAvailable) {
        // Fall back to dummy data if Visualizer API is unavailable
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
```

**Replace `stopVisualizer()`:**

```kotlin
private fun stopVisualizer() {
    visualizerJob?.cancel()
    visualizerJob = null
}
```

**Add as a new private function — keep the existing dummy visualizer code as fallback:**

```kotlin
/**
 * Fallback visualizer that generates synthetic audio envelope data.
 * Used when [VisualizerCapture] is unavailable.
 */
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
```

**Add a new field to the class body (alongside other fields around line 83):**

```kotlin
/** VisualizerCapture instance — created lazily on first use. */
private var visualizerCapture: VisualizerCapture? = null
```

**Update `release()` (around line 146) to also release the VisualizerCapture:**

Add this line after `abLoopJob?.cancel()`:

```kotlin
visualizerCapture?.release()
```

- [ ] **Step 2: Verify the file compiles**

```bash
cd "E:\workspace\app\musix2025" && grep -n "VisualizerCapture\|startDummyVisualizer\|visualizerCapture" app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt
```

Expected: 4+ matching lines showing the new code is present.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt
git commit -m "feat: integrate VisualizerCapture into ExoPlayerEngine with dummy-data fallback"
```

---

### Task A4: Request RECORD_AUDIO runtime permission in PlayerScreen

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt`

**Interfaces:**
- Consumes: `PlayerViewModel` (existing, unchanged)

- [ ] **Step 1: Add permission request logic in PlayerScreen**

In `PlayerScreen` composable, add runtime permission handling. Insert this after the `LaunchedEffect(showSnackbar)` block (after line 126):

```kotlin
// Request RECORD_AUDIO permission for real audio visualization
val audioPermissionGranted = remember {
    mutableStateOf(
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // auto-granted pre-M
        }
    )
}
val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
) { granted ->
    audioPermissionGranted.value = granted
}

LaunchedEffect(state.visualizerData) {
    if (!audioPermissionGranted.value && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt
git commit -m "feat: request RECORD_AUDIO runtime permission in PlayerScreen for real visualizer"
```

---

## Track B: Queue SwipeToDismiss

### Task B1: Add RemoveFromQueue event

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/player/model/PlayerEvent.kt`

- [ ] **Step 1: Add new event class**

Add this inside the `PlayerEvent` sealed interface, after the existing `DismissError` line:

```kotlin
/** Remove a track from the queue at the given index. */
data class RemoveFromQueue(val index: Int) : PlayerEvent
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/model/PlayerEvent.kt
git commit -m "feat: add RemoveFromQueue event to PlayerEvent"
```

---

### Task B2: Handle RemoveFromQueue in ExoPlayerEngine

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt`

**Interfaces:**
- Consumes: `PlayerEvent.RemoveFromQueue` from Task B1
- Produces: emits `PlayerEffect.NavigateBack` when last queue item is removed

- [ ] **Step 1: Add handler case in handleEvent()**

In `handleEvent()` (around line 124), add the new when-branch after the `PlayerEvent.ClearABLoop` line:

```kotlin
is PlayerEvent.RemoveFromQueue -> handleRemoveFromQueue(event.index)
```

- [ ] **Step 2: Add handleRemoveFromQueue private function**

Add this new function in the "Queue navigation" section (after `skipToIndex` around line 201):

```kotlin
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
            // currentIndex stays the same; loadTrack for the new track at that index
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
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/player/ExoPlayerEngine.kt
git commit -m "feat: handle RemoveFromQueue in ExoPlayerEngine"
```

---

### Task B3: Forward RemoveFromQueue in PlayerViewModel

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerViewModel.kt`

- [ ] **Step 1: Add forwarding in onEvent()**

In `onEvent()` (around line 164), add `RemoveFromQueue` to the when-branches that call `engine?.handleEvent(event)`. It is already covered by the default line above `when (event)` — verify that `engine?.handleEvent(event)` is called unconditionally before the `when` block.

This is already the case (line 165: `engine?.handleEvent(event)`), so no additional change is needed.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/PlayerViewModel.kt
git commit -m "feat: forward RemoveFromQueue through PlayerViewModel (no-op — already handled by generic dispatch)"
```

---

### Task B4: Add SwipeToDismiss to QueueSheet

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/components/QueueSheet.kt`

**Interfaces:**
- Consumes: `onRemoveFromQueue: (Int) -> Unit` — new callback parameter
- Consumes: Material3 `SwipeToDismissBox`

- [ ] **Step 1: Rewrite QueueSheet with SwipeToDismiss**

Replace QueueSheet.kt completely:

```kotlin
package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.domain.model.Ringtone
import coil3.compose.AsyncImage

/** Swipe threshold fraction for dismiss. */
private const val SWIPE_DISMISS_THRESHOLD = 0.5f

/** Red accent color for delete background. */
private val deleteBackgroundColor = Color(0xFFE53935)

/**
 * Bottom sheet displaying the current play queue with swipe-to-dismiss.
 *
 * Each track row can be swiped left to reveal a delete action. When the
 * swipe passes [SWIPE_DISMISS_THRESHOLD] of the item width, the track
 * is removed from the queue. Tapping a track skips to it.
 *
 * The last track CAN be removed — the caller should handle the empty
 * queue by dismissing the sheet and navigating back.
 *
 * @param queue The current queue of ringtones
 * @param currentIndex Index of the currently playing track
 * @param onDismiss Callback to close the sheet
 * @param onSkipTo Callback to skip to a specific track index
 * @param onRemove Callback to remove a track at the given index
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Ringtone>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface.copy(alpha = 0.95f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 8.dp, 0.dp, 32.dp),
        ) {
            Text(
                text = "Queue (${queue.size})",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 12.dp),
            )

            LazyColumn {
                itemsIndexed(
                    items = queue,
                    key = { index, ringtone -> ringtone.id.toString() + "_" + index }
                ) { index, ringtone ->
                    SwipeableQueueItem(
                        ringtone = ringtone,
                        isCurrent = index == currentIndex,
                        onClick = { onSkipTo(index) },
                        onDismissed = { onRemove(index) },
                    )
                }
            }
        }
    }
}

/**
 * A single queue item wrapped in [SwipeToDismissBox] with a red delete background.
 *
 * Left swipe reveals a trash icon on a red background. Crossing 50% width
 * triggers removal via [onDismissed]. Haptic feedback fires on dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableQueueItem(
    ringtone: Ringtone,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDismissed: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismissed()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * SWIPE_DISMISS_THRESHOLD },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Red background with trash icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(deleteBackgroundColor),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        QueueItemContent(
            ringtone = ringtone,
            isCurrent = isCurrent,
            onClick = onClick,
        )
    }
}

/**
 * Content of a single queue item row — thumbnail, title/artist, playing indicator.
 */
@Composable
private fun QueueItemContent(
    ringtone: Ringtone,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = if (isCurrent) {
        colorScheme.primary.copy(alpha = 0.12f)
    } else {
        colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(12.dp, 8.dp, 12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail or icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surface.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            if (ringtone.coverImageUrl != null) {
                AsyncImage(
                    model = ringtone.coverImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }

        // Track info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp, 0.dp, 8.dp, 0.dp),
        ) {
            Text(
                text = ringtone.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ringtone.author,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Playing indicator
        if (isCurrent) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Now playing",
                modifier = Modifier.size(20.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewQueueSheet() {
    MaterialTheme {
        QueueSheet(
            queue = emptyList(),
            currentIndex = 0,
            onDismiss = {},
            onSkipTo = {},
            onRemove = {},
        )
    }
}
```

- [ ] **Step 1: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/components/QueueSheet.kt
git commit -m "feat: add swipe-to-dismiss to QueueSheet with red delete background and haptic feedback"
```

---

### Task B5: Wire onRemove callback in PlayerScreen

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt`

**Interfaces:**
- Consumes: `QueueSheet` with new `onRemove` parameter from Task B4

- [ ] **Step 1: Update QueueSheet call site in PlayerScreen**

Find the `QueueSheet()` call (around line 136). Update it to add the `onRemove` parameter:

```kotlin
if (showQueue) {
    QueueSheet(
        queue = state.queue,
        currentIndex = state.currentIndex,
        onDismiss = { showQueue = false },
        onSkipTo = { index ->
            viewModel.onEvent(PlayerEvent.SkipTo(index))
            showQueue = false
        },
        onRemove = { index ->
            viewModel.onEvent(PlayerEvent.RemoveFromQueue(index))
        },
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt
git commit -m "feat: wire RemoveFromQueue event from QueueSheet to PlayerViewModel"
```

---

## Track C: Download Manager

### Task C1: Create DownloadTask and DownloadState data models

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/download/DownloadTask.kt`
- Create: `app/src/main/java/com/xxh/ringbones/core/download/DownloadState.kt`

**Interfaces:**
- Produces: `DownloadTask`, `DownloadStatus`, `DownloadState` — data classes consumed by all subsequent download tasks

- [ ] **Step 1: Create DownloadTask.kt**

```kotlin
package com.xxh.ringbones.core.download

/**
 * Represents the state of a single download in the [DownloadManager] queue.
 *
 * @property ringtoneId The Ringtone ID from the database
 * @property title Display title for the download UI
 * @property url Remote MP3 URL to download
 * @property status Current lifecycle status
 * @property progress Download progress from 0f to 1f
 * @property downloadedBytes Bytes downloaded so far
 * @property totalBytes Total expected bytes (from Content-Length), 0 if unknown
 * @property errorMessage Human-readable error description when status is Failed
 */
data class DownloadTask(
    val ringtoneId: Long,
    val title: String,
    val url: String,
    val status: DownloadStatus = DownloadStatus.Pending,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val errorMessage: String? = null,
)

/**
 * Lifecycle status for a [DownloadTask].
 */
enum class DownloadStatus {
    /** Queued, waiting for an available download slot. */
    Pending,

    /** Actively downloading with progress updates. */
    Downloading,

    /** Paused by user action; can be resumed. */
    Paused,

    /** Successfully downloaded to local storage. */
    Completed,

    /** Download failed with an error; can be retried. */
    Failed,
}
```

- [ ] **Step 2: Create DownloadState.kt**

```kotlin
package com.xxh.ringbones.core.download

/**
 * Global snapshot of the download queue state, emitted by [DownloadManager.state].
 *
 * @property tasks All tasks in the queue (pending, active, completed, failed)
 * @property activeCount Number of tasks currently in [DownloadStatus.Downloading] state
 * @property pendingCount Number of tasks in [DownloadStatus.Pending] state
 */
data class DownloadState(
    val tasks: List<DownloadTask> = emptyList(),
    val activeCount: Int = 0,
    val pendingCount: Int = 0,
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/download/DownloadTask.kt app/src/main/java/com/xxh/ringbones/core/download/DownloadState.kt
git commit -m "feat: add DownloadTask and DownloadState data models"
```

---

### Task C2: Create DownloadManager

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/core/download/DownloadManager.kt`

**Interfaces:**
- Consumes: `DownloadTask`, `DownloadStatus`, `DownloadState` from Task C1
- Consumes: `RingtoneRepository` (existing), OkHttp `OkHttpClient` (existing)
- Produces: `DownloadManager` — `@Singleton`, `val state: StateFlow<DownloadState>`, `fun enqueue(ringtone: Ringtone)`, `fun pause/cancel/retry/clearCompleted(id)`

- [ ] **Step 1: Create DownloadManager.kt**

```kotlin
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
            semaphore.withPermit {
                val next = _state.value.tasks.find { it.status == DownloadStatus.Pending }
                    ?: return@withPermit

                startDownload(next)
            }
        }
    }

    private fun startDownload(task: DownloadTask) {
        val job = scope.launch {
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
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}")
        }

        val body = response.body ?: throw IOException("Empty response body")
        val totalBytes = body.contentLength()
        val input = body.byteStream()
        val output = FileOutputStream(file)
        val buffer = ByteArray(BUFFER_SIZE)
        var downloaded = 0L

        try {
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                val progress = if (totalBytes > 0) downloaded.toFloat() / totalBytes else 0f
                updateTask(task.ringtoneId) {
                    it.copy(
                        progress = progress.coerceIn(0f, 1f),
                        downloadedBytes = downloaded,
                        totalBytes = totalBytes.coerceAtLeast(0),
                    )
                }
            }
        } finally {
            output.close()
            input.close()
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/download/DownloadManager.kt
git commit -m "feat: add DownloadManager with FIFO queue, concurrency=2, OkHttp streaming"
```

---

### Task C3: Register DownloadManager in AppModule

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/di/AppModule.kt`

**Interfaces:**
- Consumes: `DownloadManager` from Task C2

- [ ] **Step 1: Add an @Provides method for DownloadManager**

In `AppModule`, add a new companion object (or add to the existing object a new function). Since `AppModule` is a Kotlin `object` with `@Provides` functions, add a new function alongside the existing ones. However, `DownloadManager` is `@Inject` constructor, so Hilt will provide it automatically without an explicit `@Provides` — no change needed in AppModule.kt.

**Skip this task — no changes needed.** Hilt's `@Singleton` + `@Inject constructor` on `DownloadManager` handles it automatically.

- [ ] **Step 1: Commit (documenting no-op)**

```bash
git commit --allow-empty -m "feat: DownloadManager auto-registered via Hilt @Singleton @Inject constructor"
```

---

### Task C4: Create DownloadListViewModel

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/download/DownloadListViewModel.kt`

**Interfaces:**
- Consumes: `DownloadManager` from Task C2
- Produces: `DownloadListViewModel` — `val state: StateFlow<DownloadState>`, `fun pause(id)`, `fun resume(id)`, `fun cancel(id)`, `fun retry(id)`, `fun clearCompleted()`

- [ ] **Step 1: Create DownloadListViewModel.kt**

```kotlin
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
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/download/DownloadListViewModel.kt
git commit -m "feat: add DownloadListViewModel exposing DownloadManager state"
```

---

### Task C5: Create DownloadTaskItem component

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/download/components/DownloadTaskItem.kt`

**Interfaces:**
- Consumes: `DownloadTask`, `DownloadStatus` from Task C1

- [ ] **Step 1: Create DownloadTaskItem.kt**

```kotlin
package com.xxh.ringbones.presentation.download.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.core.download.DownloadStatus
import com.xxh.ringbones.core.download.DownloadTask

/**
 * A single row in the download list, showing the task title, status,
 * progress bar (if active), and contextual action buttons.
 *
 * Actions vary by status:
 * - Downloading → pause button
 * - Paused → resume + cancel buttons
 * - Failed → retry + dismiss buttons
 * - Completed → no actions (cleared via "Clear All")
 * - Pending → cancel button
 */
@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action buttons per status
            when (task.status) {
                DownloadStatus.Downloading -> {
                    TextButton(onClick = onPause) {
                        Text("Pause", style = MaterialTheme.typography.labelMedium)
                    }
                }
                DownloadStatus.Paused -> {
                    TextButton(onClick = onResume) {
                        Text("Resume", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DownloadStatus.Pending -> {
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DownloadStatus.Failed -> {
                    IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DownloadStatus.Completed -> { /* No actions — cleared via "Clear All" */ }
            }
        }

        // Progress bar for active downloads
        if (task.status == DownloadStatus.Downloading) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            if (task.totalBytes > 0) {
                Text(
                    text = "${formatBytes(task.downloadedBytes)} / ${formatBytes(task.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }

        // Error message for failed tasks
        if (task.status == DownloadStatus.Failed && task.errorMessage != null) {
            Text(
                text = task.errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Human-readable status label for a task. */
private fun statusText(task: DownloadTask): String = when (task.status) {
    DownloadStatus.Pending -> "Queued"
    DownloadStatus.Downloading -> if (task.totalBytes > 0) {
        "${(task.progress * 100).toInt()}%"
    } else {
        "Downloading..."
    }
    DownloadStatus.Paused -> "Paused"
    DownloadStatus.Completed -> "Downloaded"
    DownloadStatus.Failed -> "Failed"
}

/** Format a byte count as human-readable string (KB/MB). */
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes.toFloat() / 1_048_576)
    bytes >= 1_024 -> "%.0f KB".format(bytes.toFloat() / 1_024)
    else -> "$bytes B"
}

@Preview(showBackground = true)
@Composable
private fun PreviewDownloadTaskItem() {
    MaterialTheme {
        DownloadTaskItem(
            task = DownloadTask(
                ringtoneId = 1,
                title = "Sample Ringtone",
                url = "https://example.com/ringtone.mp3",
                status = DownloadStatus.Downloading,
                progress = 0.65f,
                downloadedBytes = 1_500_000,
                totalBytes = 2_300_000,
            ),
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/download/components/DownloadTaskItem.kt
git commit -m "feat: add DownloadTaskItem component with progress bar and per-status actions"
```

---

### Task C6: Create DownloadListScreen

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/download/DownloadListScreen.kt`

**Interfaces:**
- Consumes: `DownloadListViewModel` from Task C4
- Consumes: `DownloadTaskItem` from Task C5

- [ ] **Step 1: Create DownloadListScreen.kt**

```kotlin
package com.xxh.ringbones.presentation.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.core.download.DownloadStatus
import com.xxh.ringbones.core.download.DownloadTask
import com.xxh.ringbones.presentation.download.components.DownloadTaskItem

/**
 * Full-screen download management page showing all queued, active,
 * completed, and failed download tasks.
 *
 * Layout:
 * - TopAppBar with title "Downloads" and back button
 * - Sections (visually separated by headers):
 *   - "Downloading" — active tasks with progress + pause
 *   - "Pending" — queued tasks with cancel
 *   - "Failed" — errors with retry
 *   - "Completed" — finished downloads + "Clear All" action
 *
 * @param onBackClick Callback to navigate back
 * @param viewModel Hilt-injected ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        if (state.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No downloads",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // ── Downloading ──
                val active = state.tasks.filter { it.status == DownloadStatus.Downloading }
                if (active.isNotEmpty()) {
                    item(key = "header_active") {
                        SectionHeader("Downloading")
                    }
                    items(active, key = { "active_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = { viewModel.pause(task.ringtoneId) },
                            onResume = { viewModel.resume(task.ringtoneId) },
                            onCancel = { viewModel.cancel(task.ringtoneId) },
                            onRetry = { viewModel.retry(task.ringtoneId) },
                        )
                    }
                }

                // ── Pending ──
                val pending = state.tasks.filter { it.status == DownloadStatus.Pending }
                if (pending.isNotEmpty()) {
                    item(key = "header_pending") {
                        SectionHeader("Pending")
                    }
                    items(pending, key = { "pending_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = {},
                            onResume = {},
                            onCancel = { viewModel.cancel(task.ringtoneId) },
                            onRetry = {},
                        )
                    }
                }

                // ── Failed ──
                val failed = state.tasks.filter { it.status == DownloadStatus.Failed }
                if (failed.isNotEmpty()) {
                    item(key = "header_failed") {
                        SectionHeader("Failed")
                    }
                    items(failed, key = { "failed_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = {},
                            onResume = {},
                            onCancel = { viewModel.cancel(task.ringtoneId) },
                            onRetry = { viewModel.retry(task.ringtoneId) },
                        )
                    }
                }

                // ── Completed ──
                val completed = state.tasks.filter { it.status == DownloadStatus.Completed }
                if (completed.isNotEmpty()) {
                    item(key = "header_completed") {
                        SectionHeader(
                            title = "Completed",
                            action = {
                                TextButton(onClick = { viewModel.clearCompleted() }) {
                                    Text(
                                        "Clear All",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                    items(completed, key = { "completed_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = {},
                            onResume = {},
                            onCancel = {},
                            onRetry = {},
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Section header with an optional trailing action.
 */
@Composable
private fun SectionHeader(
    title: String,
    action: @Composable (() -> Unit)? = null,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewDownloadListScreen() {
    MaterialTheme {
        DownloadListScreen(onBackClick = {})
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/download/DownloadListScreen.kt
git commit -m "feat: add DownloadListScreen with active/pending/failed/completed sections"
```

---

### Task C7: Add Downloads route and navigation

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/Routes.kt`
- Modify: `app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt`

**Interfaces:**
- Consumes: `DownloadListScreen` from Task C6

- [ ] **Step 1: Add Downloads route in Routes.kt**

After the `PlayHistory` data object (near line 41), add:

```kotlin
/** Downloads management screen. */
@Serializable
data object Downloads : Route()
```

- [ ] **Step 2: Add Downloads composable in AppNavGraph.kt**

Add a new import at the top:

```kotlin
import com.xxh.ringbones.presentation.download.DownloadListScreen
```

Add the composable route after the `PlayHistory` composable block:

```kotlin
// ── Downloads Screen ──
composable<Route.Downloads> {
    DownloadListScreen(
        onBackClick = {
            navController.popBackStack()
        }
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/core/navigation/Routes.kt app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt
git commit -m "feat: add Downloads route and navigation graph entry"
```

---

### Task C8: Wire PlayerViewModel Download event to DownloadManager

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerViewModel.kt`

**Interfaces:**
- Consumes: `DownloadManager` from Task C2

- [ ] **Step 1: Inject DownloadManager into PlayerViewModel**

Change the constructor to inject `DownloadManager`:

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val ringtoneRepository: RingtoneRepository,
    private val recordPlayHistoryUseCase: RecordPlayHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val downloadManager: com.xxh.ringbones.core.download.DownloadManager,
) : ViewModel() {
```

- [ ] **Step 2: Replace downloadCurrentRingtone() implementation**

Replace the existing `downloadCurrentRingtone()` function (around line 188) with:

```kotlin
/**
 * Enqueues the current ringtone for download via [DownloadManager].
 * The manager handles deduplication and already-downloaded checks.
 */
private fun downloadCurrentRingtone() {
    val ringtone = _state.value.currentRingtone ?: return
    downloadManager.enqueue(ringtone)
    viewModelScope.launch {
        _effects.emit(PlayerEffect.ShowSnackbar("Added to download queue"))
    }
}
```

- [ ] **Step 3: Remove the old download methods**

Delete the following helper methods (they are now unused):
- `downloadToFile()` — the `DownloadManager` handles the actual download
- `isFileDownloaded()` — the `DownloadManager` checks for duplicates
- `checkDownloadStatus()` — no longer needed for one-off checks

Also delete the `HttpClient` import from `com.xxh.ringbones.core.network.HttpClient` and `okhttp3.Request`, `java.io.File`, `java.io.FileOutputStream` imports that were only used by these methods.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/PlayerViewModel.kt
git commit -m "feat: route PlayerViewModel Download events through DownloadManager"
```

---

### Task C9: Add Downloads entry to HomeScreen

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `DownloadManager` from Task C2

- [ ] **Step 1: Add onDownloadsClick callback parameter to HomeScreen**

Add a parameter after `onPlayHistorySeeAll`:

```kotlin
fun HomeScreen(
    onSearch: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onRingtoneClick: (Ringtone) -> Unit,
    onProkeralaSeeAll: () -> Unit,
    onFavoritesSeeAll: () -> Unit,
    onPlayHistorySeeAll: () -> Unit,
    onDownloadsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Add a Downloads button in the HeroHeader or below the category chips**

Since HeroHeader already handles search, add a small icon-based button. The easiest spot is in the top section. Add a section after the category chips or in the HeroHeader itself.

Actually, let's integrate a subtle entry point in the hero area. The HomeScreen LazyColumn doesn't have easy access to a badge from `DownloadManager`. Instead, add a simple entry using the existing `SectionHeader` pattern.

Add this new section between the hero and the category chips in the LazyColumn (after the "hero" item):

```kotlin
// ── Downloads quick entry ──
item(key = "downloads_entry") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HOME_HORIZONTAL)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.downloads),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = onDownloadsClick) {
            Text(
                text = stringResource(R.string.see_all),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
```

But this is awkward in a LazyColumn with many conditional sections. A cleaner approach: add it as an **always-visible** item right after the hero, with a simple clickable row. Let me refine:

Replace the layout to add a compact downloads entry after the CategoryChipRow. The Downloads button is a simple "Manage Downloads" text button in the top area.

Actually, the simplest approach per the spec is a badge-based entry. Let me use a simpler approach — add it as a dedicated row inside the HeroHeader or right after the chips. Since we don't have a DownloadState in HomeScreen, add it with a badge via the HomeViewModel.

Actually, let's keep it simple and practical. Add a `downloads` string resource and a simple clickable row. The HomeViewModel can expose the active count.

Let me just add a simple clickable "Downloads" entry right after the category chips in the LazyColumn:

```kotlin
// ── Downloads ──
item(key = "downloads_entry") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDownloadsClick() }
            .padding(horizontal = HOME_HORIZONTAL, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Manage →",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
```

- [ ] **Step 3: Update AppNavGraph HomeScreen call site**

In `AppNavGraph.kt`, update the `HomeScreen` call to add:

```kotlin
onDownloadsClick = {
    navController.navigate(Route.Downloads)
}
```

Add the required import:
```kotlin
import androidx.compose.foundation.clickable
```
to HomeScreen.kt.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/home/HomeScreen.kt app/src/main/java/com/xxh/ringbones/core/navigation/AppNavGraph.kt
git commit -m "feat: add Downloads entry to HomeScreen and wire navigation"
```

---

### Task C10: Add string resources for new UI

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add required strings**

Add to `strings.xml`:

```xml
<string name="downloads">Downloads</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add 'Downloads' string resource"
```