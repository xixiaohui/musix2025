package com.xxh.ringbones.domain.model

/** Domain model for a playback history entry. */
data class PlayHistory(
    val id: Long,
    val ringtone: Ringtone,
    val playedAt: Long,
    val playDuration: Long
)
