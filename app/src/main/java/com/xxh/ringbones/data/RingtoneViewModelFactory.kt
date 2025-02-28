package com.xxh.ringbones.data

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RingtoneViewModelFactory (private val application: Application): ViewModelProvider.Factory{

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(RingtoneViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return RingtoneViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}