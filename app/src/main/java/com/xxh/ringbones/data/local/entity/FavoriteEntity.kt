package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for user-favorited ringtones. One entry per ringtone. */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["ringtoneId"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ringtoneId: Long,
    val favoritedAt: Long = System.currentTimeMillis()
)
