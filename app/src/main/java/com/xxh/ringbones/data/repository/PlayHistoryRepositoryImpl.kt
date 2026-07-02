package com.xxh.ringbones.data.repository

import com.xxh.ringbones.data.local.dao.PlayHistoryDao
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.local.entity.PlayHistoryEntity
import com.xxh.ringbones.data.mapper.RingtoneMapper
import com.xxh.ringbones.domain.model.PlayHistory
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayHistoryRepositoryImpl @Inject constructor(
    private val playHistoryDao: PlayHistoryDao,
    private val ringtoneDao: RingtoneDao
) : PlayHistoryRepository {

    override fun getRecentPlays(limit: Int): Flow<List<Ringtone>> =
        playHistoryDao.getRecentPlays(limit).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun getMostPlayed(limit: Int): Flow<List<Ringtone>> =
        playHistoryDao.getMostPlayed(limit).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override suspend fun record(ringtoneId: Long, duration: Long) {
        playHistoryDao.insert(
            PlayHistoryEntity(ringtoneId = ringtoneId, playDuration = duration)
        )
    }
}
