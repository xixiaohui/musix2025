package com.xxh.ringbones.core.player.model

/**
 * Sealed interface for all user-initiated player actions.
 *
 * Dispatched from UI → [com.xxh.ringbones.presentation.player.PlayerViewModel] →
 * [com.xxh.ringbones.core.player.PlayerEngine.handleEvent].
 */
sealed interface PlayerEvent {
    /** Toggle between play and pause. */
    data object PlayPause : PlayerEvent

    /** Seek to an absolute position in the current track. */
    data class Seek(val positionMs: Long) : PlayerEvent

    /** Skip to the next track in the queue. */
    data object Next : PlayerEvent

    /** Skip to the previous track or restart current if progress > 3s. */
    data object Previous : PlayerEvent

    /** Jump to a specific index in the queue. */
    data class SkipTo(val index: Int) : PlayerEvent

    /** Toggle favorite status for the current track. */
    data object ToggleFavorite : PlayerEvent

    /** Download the current track for offline use. */
    data object Download : PlayerEvent

    /** Set the current track as the device ringtone. */
    data object SetRingtone : PlayerEvent

    /** Toggle shuffle mode on/off. */
    data object ToggleShuffle : PlayerEvent

    /** Set the repeat mode. */
    data class SetRepeatMode(val mode: RepeatMode) : PlayerEvent

    /** Set playback speed multiplier. */
    data class SetPlaybackSpeed(val speed: Float) : PlayerEvent

    /** Start/reset the sleep timer with the given minutes, or null to cancel. */
    data class SetSleepTimer(val minutes: Int?) : PlayerEvent

    /** Mark the current position as the A or B point of an A–B loop. */
    data class SetABPoint(val isStart: Boolean) : PlayerEvent

    /** Clear the active A–B loop. */
    data object ClearABLoop : PlayerEvent

    /** Select an EQ preset. */
    data class SetEqPreset(val preset: EqPreset) : PlayerEvent

    /** Dismiss the currently displayed error. */
    data object DismissError : PlayerEvent

    /** Remove a track from the queue at the given index. */
    data class RemoveFromQueue(val index: Int) : PlayerEvent
}