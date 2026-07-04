package com.xxh.ringbones.core.player.model

import com.xxh.ringbones.domain.model.Ringtone

/**
 * Single source of truth for the player UI state.
 *
 * Exposed as a [StateFlow] by [com.xxh.ringbones.core.player.PlayerEngine]
 * and consumed by [com.xxh.ringbones.presentation.player.PlayerScreen] through
 * [com.xxh.ringbones.presentation.player.PlayerViewModel].
 *
 * @property currentRingtone The currently loaded/playing ringtone, or null if none
 * @property queue The ordered play queue
 * @property currentIndex Index into [queue] of the current track
 * @property isPlaying Whether playback is actively progressing
 * @property isLoading Whether the player is preparing/buffering
 * @property progress Current playback position in milliseconds
 * @property duration Total duration of the current track in milliseconds
 * @property bufferedPercent Percentage (0–100) of the track that has been buffered
 * @property isFavorite Whether the current track is in the user's favorites
 * @property repeatMode Current repeat mode
 * @property shuffleMode Whether shuffle is active
 * @property playbackSpeed Current speed multiplier (0.5x–3.0x)
 * @property sleepTimerMinutes Remaining minutes on the sleep timer, or null if inactive
 * @property abLoop Active A–B loop region, or null if not set
 * @property eqPreset Active EQ preset
 * @property visualizerData 128 FFT bin magnitudes for the spectrum visualizer
 * @property error Current player error, or null if no error
 * @property isDownloaded Whether the current track has been downloaded to local storage
 */
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
    val isDownloaded: Boolean = false,
)

/**
 * Represents a player-level error that the UI should display.
 *
 * @property message Human-readable error description
 * @property recoverable Whether the user can retry (true) or the error is fatal (false)
 */
data class PlayerError(
    val message: String,
    val recoverable: Boolean = true,
)