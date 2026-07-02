package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import javax.inject.Inject

class RecordPlayHistoryUseCase @Inject constructor(
    private val playHistoryRepository: PlayHistoryRepository
) {
    suspend operator fun invoke(ringtoneId: Long, duration: Long = 0) {
        playHistoryRepository.record(ringtoneId, duration)
    }
}
