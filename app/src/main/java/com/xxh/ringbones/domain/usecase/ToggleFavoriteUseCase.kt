package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(ringtoneId: Long) {
        val isFav = favoriteRepository.isFavorite(ringtoneId).first()
        if (isFav) {
            favoriteRepository.removeFavorite(ringtoneId)
        } else {
            favoriteRepository.addFavorite(ringtoneId)
        }
    }
}
