package com.xxh.ringbones.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


class RingtoneViewModel(application: Application) : AndroidViewModel(application) {


    private val ringtoneDao = AppDatabase.getInstance(application).ringtoneDao()

    val allRingtones: Flow<List<Ringtone>> = ringtoneDao.getAllRingtones()

    val ringtonePager = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { ringtoneDao.getAllRingtonesPaging() }
    ).flow

    fun insert(ringtone: Ringtone) {
        viewModelScope.launch {
            ringtoneDao.insert(ringtone)
        }
    }

}