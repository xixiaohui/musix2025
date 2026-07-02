package com.xxh.ringbones.domain.model

/**
 * Domain model representing a ringtone track.
 * Pure Kotlin — no Room, no Android dependencies.
 */
data class Ringtone(
    val id: Long,
    val title: String,
    val author: String,
    val duration: String,
    val url: String,
    val mimeType: String,
    val category: String,
    val coverImageUrl: String? = null,
    val fileSize: Long = 0,
    val downloadPath: String? = null,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
    val isFavorite: Boolean = false
)
