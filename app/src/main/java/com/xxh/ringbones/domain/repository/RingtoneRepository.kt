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
}
