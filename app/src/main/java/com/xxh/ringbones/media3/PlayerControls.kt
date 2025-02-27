package com.xxh.ringbones.media3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer


@Composable
fun PlayerControls(player: ExoPlayer?) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Button(
            onClick = { player?.playWhenReady = true }
        ) {
            Text("Play")
        }
        Button(
            onClick = { player?.playWhenReady = false }
        ) {
            Text("Pause")
        }
        Button(
            onClick = {
                player?.seekTo(player.currentPosition - 10_000)
            }
        ) {
            Text("Seek -10s")
        }
        Button(
            onClick = {
                player?.seekTo(player.currentPosition + 10_000)
            }
        ) {
            Text("Seek +10s")
        }
    }
}