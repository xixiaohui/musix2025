package com.xxh.ringbones.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.usecase.GetFavoriteRingtonesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Favorites list screen.
 *
 * Observes all user-favorited ringtones from the database via
 * [GetFavoriteRingtonesUseCase]. The use case returns a reactive
 * [Flow] that emits whenever favorites are added or removed.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoriteRingtonesUseCase: GetFavoriteRingtonesUseCase
) : ViewModel() {

    /** Observable list of all favorited ringtones. */
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    /** Whether the initial data load is still in progress. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFavorites()
    }

    /** Observes favorited ringtones from the use case. */
    private fun loadFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getFavoriteRingtonesUseCase().collect { list ->
                    _ringtones.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}