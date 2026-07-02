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

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRingtonesUseCase: SearchRingtonesUseCase
) : ViewModel() {

    private val query: String = savedStateHandle["query"] ?: ""
    private val byCategory: Boolean = savedStateHandle["byCategory"] ?: true

    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    init {
        viewModelScope.launch {
            searchRingtonesUseCase(query, byCategory).collect { list ->
                _ringtones.value = list
            }
        }
    }
}
