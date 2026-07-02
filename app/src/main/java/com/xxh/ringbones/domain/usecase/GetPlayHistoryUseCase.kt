package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.core.util.Constants
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlayHistoryUseCase @Inject constructor(
    private val playHistoryRepository: PlayHistoryRepository
) {
    fun getRecentPlays(): Flow<List<Ringtone>> =
        playHistoryRepository.getRecentPlays(Constants.MAX_RECENT_PLAYS)

    fun getMostPlayed(): Flow<List<Ringtone>> =
        playHistoryRepository.getMostPlayed(Constants.MAX_MOST_PLAYED)
}
