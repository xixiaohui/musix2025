package com.xxh.ringbones.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.PlayHistoryRepository
import com.xxh.ringbones.domain.usecase.GetPlayHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Play History list screen.
 *
 * Observes recently played ringtones from the database via
 * [GetPlayHistoryUseCase]. The use case returns a reactive
 * [Flow] that emits whenever a new play event is recorded.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getPlayHistoryUseCase: GetPlayHistoryUseCase,
    private val playHistoryRepository: PlayHistoryRepository,
) : ViewModel() {

    /** Observable list of recently played ringtones, most recent first. */
    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    /** Whether the initial data load is still in progress. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHistory()
    }

    /** Observes recently played ringtones from the use case. */
    private fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getPlayHistoryUseCase.getRecentPlays().collect { list ->
                    _ringtones.value = list
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    /** Removes a ringtone from the play history. */
    fun removeFromHistory(ringtoneId: Long) {
        viewModelScope.launch {
            try {
                playHistoryRepository.removeByRingtoneId(ringtoneId)
            } catch (_: Exception) { }
        }
    }
}