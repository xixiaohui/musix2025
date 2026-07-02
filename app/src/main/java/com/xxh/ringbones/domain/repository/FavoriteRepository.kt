package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.flow.Flow

/** Contract for favorite ringtone management. */
interface FavoriteRepository {
    fun getFavorites(): Flow<List<Ringtone>>
    fun isFavorite(ringtoneId: Long): Flow<Boolean>
    suspend fun addFavorite(ringtoneId: Long)
    suspend fun removeFavorite(ringtoneId: Long)
}
