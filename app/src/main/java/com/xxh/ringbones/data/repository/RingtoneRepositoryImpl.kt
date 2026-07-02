package com.xxh.ringbones.data.repository

import androidx.paging.PagingSource
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.mapper.RingtoneMapper
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneRepositoryImpl @Inject constructor(
    private val ringtoneDao: RingtoneDao
) : RingtoneRepository {

    override fun searchByTitle(query: String): Flow<List<Ringtone>> =
        ringtoneDao.searchByTitle(query).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun searchByCategory(category: String): Flow<List<Ringtone>> =
        ringtoneDao.searchByCategory(category).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun getById(id: Long): Flow<Ringtone?> =
        ringtoneDao.getById(id).map { entity -> entity?.let { RingtoneMapper.toDomain(it) } }

    override fun getAllPaged(): PagingSource<Int, Ringtone> =
        ringtoneDao.getAllPaged().map { RingtoneMapper.toDomain(it) } as PagingSource<Int, Ringtone>

    override suspend fun insertAll(ringtones: List<Ringtone>) {
        ringtoneDao.insertAll(ringtones.map { RingtoneMapper.toEntity(it) })
    }

    override suspend fun updatePlayCount(id: Long, count: Int) {
        ringtoneDao.updatePlayCount(id, count)
    }

    override suspend fun updateLastPlayed(id: Long, timestamp: Long) {
        ringtoneDao.updateLastPlayed(id, timestamp)
    }
}
