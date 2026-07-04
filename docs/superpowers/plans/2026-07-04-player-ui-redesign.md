# Player Page UI Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apple Music glassmorphism UI — glass cards wrap each section, vertical action buttons, clean spatial layering. All existing functionality preserved.

**Architecture:** 3 new small component files (TrackInfoCard, ActionCard, FavoriteRow) + PlayerScreen layout rewrite. Existing components (AlbumCover, CustomSeekBar, PlaybackControls, ExtraControls, SpectrumVisualizer, QueueSheet, dialogs) are reused with zero or minimal changes.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Coil 3.x, Hilt

## Global Constraints

- Pure Compose UI — zero `AndroidView`, zero XML
- `compileSdk = 37`, `minSdk = 24`, `targetSdk = 35`
- Kotlin 2.3.21, Compose BOM 2026.06.01
- All padding must use 4-positional-arg form (`padding(start, top, end, bottom)` — no named parameters)
- `MaterialTheme.colorScheme` access at `@Composable` top-level, not inside `remember{}`
- Coil 3.x import: `coil3.compose.AsyncImage`
- All existing functionality preserved: download, set ringtone, favorite, queue, extras, visualizer
- `material.icons.extended` already available

---

### Task 1: CustomSeekBar Style Tweak

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/components/CustomSeekBar.kt:32`

**Interfaces:**
- Produces: Same signature — `fun CustomSeekBar(progress: Long, duration: Long, onSeek: (Long) -> Unit, modifier: Modifier)`

- [ ] **Step 1: Thinner track + muted unplayed color**

Change line 32 from `4.dp` to `3.dp`:

```kotlin
/** Track line thickness. */
private val TRACK_HEIGHT = 3.dp
```

And update the unplayed track color at the existing `trackColor` variable (already at `copy(alpha = 0.2f)`, change to `0.15f`):

In the function body, find:
```kotlin
val trackColor = colorScheme.onSurface.copy(alpha = 0.2f)
```
Change to:
```kotlin
val trackColor = colorScheme.onSurface.copy(alpha = 0.15f)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/components/CustomSeekBar.kt
git commit -m "style: thinner seekbar track (3dp), muted unplayed color"
```

---

### Task 2: Create TrackInfoCard — Glass Container

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/player/components/TrackInfoCard.kt`

**Interfaces:**
- Consumes: `TrackInfo` composable, `CustomSeekBar` composable (existing signatures)
- Produces: `fun TrackInfoCard(title, artist, trackKey, progress, duration, onSeek, modifier)`

- [ ] **Step 1: Create TrackInfoCard.kt**

```kotlin
package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass card wrapping [TrackInfo] and [CustomSeekBar].
 *
 * Provides a unified rounded container with subtle surface tint
 * that sits above the immersive background.
 *
 * @param title Track title
 * @param artist Artist/author name
 * @param trackKey Unique key for crossfade animation on track change
 * @param progress Current playback position in milliseconds
 * @param duration Total track duration in milliseconds
 * @param onSeek Callback with absolute seek position in milliseconds
 * @param modifier External modifier
 */
@Composable
fun TrackInfoCard(
    title: String,
    artist: String,
    trackKey: Long,
    progress: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassColor = colorScheme.surface.copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 0.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(glassColor)
            .padding(20.dp, 16.dp, 20.dp, 16.dp),
    ) {
        TrackInfo(
            title = title,
            artist = artist,
            trackKey = trackKey,
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomSeekBar(
            progress = progress,
            duration = duration,
            onSeek = onSeek,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTrackInfoCard() {
    MaterialTheme {
        TrackInfoCard(
            title = "Bohemian Rhapsody",
            artist = "Queen",
            trackKey = 1L,
            progress = 75_000L,
            duration = 240_000L,
            onSeek = {},
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/components/TrackInfoCard.kt
git commit -m "feat: add TrackInfoCard glass container component"
```

---

### Task 3: Create ActionCard — Set Ringtone + Download Buttons

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/player/components/ActionCard.kt`

**Interfaces:**
- Produces: `fun ActionCard(onSetRingtone: () -> Unit, onDownload: () -> Unit, modifier: Modifier)`

- [ ] **Step 1: Create ActionCard.kt**

```kotlin
package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Button row height. */
private val ACTION_ROW_HEIGHT = 52.dp

/**
 * Frosted-glass card with two vertically-stacked action buttons:
 * "Set Ringtone" and "Download".
 *
 * Each row provides a ripple touch target with icon + label.
 *
 * @param onSetRingtone Callback when the Set Ringtone row is tapped
 * @param onDownload Callback when the Download row is tapped
 * @param modifier External modifier
 */
@Composable
fun ActionCard(
    onSetRingtone: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassColor = colorScheme.surface.copy(alpha = 0.10f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp, 0.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(glassColor),
    ) {
        // Set Ringtone row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ACTION_ROW_HEIGHT)
                .clickable(onClick = onSetRingtone)
                .padding(16.dp, 0.dp, 16.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Set Ringtone",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
        }

        HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))

        // Download row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ACTION_ROW_HEIGHT)
                .clickable(onClick = onDownload)
                .padding(16.dp, 0.dp, 16.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Download",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewActionCard() {
    MaterialTheme {
        ActionCard(
            onSetRingtone = {},
            onDownload = {},
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/components/ActionCard.kt
git commit -m "feat: add ActionCard glass component with Set Ringtone + Download"
```

---

### Task 4: Create FavoriteRow — ♡ + ≡ Icon Row

**Files:**
- Create: `app/src/main/java/com/xxh/ringbones/presentation/player/components/FavoriteRow.kt`

**Interfaces:**
- Produces: `fun FavoriteRow(isFavorite: Boolean, onToggleFavorite: () -> Unit, onQueue: () -> Unit, modifier: Modifier)`

- [ ] **Step 1: Create FavoriteRow.kt**

```kotlin
package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Small icon row with favorite toggle and queue access.
 *
 * @param isFavorite Whether the current track is favorited
 * @param onToggleFavorite Callback for favorite toggle
 * @param onQueue Callback to open the queue bottom sheet
 * @param modifier External modifier
 */
@Composable
fun FavoriteRow(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Favorite toggle
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp),
            )
        }

        // Queue
        @Suppress("DEPRECATION")
        IconButton(onClick = onQueue) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavoriteRow() {
    MaterialTheme {
        FavoriteRow(
            isFavorite = true,
            onToggleFavorite = {},
            onQueue = {},
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/components/FavoriteRow.kt
git commit -m "feat: add FavoriteRow component with favorite + queue icons"
```

---

### Task 5: PlayerScreen Layout Rewrite

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt`

**Interfaces:**
- Consumes: `TrackInfoCard`, `ActionCard`, `FavoriteRow` (from Tasks 2–4), plus all existing components
- Produces: Same signature — `fun PlayerScreen(onBackClick: () -> Unit, modifier: Modifier, viewModel: PlayerViewModel)`

- [ ] **Step 1: Rewrite PlayerScreen layout**

Replace the entire file with the glassmorphism layout. The scrollable content column now uses glass cards for grouping:

```kotlin
package com.xxh.ringbones.presentation.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.core.player.model.PlayerEffect
import com.xxh.ringbones.core.player.model.PlayerEvent
import com.xxh.ringbones.core.player.model.RepeatMode
import com.xxh.ringbones.core.util.RingtoneHelper
import com.xxh.ringbones.presentation.player.components.ActionCard
import com.xxh.ringbones.presentation.player.components.AlbumCover
import com.xxh.ringbones.presentation.player.components.EqSelector
import com.xxh.ringbones.presentation.player.components.ExtraControls
import com.xxh.ringbones.presentation.player.components.FavoriteRow
import com.xxh.ringbones.presentation.player.components.ImmersiveBackground
import com.xxh.ringbones.presentation.player.components.PlaybackControls
import com.xxh.ringbones.presentation.player.components.QueueSheet
import com.xxh.ringbones.presentation.player.components.SleepTimerDialog
import com.xxh.ringbones.presentation.player.components.SpectrumVisualizer
import com.xxh.ringbones.presentation.player.components.TrackInfoCard
import kotlin.time.Duration.Companion.milliseconds

/**
 * Full-screen immersive music player with Apple Music glassmorphism design.
 *
 * Layout (top-to-bottom):
 * - Transparent TopBar (back + title)
 * - Immersive gradient background
 * - Album cover with glow
 * - Track info + seekbar in glass card
 * - Circular playback controls
 * - Action card: Set Ringtone + Download
 * - Favorite + Queue icon row
 * - Collapsible extras glass card
 * - Spectrum visualizer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by viewModel.state.collectAsState()
    val effects = viewModel.effects
    val context = LocalContext.current

    // UI-local state for dialogs/sheets
    var showExtraControls by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf<String?>(null) }

    // Consume one-shot effects
    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is PlayerEffect.ShowSnackbar -> showSnackbar = effect.message
                is PlayerEffect.NavigateBack -> onBackClick()
                is PlayerEffect.OpenWriteSettings -> {
                    context.startActivity(RingtoneHelper.openWriteSettingsIntent(context))
                }
            }
        }
    }

    // Snackbar auto-dismiss
    LaunchedEffect(showSnackbar) {
        if (showSnackbar != null) {
            kotlinx.coroutines.delay(2500.milliseconds)
            showSnackbar = null
        }
    }

    val ringtone = state.currentRingtone
    val bgColorIndex = remember(ringtone?.category) {
        kotlin.math.abs(ringtone?.category?.hashCode() ?: 0)
    }

    // ── Dialogs & Sheets ──

    if (showQueue) {
        QueueSheet(
            queue = state.queue,
            currentIndex = state.currentIndex,
            onDismiss = { showQueue = false },
            onSkipTo = { index ->
                viewModel.onEvent(PlayerEvent.SkipTo(index))
                showQueue = false
            },
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentMinutes = state.sleepTimerMinutes,
            onDismiss = { showSleepTimerDialog = false },
            onSelected = { minutes ->
                viewModel.onEvent(PlayerEvent.SetSleepTimer(minutes))
            },
        )
    }

    if (showSpeedSheet) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSpeedSheet = false },
            title = { Text("Playback Speed", style = MaterialTheme.typography.titleLarge) },
            text = {
                com.xxh.ringbones.presentation.player.components.SpeedSelector(
                    currentSpeed = state.playbackSpeed,
                    onSpeedSelected = { speed ->
                        viewModel.onEvent(PlayerEvent.SetPlaybackSpeed(speed))
                        showSpeedSheet = false
                    },
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showSpeedSheet = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showEqSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showEqSheet = false },
            containerColor = colorScheme.surface.copy(alpha = 0.95f),
        ) {
            EqSelector(
                currentPreset = state.eqPreset,
                onPresetSelected = { preset ->
                    viewModel.onEvent(PlayerEvent.SetEqPreset(preset))
                    showEqSheet = false
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Main Layout ──

    Box(modifier = modifier.fillMaxSize()) {
        ImmersiveBackground(paletteIndex = bgColorIndex)

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar — transparent
            TopAppBar(
                title = {
                    ringtone?.let { track ->
                        Column {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } ?: Text("", style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colorScheme.onSurface,
                ),
            )

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(0.dp, 0.dp, 0.dp, 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Loading state
                if (state.isLoading && ringtone == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                    }
                    return@Column
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Album cover
                AlbumCover(
                    coverImageUrl = ringtone?.coverImageUrl,
                    category = ringtone?.category ?: "Music",
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Track info + seekbar glass card
                TrackInfoCard(
                    title = ringtone?.title ?: "",
                    artist = ringtone?.author ?: "",
                    trackKey = ringtone?.id ?: 0L,
                    progress = state.progress,
                    duration = state.duration,
                    onSeek = { viewModel.onEvent(PlayerEvent.Seek(it)) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Playback controls
                PlaybackControls(
                    isPlaying = state.isPlaying,
                    onPrevious = { viewModel.onEvent(PlayerEvent.Previous) },
                    onPlayPause = { viewModel.onEvent(PlayerEvent.PlayPause) },
                    onNext = { viewModel.onEvent(PlayerEvent.Next) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action card — Set Ringtone + Download
                ActionCard(
                    onSetRingtone = { viewModel.onEvent(PlayerEvent.SetRingtone) },
                    onDownload = { viewModel.onEvent(PlayerEvent.Download) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Favorite + Queue row
                FavoriteRow(
                    isFavorite = state.isFavorite,
                    onToggleFavorite = { viewModel.onEvent(PlayerEvent.ToggleFavorite) },
                    onQueue = { showQueue = true },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Extras toggle
                IconButton(onClick = { showExtraControls = !showExtraControls }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showExtraControls) "Hide extras" else "Show extras",
                        tint = colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Extras glass card
                ExtraControls(
                    visible = showExtraControls,
                    repeatMode = state.repeatMode.name,
                    shuffleMode = state.shuffleMode,
                    playbackSpeed = state.playbackSpeed,
                    sleepTimerMinutes = state.sleepTimerMinutes,
                    eqPreset = state.eqPreset.name,
                    onRepeatClick = {
                        val nextMode = when (state.repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.OFF
                        }
                        viewModel.onEvent(PlayerEvent.SetRepeatMode(nextMode))
                    },
                    onShuffleClick = { viewModel.onEvent(PlayerEvent.ToggleShuffle) },
                    onSpeedClick = { showSpeedSheet = true },
                    onTimerClick = { showSleepTimerDialog = true },
                    onEqClick = { showEqSheet = true },
                    onAbLoopClick = {
                        if (state.abLoop != null) {
                            viewModel.onEvent(PlayerEvent.ClearABLoop)
                        } else {
                            viewModel.onEvent(PlayerEvent.SetABPoint(true))
                        }
                    },
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 0.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Spectrum visualizer
                SpectrumVisualizer(
                    magnitudes = state.visualizerData,
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 0.dp),
                )

                // Error display
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp, 0.dp, 24.dp, 0.dp),
                    )
                }
            }

            // Snackbar overlay
            AnimatedVisibility(
                visible = showSnackbar != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 0.dp, 16.dp, 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.surface.copy(alpha = 0.9f))
                        .padding(16.dp, 12.dp, 16.dp, 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = showSnackbar ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPlayerScreen() {
    MaterialTheme {
        ImmersiveBackground(paletteIndex = 0)
    }
}
```

- [ ] **Step 2: Remove unused imports from old layout**

The old layout used `ActionRow` and inline buttons which are no longer needed. The new layout uses `ActionCard` and `FavoriteRow`. The import list in the file above is correct — verify that `ActionRow` and `SpeedSelector` are not imported (they're used via full import in the dialog body).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/xxh/ringbones/presentation/player/PlayerScreen.kt
git commit -m "feat: Apple Music glassmorphism player layout with glass cards"
```

---

### Task 6: Remove Deprecated Icons.Filled.QueueMusic Usage

**Files:**
- Modify: `app/src/main/java/com/xxh/ringbones/presentation/player/components/FavoriteRow.kt`
- Note: This task ensures `Icons.AutoMirrored.Filled.QueueMusic` is used instead of the deprecated `Icons.Filled.QueueMusic`. Already handled in Task 4 code above — verify during review.

- [ ] **Step 1: Verify and commit if needed**

```bash
# Confirm no deprecated QueueMusic usage remains
grep -rn "Icons.Filled.QueueMusic\|Icons.Default.QueueMusic" app/src/main/java/com/xxh/ringbones/presentation/player/
# If clean, skip commit. If found, replace with Icons.AutoMirrored.Filled.QueueMusic
```

---

### Task 7: Full Build Verification

- [ ] **Step 1: Build the project**

```bash
cd E:\workspace\app\musix2025
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Verify all new files exist**

```bash
ls app/src/main/java/com/xxh/ringbones/presentation/player/components/TrackInfoCard.kt
ls app/src/main/java/com/xxh/ringbones/presentation/player/components/ActionCard.kt
ls app/src/main/java/com/xxh/ringbones/presentation/player/components/FavoriteRow.kt
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: final verification and cleanup for player UI redesign"
```