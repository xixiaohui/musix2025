package com.xxh.ringbones.media3

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.xxh.ringbones.R


@Composable
fun Media3PlayerView(
    modifier: Modifier,
    videoUrl: String,
    playerViewModel: PlayerViewModel = viewModel()
) {

    val context = LocalContext.current
    val player by playerViewModel.playerState.collectAsState()

    LaunchedEffect(videoUrl) {
        playerViewModel.initializePlayer(context, videoUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.savePlayerState()
            playerViewModel.releasePlayer()
        }
    }

    Column(
        modifier = modifier
    ) {
        Media3AndroidView(player)
//        PlayerControls(player)
    }
}


@OptIn(UnstableApi::class)
@Composable
fun Media3AndroidView(player: ExoPlayer?) {

    Box(
        modifier = Modifier
            .fillMaxSize()

    ){

        AndroidView(
            factory = { context ->
                val playView = PlayerView(context).apply {
                    this.player = player
                    setBackgroundColor(Color.GREEN)
                    showController()

                    videoSurfaceView?.setBackgroundColor(Color.TRANSPARENT)

                }
                playView
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier = Modifier.fillMaxSize()

        )
    }




}