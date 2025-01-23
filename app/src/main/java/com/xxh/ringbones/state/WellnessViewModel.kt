package com.xxh.ringbones.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel

class WellnessViewModel : ViewModel() {

    private val _tasks = getWellnessTasks().toMutableStateList()
    val tasks: List<WellnessTask>
        get() = _tasks

    fun remove(item: WellnessTask) {
        _tasks.remove(item)
    }

    private fun getWellnessTasks() = List(30) { i -> WellnessTask(i, "Task # $i") }

    fun changeTaskChecked(item: WellnessTask, checked: MutableState<Boolean>){

        _tasks.find { it.id == item.id }?.let { task ->
            task.checked = checked
        }

    }
}