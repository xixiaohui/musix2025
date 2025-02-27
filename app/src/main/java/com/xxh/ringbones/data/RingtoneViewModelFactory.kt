package com.xxh.ringbones.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RingtoneViewModelFactory (private val dao: RingtoneDao): ViewModelProvider.Factory{

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(RingtoneViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return RingtoneViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}