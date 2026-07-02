package com.xxh.ringbones.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RingtoneViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val ringtoneDao = AppDatabase.getInstance(application).ringtoneDao()

    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    val ringtonePager = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { ringtoneDao.getAllRingtonesPaging() }
    ).flow

    private var searchJob: Job? = null

    fun searchByType(typeName: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            ringtoneDao.searchRingtoneByTypeExactly(typeName).collect { list ->
                _ringtones.value = list
            }
        }
    }

    fun searchByTitle(title: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            ringtoneDao.searchRingtoneByTitle(title).collect { list ->
                _ringtones.value = list
            }
        }
    }

    fun insert(ringtone: Ringtone) {
        viewModelScope.launch {
            ringtoneDao.insert(ringtone)
        }
    }

    fun insert(title: String, author: String, time: String, url: String, type: String) {
        viewModelScope.launch {
            ringtoneDao.insert(
                Ringtone(
                    title = title,
                    author = author,
                    time = time,
                    url = url,
                    type = type
                )
            )
        }
    }

    fun deleteRingtone(ringtone: Ringtone) {
        viewModelScope.launch {
            ringtoneDao.delete(ringtone)
        }
    }
}
