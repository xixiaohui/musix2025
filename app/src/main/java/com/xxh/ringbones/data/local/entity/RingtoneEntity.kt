package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the ringtones table.
 * Represents a downloadable ringtone track.
 */
@Entity(tableName = "ringtones")
data class RingtoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room query result for category-level aggregation.
 *
 * @property category Category name
 * @property count Number of ringtones in this category
 */
data class CategoryCount(
    val category: String,
    val count: Int
)
