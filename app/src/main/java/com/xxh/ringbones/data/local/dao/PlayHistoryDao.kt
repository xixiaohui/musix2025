package com.xxh.ringbones.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.PlayHistoryEntity
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun insert(history: PlayHistoryEntity)

    @Query("""
        SELECT r.* FROM ringtones r
        INNER JOIN play_history ph ON r.id = ph.ringtoneId
        GROUP BY r.id
        ORDER BY MAX(ph.playedAt) DESC
        LIMIT :limit
    """)
    fun getRecentPlays(limit: Int): Flow<List<RingtoneEntity>>

    @Query("""
        SELECT r.* FROM ringtones r
        INNER JOIN play_history ph ON r.id = ph.ringtoneId
        GROUP BY r.id
        ORDER BY COUNT(ph.id) DESC
        LIMIT :limit
    """)
    fun getMostPlayed(limit: Int): Flow<List<RingtoneEntity>>

    @Query("""
        SELECT ph.* FROM play_history ph
        WHERE ph.ringtoneId = :ringtoneId
        ORDER BY ph.playedAt DESC
        LIMIT :limit
    """)
    fun getHistoryForRingtone(ringtoneId: Long, limit: Int = 20): Flow<List<PlayHistoryEntity>>
}
