package com.xxh.ringbones.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for the ringtones table. */
@Dao
interface RingtoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ringtones: List<RingtoneEntity>)

    @Query("SELECT * FROM ringtones ORDER BY id ASC")
    fun getAllPaged(): PagingSource<Int, RingtoneEntity>

    @Query("SELECT * FROM ringtones WHERE id = :id")
    fun getById(id: Long): Flow<RingtoneEntity?>

    @Query(
        "SELECT * FROM ringtones WHERE title LIKE :query OR author LIKE :query ORDER BY id ASC"
    )
    fun searchByTitle(query: String): Flow<List<RingtoneEntity>>

    @Query("SELECT * FROM ringtones WHERE category = :category ORDER BY id ASC")
    fun searchByCategory(category: String): Flow<List<RingtoneEntity>>

    @Query("UPDATE ringtones SET playCount = :count WHERE id = :id")
    suspend fun updatePlayCount(id: Long, count: Int)

    @Query("UPDATE ringtones SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    @Query("UPDATE ringtones SET downloadPath = :path WHERE id = :id")
    suspend fun updateDownloadPath(id: Long, path: String)
}
