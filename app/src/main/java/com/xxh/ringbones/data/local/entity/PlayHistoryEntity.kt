package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room entity recording each ringtone playback event. */
@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ringtoneId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val playDuration: Long = 0
)
