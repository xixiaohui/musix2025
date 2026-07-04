package com.xxh.ringbones.core.player.model

/**
 * Defines an A–B loop region within the current track.
 *
 * When set, playback loops between [startMs] and [endMs] instead
 * of playing through the entire track.
 *
 * @property startMs Start position in milliseconds (inclusive)
 * @property endMs End position in milliseconds (exclusive)
 */
data class ABLoop(
    val startMs: Long,
    val endMs: Long,
)