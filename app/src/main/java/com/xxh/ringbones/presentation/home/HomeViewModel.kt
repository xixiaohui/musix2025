package com.xxh.ringbones.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Number of top ringtones to display in the featured section. */
private const val TOP_RINGTONE_LIMIT = 10

/**
 * ViewModel for the Home screen.
 *
 * Observes category names from Room and loads ringtone counts per category,
 * top-played featured ringtones, and recently added ringtones.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ringtoneRepository: RingtoneRepository
) : ViewModel() {

    /** Observable list of distinct category names from the database. */
    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    /** Category name → ringtone count mapping for the category grid. */
    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String, Int>> = _categoryCounts.asStateFlow()

    /** Observable list of top-played ringtones for the featured section. */
    private val _featuredRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val featuredRingtones: StateFlow<List<Ringtone>> = _featuredRingtones.asStateFlow()

    /** Observable list of recent ringtones across all categories. */
    private val _recentRingtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val recentRingtones: StateFlow<List<Ringtone>> = _recentRingtones.asStateFlow()

    init {
        loadCategories()
        loadCategoryCounts()
        loadFeatured()
    }

    /** Observes distinct categories from Room. */
    private fun loadCategories() {
        viewModelScope.launch {
            ringtoneRepository.getDistinctCategories().collect { list ->
                _availableCategories.value = list
            }
        }
    }

    /** Observes ringtone counts grouped by category. */
    private fun loadCategoryCounts() {
        viewModelScope.launch {
            ringtoneRepository.getCategoryCounts().collect { map ->
                _categoryCounts.value = map
            }
        }
    }

    /** Observes top-played ringtones from Room. */
    private fun loadFeatured() {
        viewModelScope.launch {
            ringtoneRepository.getTopPlayed(TOP_RINGTONE_LIMIT).collect { list ->
                _featuredRingtones.value = list
            }
        }
    }
}