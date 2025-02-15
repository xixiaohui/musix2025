package com.xxh.ringbones.media3

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.xxh.ringbones.R
import kotlinx.coroutines.delay


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
//        Media3AndroidView(player)
//        PlayerControls(player)

        if (player != null) {
            XMLLayoutWithPlayerView(player)
        }

    }
}


@OptIn(UnstableApi::class)
@Composable
fun Media3AndroidView(player: ExoPlayer?) {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                val playView = PlayerView(context).apply {
                    this.player = player
                    setBackgroundColor(Color.GREEN)
                    useController = false

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

@Composable
fun rememberExoPlayer(audioUri: Uri): ExoPlayer {
    val context = LocalContext.current
    return remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(audioUri)
            setMediaItem(mediaItem)
            prepare()
        }
    }
}

@Composable
fun CustomAudioPlayer(audioUri: Uri) {
    val exoPlayer = rememberExoPlayer(audioUri)
    val isPlaying by remember { derivedStateOf { exoPlayer.isPlaying } }
    val duration = exoPlayer.duration.coerceAtLeast(0L)
    var sliderPosition by remember { mutableStateOf(0f) }

    // 更新进度条
    LaunchedEffect(exoPlayer) {
        while (true) {
            sliderPosition = exoPlayer.currentPosition / duration.toFloat()
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 播放/暂停按钮
        IconButton(onClick = {
            if (isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        }) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        // 进度条
        Slider(
            value = sliderPosition,
            onValueChange = { newPosition ->
                sliderPosition = newPosition
                exoPlayer.seekTo((duration * newPosition).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun VolumeControl(exoPlayer: ExoPlayer) {
    var volume by remember { mutableStateOf(1f) }

    // 控制音量
    LaunchedEffect(volume) {
        exoPlayer.volume = volume
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Volume")
        Slider(
            value = volume,
            onValueChange = { newVolume ->
                volume = newVolume
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MusicPlayerScreen() {
    val audioUri = Uri.parse("https://www.example.com/audio.mp3")
    val exoPlayer = rememberExoPlayer(audioUri)

    // 确保播放器在退出时释放
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 音频播放器控件
        CustomAudioPlayer(audioUri = audioUri)

        // 音量控制
        VolumeControl(exoPlayer = exoPlayer)
    }
}

@Composable
fun XMLLayoutInCompose() {
    val context = LocalContext.current

    // 使用 LayoutInflater 加载 XML 布局
    val layoutInflater = LayoutInflater.from(context)
    val viewGroup = layoutInflater.inflate(R.layout.activity_main, null) as ViewGroup

    // 使用 AndroidView 来显示加载的 XML 布局
    AndroidView(
        factory = { viewGroup },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun XMLLayoutWithPlayerView() {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // 使用 LayoutInflater 加载 XML 布局
    val layoutInflater = LayoutInflater.from(context)
    val viewGroup = layoutInflater.inflate(R.layout.activity_main, null) as ViewGroup

    // 获取 XML 布局中的 PlayerView
    val playerView = viewGroup.findViewById<PlayerView>(R.id.player_view)
    playerView.player = exoPlayer  // 设置 ExoPlayer

    // 播放音频
    val mediaItem = MediaItem.fromUri("https://www.example.com/audio.mp3")
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()

    // 使用 AndroidView 来显示加载的 XML 布局
    AndroidView(
        factory = { viewGroup },
        modifier = Modifier.fillMaxSize()
    )

    // 释放 ExoPlayer
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@Composable
fun XMLLayoutWithPlayerView(exoPlayer: ExoPlayer?) {
    val context = LocalContext.current


    // 使用 LayoutInflater 加载 XML 布局
    val layoutInflater = LayoutInflater.from(context)
    val viewGroup = layoutInflater.inflate(R.layout.activity_main, null) as ViewGroup

    // 获取 XML 布局中的 PlayerView
    val playerView = viewGroup.findViewById<PlayerView>(R.id.player_view)
    playerView.player = exoPlayer  // 设置 ExoPlayer


    // 使用 AndroidView 来显示加载的 XML 布局
    AndroidView(
        factory = { viewGroup },
        modifier = Modifier.fillMaxSize(),
        update = { viewGroup ->
            val playerView = viewGroup.findViewById<PlayerView>(R.id.player_view)
            playerView.player = exoPlayer


        }
    )


}