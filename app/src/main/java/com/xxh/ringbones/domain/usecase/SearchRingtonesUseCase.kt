package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Searches ringtones by title (fuzzy) or by category (exact match).
 */
class SearchRingtonesUseCase @Inject constructor(
    private val ringtoneRepository: RingtoneRepository
) {
    operator fun invoke(query: String, byCategory: Boolean): Flow<List<Ringtone>> =
        if (byCategory) {
            ringtoneRepository.searchByCategory(query)
        } else {
            ringtoneRepository.searchByTitle("%$query%")
        }
}
