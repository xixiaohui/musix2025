package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.flow.Flow

/** Contract for ringtone data access. Implemented by the data layer. */
interface RingtoneRepository {
    fun searchByTitle(query: String): Flow<List<Ringtone>>
    fun searchByCategory(category: String): Flow<List<Ringtone>>
    fun getById(id: Long): Flow<Ringtone?>
    fun getAllPaged(): androidx.paging.PagingSource<Int, Ringtone>
    suspend fun insertAll(ringtones: List<Ringtone>)
    suspend fun updatePlayCount(id: Long, count: Int)
    suspend fun updateLastPlayed(id: Long, timestamp: Long)
    /** Updates the local download path for a ringtone. */
    suspend fun updateDownloadPath(id: Long, path: String)

    /** Toggles the favorite flag for the given ringtone. */
    suspend fun toggleFavorite(id: Long)
    /** Returns ringtones matching the given IDs, preserving order. */
    fun getByIds(ids: List<Long>): Flow<List<Ringtone>>
    /** Returns distinct category names currently in the database. */
    fun getDistinctCategories(): Flow<List<String>>
    /** Returns the top N most-played ringtones. */
    fun getTopPlayed(limit: Int): Flow<List<Ringtone>>
    /** Returns category → count map for the category browse grid. */
    fun getCategoryCounts(): Flow<Map<String, Int>>

    /** Returns ringtones whose download URL contains the given domain string. */
    fun getByUrlDomain(domain: String): Flow<List<Ringtone>>
}
