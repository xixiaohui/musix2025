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
import androidx.compose.material.icons.filled.PictureInPicture
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.xxh.ringbones.presentation.player.components.PlayerColors
import com.xxh.ringbones.presentation.player.components.QueueSheet
import com.xxh.ringbones.presentation.player.components.SleepTimerDialog
import com.xxh.ringbones.presentation.player.components.SpectrumVisualizer
import com.xxh.ringbones.presentation.player.components.TrackInfoCard
import kotlin.time.Duration.Companion.milliseconds

/**
 * Full-screen immersive music player with Apple Music glassmorphism design.
 *
 * All text and surfaces use explicit light-on-dark colors from [PlayerColors]
 * because the [ImmersiveBackground] is always deep-dark regardless of system
 * theme. Using theme-dependent colors like [MaterialTheme.colorScheme.onSurface]
 * would produce invisible dark-on-dark text in light mode.
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
    isInPiPMode: Boolean = false,
    onEnterPiP: () -> Unit = {},
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

    LaunchedEffect(Unit) {
        if (!audioPermissionGranted.value && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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
            onRemove = { index ->
                viewModel.onEvent(PlayerEvent.RemoveFromQueue(index))
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
            // Top bar — transparent with explicit light text
            TopAppBar(
                title = {
                    ringtone?.let { track ->
                        Column {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = PlayerColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = PlayerColors.textSecondary,
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
                            tint = PlayerColors.textPrimary,
                        )
                    }
                },
                actions = {
                    // Picture-in-Picture toggle — hides in PiP mode itself
                    if (!isInPiPMode) {
                        IconButton(onClick = onEnterPiP) {
                            Icon(
                                imageVector = Icons.Default.PictureInPicture,
                                contentDescription = "Enter picture-in-picture",
                                tint = PlayerColors.textSecondary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = PlayerColors.textPrimary,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF7C85F5),
                        )
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
                    isDownloaded = state.isDownloaded,
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
                        tint = PlayerColors.textDisabled,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Extras
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
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp, 0.dp, 24.dp, 0.dp),
                    )
                }
            }

            // Snackbar overlay with explicit light text
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
                        .background(PlayerColors.elevatedSurface)
                        .padding(16.dp, 12.dp, 16.dp, 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = showSnackbar ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlayerColors.textPrimary,
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
