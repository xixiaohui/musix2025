package com.xxh.ringbones.presentation.prokerala

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

/** Domain string used to filter prokerala ringtones by URL. */
private const val PROKERALA_DOMAIN = "dl.prokerala.com"

/**
 * ViewModel for the Prokerala ringtone list screen.
 *
 * Loads all ringtones whose download URL contains the prokerala domain
 * from the Room database.
 */
@HiltViewModel
class ProkeralaListViewModel @Inject constructor(
    private val ringtoneRepository: RingtoneRepository
) : ViewModel() {

    /** Observable list of all prokerala ringtones. */
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    /** Whether data is still loading. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRingtones()
    }

    /** Observes prokerala ringtones from the repository. */
    private fun loadRingtones() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                ringtoneRepository.getByUrlDomain(PROKERALA_DOMAIN).collect { list ->
                    _ringtones.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}