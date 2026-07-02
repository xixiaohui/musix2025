package com.xxh.ringbones.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.search.components.RingtoneList

@Composable
fun SearchResultScreen(
    onRingtoneClick: (Ringtone) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtones by viewModel.ringtones.collectAsState()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            RingtoneList(
                ringtoneList = ringtones,
                loading = false,
                onRingtoneClick = onRingtoneClick
            )
        }
    }
}
