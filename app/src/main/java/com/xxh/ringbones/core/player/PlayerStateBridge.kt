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