package com.xxh.ringbones.core.player

import com.xxh.ringbones.core.player.model.PlayerEvent
import com.xxh.ringbones.core.player.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for the playback engine that powers the player screen.
 *
 * Implementations own the Media3 [androidx.media3.exoplayer.ExoPlayer] lifecycle,
 * queue management, visualizer data, and all playback-related state.
 *
 * The engine exposes a single [StateFlow] of [PlayerState] that the
 * [com.xxh.ringbones.presentation.player.PlayerViewModel] exposes to the UI,
 * and accepts [PlayerEvent] actions to mutate playback.
 */
interface PlayerEngine {
    /** Hot stream of player state. Emits on every state change. */
    val state: StateFlow<PlayerState>

    /**
     * Process a user action.
     *
     * The engine validates the event against current state and applies
     * the appropriate mutation. Invalid events (e.g., seeking past duration)
     * are silently ignored.
     */
    fun handleEvent(event: PlayerEvent)

    /**
     * Release all resources (ExoPlayer, visualizer, coroutines).
     *
     * Must be called exactly once when the engine is no longer needed.
     * After release, [state] stops emitting and [handleEvent] is a no-op.
     */
    fun release()
}