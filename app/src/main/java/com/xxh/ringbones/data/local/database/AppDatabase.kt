package com.xxh.ringbones.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xxh.ringbones.data.local.dao.FavoriteDao
import com.xxh.ringbones.data.local.dao.PlayHistoryDao
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.local.dao.SearchHistoryDao
import com.xxh.ringbones.data.local.entity.FavoriteEntity
import com.xxh.ringbones.data.local.entity.PlayHistoryEntity
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import com.xxh.ringbones.data.local.entity.SearchHistoryEntity

/**
 * Room database holding all ringtone-related tables.
 * Managed by Hilt as a singleton.
 */
@Database(
    entities = [
        RingtoneEntity::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ringtoneDao(): RingtoneDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
