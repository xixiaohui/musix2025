package com.xxh.ringbones.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class RingtoneViewModel(private val ringtoneDao: RingtoneDao) : ViewModel(){
//class RingtoneViewModel(application: Application) : AndroidViewModel(application){
//    private val ringtoneDao = AppDatabase.getInstance(application).ringtoneDao()

    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones.asStateFlow()

    val ringtonePager = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { ringtoneDao.getAllRingtonesPaging() }
    ).flow

    init {
        viewModelScope.launch {
            ringtoneDao.getAllRingtones().collect { list ->
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

}