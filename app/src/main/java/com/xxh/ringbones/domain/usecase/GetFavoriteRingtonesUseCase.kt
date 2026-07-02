package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteRingtonesUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(): Flow<List<Ringtone>> = favoriteRepository.getFavorites()
}
