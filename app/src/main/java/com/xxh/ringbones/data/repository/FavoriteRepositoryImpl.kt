package com.xxh.ringbones.data.repository

import com.xxh.ringbones.data.local.dao.FavoriteDao
import com.xxh.ringbones.data.local.entity.FavoriteEntity
import com.xxh.ringbones.data.mapper.RingtoneMapper
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<Ringtone>> =
        favoriteDao.getFavoriteRingtones().map { entities ->
            entities.map { RingtoneMapper.toDomain(it, isFavorite = true) }
        }

    override fun isFavorite(ringtoneId: Long): Flow<Boolean> =
        favoriteDao.isFavorite(ringtoneId)

    override suspend fun removeFavorite(ringtoneId: Long) {
        favoriteDao.deleteByRingtoneId(ringtoneId)
    }

    override suspend fun addFavorite(ringtoneId: Long) {
        favoriteDao.insert(FavoriteEntity(ringtoneId = ringtoneId))
    }
}
