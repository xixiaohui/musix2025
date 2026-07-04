package com.xxh.ringbones.core.player.model

/**
 * Playback repeat mode.
 *
 * - [OFF]: Do not repeat; stop at the end of the queue.
 * - [ONE]: Repeat the current track indefinitely.
 * - [ALL]: Repeat the entire queue from the beginning.
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL,
}