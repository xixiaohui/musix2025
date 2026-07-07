package com.xxh.ringbones.domain.usecase

import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Toggles the favorite status of a ringtone across both storage systems:
 * - [FavoriteRepository] manages the [favorites] join table (used by HomeScreen, FavoritesScreen)
 * - [RingtoneDao.setFavorite] syncs the [ringtones.isFavorite] column (used by PlayerScreen)
 *
 * Both must be kept in sync so that the PlayerScreen heart icon and the
 * Home/Favorites screens agree on which ringtones are favorited.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val ringtoneDao: RingtoneDao,
) {
    /** Toggles favorite state for [ringtoneId] in both the [favorites] table and [ringtones] table. */
    suspend operator fun invoke(ringtoneId: Long) {
        val isFav = favoriteRepository.isFavorite(ringtoneId).first()
        if (isFav) {
            favoriteRepository.removeFavorite(ringtoneId)
            ringtoneDao.setFavorite(ringtoneId, isFavorite = false)
        } else {
            favoriteRepository.addFavorite(ringtoneId)
            ringtoneDao.setFavorite(ringtoneId, isFavorite = true)
        }
    }
}
