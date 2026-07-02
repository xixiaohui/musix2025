package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.Ringtone
import kotlinx.coroutines.flow.Flow

/** Contract for playback history tracking. */
interface PlayHistoryRepository {
    fun getRecentPlays(limit: Int): Flow<List<Ringtone>>
    fun getMostPlayed(limit: Int): Flow<List<Ringtone>>
    suspend fun record(ringtoneId: Long, duration: Long)
}
