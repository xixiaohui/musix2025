package com.xxh.ringbones.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room entity for user search queries. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
