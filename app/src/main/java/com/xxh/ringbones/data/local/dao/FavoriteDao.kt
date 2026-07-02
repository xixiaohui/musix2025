package com.xxh.ringbones.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.FavoriteEntity
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE ringtoneId = :ringtoneId")
    suspend fun deleteByRingtoneId(ringtoneId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE ringtoneId = :ringtoneId)")
    fun isFavorite(ringtoneId: Long): Flow<Boolean>

    @Query("SELECT ringtoneId FROM favorites ORDER BY favoritedAt DESC")
    fun getFavoriteIds(): Flow<List<Long>>

    @Query("""
        SELECT r.* FROM ringtones r
        INNER JOIN favorites f ON r.id = f.ringtoneId
        ORDER BY f.favoritedAt DESC
    """)
    fun getFavoriteRingtones(): Flow<List<RingtoneEntity>>
}
