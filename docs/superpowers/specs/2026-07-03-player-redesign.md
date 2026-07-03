# Player Page Redesign — Design Spec

**Date:** 2026-07-03  
**Status:** Approved  
**Scope:** Complete rewrite — UI + playback engine

---

## 1. Goals

- Full-screen immersive dark-themed player (Spotify/Apple Music style)
- Pure Compose UI — zero `AndroidView`, no Media3 native `PlayerView`
- Complete playback feature set: queue, repeat/shuffle, sleep timer, AB-loop, EQ, visualizer
- Real album cover support via Coil, gradient fallback when no cover URL
- Shared element transition from list cards to player album art

---

## 2. Architecture

```
PlayerScreen (Composable)
  ├── PlayerViewModel (Hilt)
  │     ├── PlayerState   — single data class
  │     ├── PlayerEvent   — sealed interface (user actions)
  │     └── PlayerEffect  — sealed interface (one-shot side-effects)
  │
  ├── PlayerEngine (interface, injectable)
  │     └── ExoPlayerEngine (implementation)
  │           ├── ExoPlayer lifecycle
  │           ├── queue management
  │           ├── repeat / shuffle modes
  │           ├── visualizer FFT data (60fps)
  │           └── exposes StateFlow<PlayerState>
  │
  └── UI Component Tree (pure Compose)
```

### 2.1 Dependency Wiring

- `PlayerEngine` → plain class instantiated in ViewModel (one engine per player session), no singleton scope
- `PlayerViewModel` → `@HiltViewModel`, receives `PlayerEngine` + `RingtoneRepository` + `RecordPlayHistoryUseCase` + `SavedStateHandle` (for `ringtoneId` + optional `queueIds`)
- `PlayerScreen` → `hiltViewModel<PlayerViewModel>()`

---

## 3. Data Models

### 3.1 PlayerState

```kotlin
data class PlayerState(
    val currentRingtone: Ringtone? = null,
    val queue: List<Ringtone> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Long = 0,
    val duration: Long = 0,
    val bufferedPercent: Int = 0,
    val isFavorite: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int? = null,
    val abLoop: ABLoop? = null,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val visualizerData: List<Float> = emptyList(),
    val error: PlayerError? = null,
)
```

### 3.2 PlayerEvent

```kotlin
sealed interface PlayerEvent {
    data object PlayPause : PlayerEvent
    data class Seek(val positionMs: Long) : PlayerEvent
    data object Next : PlayerEvent
    data object Previous : PlayerEvent
    data class SkipTo(val index: Int) : PlayerEvent
    data object ToggleFavorite : PlayerEvent
    data object Download : PlayerEvent
    data object SetRingtone : PlayerEvent
    data object ToggleShuffle : PlayerEvent
    data class SetRepeatMode(val mode: RepeatMode) : PlayerEvent
    data class SetPlaybackSpeed(val speed: Float) : PlayerEvent
    data class SetSleepTimer(val minutes: Int?) : PlayerEvent
    data class SetABPoint(val isStart: Boolean) : PlayerEvent
    data object ClearABLoop : PlayerEvent
    data class SetEqPreset(val preset: EqPreset) : PlayerEvent
    data object DismissError : PlayerEvent
}
```

### 3.3 PlayerEffect

```kotlin
sealed interface PlayerEffect {
    data class ShowSnackbar(val message: String) : PlayerEffect
    data object NavigateBack : PlayerEffect
}
```

### 3.4 Supporting Types

```kotlin
enum class RepeatMode { OFF, ONE, ALL }
enum class EqPreset { FLAT, BASS_BOOST, TREBLE_BOOST, VOCAL, CLASSICAL, ROCK, POP, CUSTOM }

data class ABLoop(
    val startMs: Long,
    val endMs: Long,
)
```

---

## 4. UI Layout & Visual Structure

### 4.1 Visual Hierarchy (top to bottom)

```
┌──────────────────────────────┐
│  ← Back     Title(marquee)  ♡│  Semi-transparent TopBar
├──────────────────────────────┤
│         Dynamic Glow         │  Animated gradient background
│                              │
│     ┌──────────────────┐     │
│     │   Album Cover    │     │  280dp, rounded corners, glowing edge
│     │   Coil / fallback│     │  SharedTransition element
│     └──────────────────┘     │
│                              │
│     Track Title (bold)       │
│     Artist Name (muted)      │
│                              │
│  ├──────○═══════○──────┤     │  Custom SeekBar (draggable circle thumb)
│  01:23          03:45        │  Time labels
│                              │
│    ⏮     ▶ / ⏸     ⏭      │  Circular control buttons
│                              │
│  💾 Download  ♡ Fav  🔔 Set  │  Action icon row
│                              │
│  ── Expandable extras ──     │  Glass panel
│  🔄  🔀  1.5x  ⏰  EQ  A⟷B  │
│                              │
│  ▓▓▓▓  Spectrum Visualizer ▓▓│  Real-time FFT bars
│                              │
└──────────────────────────────┘
```

### 4.2 Color Palette

- Background: deep dark (`#0A0A0F`) with animated gradient overlays
- Surface glass panels: `surface.copy(alpha = 0.12f)` with `blur` modifier
- Album cover glow: extracted from cover image palette (Palette API), animated
- Accent: `MaterialTheme.colorScheme.primary` for active controls
- Text: `onSurface` with alpha variations for hierarchy

### 4.3 Typography

- Track title: `headlineSmall`, bold, marquee when overflowing
- Artist: `bodyLarge`, `onSurface.copy(alpha = 0.6f)`
- Time labels: `labelSmall`, tabular numbers

---

## 5. Component Specs

### 5.1 `ImmersiveBackground`
- Deep dark base layer + animated radial gradient based on cover palette
- Backdrop blur on glass panels (compileSdk 37, Android 15 `blur` modifier)
- Transitions smoothly when track changes (1500ms color animation)

### 5.2 `AlbumCover`
- Size: 280dp, corner radius: 24dp
- Loads `ringtone.coverImageUrl` via Coil with crossfade
- Fallback: gradient brush + music note icon (preserve existing `AlumArt` gradient palette)
- Supports `Modifier.sharedElement()` for navigation transition
- Glow shadow: `shadowElevation = 24dp` with accent color tint

### 5.3 `TrackInfo`
- Title: marquee animation when text overflows
- Artist below, muted color
- AnimatedVisibility fade-in on track change

### 5.4 `CustomSeekBar`
- Pure Compose `Canvas` draw
- Track: thin rounded line, played portion in accent color, remaining in white/alpha
- Thumb: circle (12dp), draggable via `Modifier.pointerInput` / `detectHorizontalDragGestures`
- Time labels: `currentPosition / duration` in `mm:ss` format

### 5.5 `PlaybackControls`
- Three circular buttons: prev, play/pause (largest, 64dp), next
- Play/pause icon animated with `AnimatedContent` crossfade
- Tapping prev when progress > 3s restarts current track, else goes to previous

### 5.6 `ActionRow`
- Icon buttons: download, favorite (filled when active), set ringtone
- Evenly spaced row
- Each with touch feedback ripple

### 5.7 `ExtraControls`
- Collapsible section (AnimatedVisibility), glass panel container
- Repeat toggle (OFF → ONE → ALL cycle)
- Shuffle toggle
- Playback speed: 0.5x / 0.75x / 1.0x / 1.25x / 1.5x / 2.0x / 3.0x
- Sleep timer: 15min / 30min / 60min / off
- EQ preset selector (bottom sheet)
- AB-loop: tap A to set start, tap B to set end

### 5.8 `SpectrumVisualizer`
- Vertical bars drawn on Canvas
- Data source: `visualizerData: List<Float>` from `PlayerEngine` (128 FFT bins)
- Gradient coloring: bottom muted → top accent
- Smooth animated bar height changes
- 48dp height, full width

### 5.9 `QueueSheet`
- `ModalBottomSheet` showing current queue
- Current track highlighted with playing indicator
- Drag to reorder (optional, v1: static list)
- Tap to skip, swipe to remove

---

## 6. PlayerEngine Implementation

### 6.1 Key Responsibilities

```kotlin
interface PlayerEngine {
    val state: StateFlow<PlayerState>
    fun handleEvent(event: PlayerEvent)
    fun release()
}
```

### 6.2 ExoPlayer Integration

- Uses Media3 `ExoPlayer` — single instance per engine
- `ExoPlayer.AudioOffloadListener` → FFT computation → `visualizerData`
- Proper lifecycle: release in `onCleared()`
- Auto-advance to next track on completion (respects repeat mode)
- Error handling: catch `PlaybackException`, expose via `state.error`

### 6.3 Queue Management

- On init: receive initial track + optional full queue from navigation args
- `next()` / `previous()`: update index, create new MediaItem, play
- Shuffle: Fisher-Yates shuffle of remaining queue, track original order for unshuffle
- Repeat ONE: set ExoPlayer `repeatMode = REPEAT_MODE_ONE`
- Repeat ALL: auto-advance wraps to index 0

### 6.4 Visualizer

- ExoPlayer `AudioProcessor` or `AudioOffloadListener` to capture PCM samples
- FFT computation on `Dispatchers.Default`
- Emit 128-bin float array at ~30fps via `StateFlow`
- Disable when visualizer is not visible

---

## 7. RingtoneEntity / Ringtone Changes

- `coverImageUrl` already exists in both entity and domain model — no change needed
- `isFavorite` already exists in domain `Ringtone` — verify Room entity has it
- Verify `RingtoneRepository` exposes `getById(id: Long): Flow<Ringtone>` and `toggleFavorite(id: Long)`

---

## 8. Navigation Changes

- Pass `ringtoneId` + `queueIds: LongArray?` as nav args
- `PlayerViewModel` resolves queue from `queueIds` via `RingtoneRepository.getByIds(ids)`
- Support `SharedTransitionScope` for album cover shared element animation

---

## 9. Files to Create / Modify

### New files
| File | Purpose |
|------|---------|
| `core/player/PlayerEngine.kt` | Interface |
| `core/player/ExoPlayerEngine.kt` | Implementation |
| `core/player/model/PlayerState.kt` | State data class |
| `core/player/model/PlayerEvent.kt` | Event sealed interface |
| `core/player/model/PlayerEffect.kt` | Effect sealed interface |
| `core/player/model/RepeatMode.kt` | Enum |
| `core/player/model/EqPreset.kt` | Enum |
| `core/player/model/ABLoop.kt` | Data class |
| `core/player/visualizer/FFTProcessor.kt` | FFT computation |
| `presentation/player/PlayerViewModel.kt` | Rewrite |
| `presentation/player/PlayerScreen.kt` | Rewrite |
| `presentation/player/components/ImmersiveBackground.kt` | Dynamic animated background |
| `presentation/player/components/AlbumCover.kt` | Coil cover + fallback |
| `presentation/player/components/TrackInfo.kt` | Title + artist |
| `presentation/player/components/CustomSeekBar.kt` | Draggable seek bar |
| `presentation/player/components/PlaybackControls.kt` | Play/pause/prev/next |
| `presentation/player/components/ActionRow.kt` | Download/fav/set ringtone |
| `presentation/player/components/ExtraControls.kt` | Repeat/shuffle/speed/timer/EQ/AB |
| `presentation/player/components/SpectrumVisualizer.kt` | FFT bars |
| `presentation/player/components/QueueSheet.kt` | Queue bottom sheet |
| `presentation/player/components/SpeedSelector.kt` | Speed picker |
| `presentation/player/components/SleepTimerDialog.kt` | Timer picker |
| `presentation/player/components/EqSelector.kt` | EQ preset selector |

### Modified files
| File | Change |
|------|--------|
| `domain/model/Ringtone.kt` | Verify `isFavorite` field exists |
| `data/local/entity/RingtoneEntity.kt` | Add `isFavorite` if missing |
| `data/local/dao/RingtoneDao.kt` | Add `toggleFavorite()` query |
| `domain/repository/RingtoneRepository.kt` | Add `toggleFavorite()`, `getByIds()` |
| `data/repository/RingtoneRepositoryImpl.kt` | Implement above |
| `core/navigation/AppNavGraph.kt` | Update player route with queue params |
| `app/build.gradle.kts` | Remove `AndroidView` dependency if unused elsewhere |

### Deleted files
| File | Reason |
|------|--------|
| `presentation/player/components/PlayerView.kt` | Replaced by pure Compose controls |
| `presentation/player/components/DynamicBackground.kt` | Replaced by `ImmersiveBackground` |
| `presentation/player/components/AlbumArt.kt` | Replaced by `AlbumCover` |
| `presentation/player/PlayerViewModel.kt` | Complete rewrite |

---

## 10. Build Order

1. **Models & Engine** — `PlayerState`, `PlayerEvent`, `PlayerEffect`, enums, `PlayerEngine` interface, `ExoPlayerEngine`, `FFTProcessor`
2. **ViewModel** — `PlayerViewModel` rewired to `PlayerEngine`
3. **Core Components** — `ImmersiveBackground`, `AlbumCover`, `CustomSeekBar`, `PlaybackControls`
4. **Feature Components** — `TrackInfo`, `ActionRow`, `ExtraControls`, `SpectrumVisualizer`, `QueueSheet`, dialogs
5. **Screen Assembly** — `PlayerScreen` putting all components together
6. **Navigation** — Update `AppNavGraph` for shared transitions + queue args
7. **Cleanup** — Delete old files, verify compilation