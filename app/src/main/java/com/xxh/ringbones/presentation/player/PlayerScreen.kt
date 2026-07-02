package com.xxh.ringbones.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.presentation.player.components.PlayerView

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtone by viewModel.ringtone.collectAsState()
    val exoPlayer by viewModel.exoPlayer.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.savePlayerState()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (ringtone != null && exoPlayer != null) {
                PlayerView(exoPlayer = exoPlayer!!)
            }
        }
    }
}
