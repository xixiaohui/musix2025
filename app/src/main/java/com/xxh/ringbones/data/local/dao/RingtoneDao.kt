package com.xxh.ringbones.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xxh.ringbones.data.local.entity.CategoryCount
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow

/** Data access object for the ringtones table. */
@Dao
interface RingtoneDao {

    @Query("DELETE FROM ringtones")
    suspend fun deleteAll()

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

    /** Returns distinct category names present in the ringtones table. */
    @Query("SELECT DISTINCT category FROM ringtones ORDER BY category ASC")
    fun getDistinctCategories(): Flow<List<String>>

    /** Returns ringtones in a category ordered by most recently added first. */
    @Query("SELECT * FROM ringtones WHERE category = :category ORDER BY id DESC")
    fun getByCategoryPaged(category: String): PagingSource<Int, RingtoneEntity>

    /** Toggles the favorite status for a ringtone by ID. */
    @Query("UPDATE ringtones SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    /** Explicitly sets the favorite status for a ringtone by ID. */
    @Query("UPDATE ringtones SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    /** Returns ringtones by their IDs, preserving the order of the input list. */
    @Query("SELECT * FROM ringtones WHERE id IN (:ids)")
    fun getByIds(ids: List<Long>): Flow<List<RingtoneEntity>>

    /** Returns the top N most-played ringtones. */
    @Query("SELECT * FROM ringtones ORDER BY playCount DESC LIMIT :limit")
    fun getTopPlayed(limit: Int): Flow<List<RingtoneEntity>>

    /** Returns category names paired with ringtone counts, grouped and ordered. */
    @Query("SELECT category, COUNT(*) as count FROM ringtones GROUP BY category ORDER BY category ASC")
    fun getCategoryCounts(): Flow<List<CategoryCount>>

    /** Returns ringtones whose download URL contains the given domain string. */
    @Query("SELECT * FROM ringtones WHERE url LIKE '%' || :domain || '%' ORDER BY id ASC")
    fun getByUrlDomain(domain: String): Flow<List<RingtoneEntity>>
}
