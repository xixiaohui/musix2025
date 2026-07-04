package com.xxh.ringbones.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.usecase.SearchRingtonesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Category Detail / Search Results screen.
 * Loads ringtones filtered by category or title search query.
 *
 * Navigation arguments are extracted from [SavedStateHandle]:
 * - `query`: the category name or search term
 * - `byCategory`: `true` to filter by category, `false` for title search
 */
@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRingtonesUseCase: SearchRingtonesUseCase
) : ViewModel() {

    /** Raw query string from navigation arguments. */
    private val query: String = savedStateHandle["query"] ?: ""

    /** Whether the query filters by category (true) or by title (false). */
    private val byCategory: Boolean = savedStateHandle["byCategory"] ?: true

    /** Observable list of ringtones for the current query. */
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    /** Display name for the screen header. */
    private val _categoryName = MutableStateFlow("")
    val categoryName: StateFlow<String> = _categoryName.asStateFlow()

    /** Whether data is still loading. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        _categoryName.value = query
        loadRingtones()
    }

    /** Observes ringtones from the use case and updates UI state. */
    private fun loadRingtones() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                searchRingtonesUseCase(query, byCategory).collect { list ->
                    _ringtones.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}