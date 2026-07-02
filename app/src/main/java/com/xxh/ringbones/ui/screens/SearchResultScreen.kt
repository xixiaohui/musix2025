package com.xxh.ringbones.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxh.ringbones.data.Ringtone
import com.xxh.ringbones.data.RingtoneViewModel
import com.xxh.ringbones.ui.components.RingtoneList

@Composable
fun SearchResultScreen(
    searchText: String,
    searchByType: Boolean = true,
    onRingtoneClick: (Ringtone) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: RingtoneViewModel = viewModel()

    LaunchedEffect(searchText, searchByType) {
        if (searchByType) {
            viewModel.searchByType(searchText)
        } else {
            viewModel.searchByTitle("%$searchText%")
        }
    }

    val ringtones by viewModel.ringtones.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            RingtoneList(
                ringtoneList = ringtones,
                loading = false,
                onRingtoneClick = onRingtoneClick
            )
        }
    }
}

/**
 * Simple provider that holds an Application reference for ViewModelFactory.
 * Set by MainActivity before any screen composable is rendered.
 */
object LocalContextProvider {
    lateinit var application: android.app.Application
}
