package com.xxh.ringbones.media3

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.google.android.material.snackbar.Snackbar
import com.xxh.ringbones.PlayActivity
import kotlinx.coroutines.delay
import java.io.File


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
        modifier = modifier.fillMaxWidth()
    ) {

        Media3AndroidView(player)
//        player?.let { XMLLayoutWithPlayerView(context, it) }
    }
}

@Composable
fun getCurrentActivity(): PlayActivity? {
    // 获取当前上下文
    val context = LocalContext.current

    // 转换为 Activity 类型
    return context as? PlayActivity
}


fun addDownloadButton(context: Context, player: ExoPlayer?): ImageButton {
    // 创建 ImageButton
    val downloadButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(100, 100).apply {
            gravity = Gravity.BOTTOM or Gravity.END  // 右下角
            setMargins(0, 0, 80, 10) // 距离底部100dp
        }
        setImageResource(com.xxh.ringbones.R.drawable.download_24px)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(
            ContextCompat.getColor(context, com.xxh.ringbones.R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }

    downloadButton.setOnClickListener {
        AlertDialog.Builder(context)
            .setTitle(context.getString(com.xxh.ringbones.R.string.tips))
            .setMessage(context.getString(com.xxh.ringbones.R.string.downloadTips))
            .setPositiveButton(context.getString(com.xxh.ringbones.R.string.confirm)) { _, _ ->


                //download
                // 获取当前播放的 URL（MediaItem URI）
                val currentMediaItem = player?.currentMediaItem
                val audioUrl = currentMediaItem?.localConfiguration?.uri.toString()

                val activity = context as PlayActivity

                activity.let {
                    Log.v("musixRingtone+", it.localClassName)
                }

                PlayActivity.Utility.downloadMusic(activity, audioUrl)

                Snackbar.make(
                    downloadButton,
                    context.getString(com.xxh.ringbones.R.string.downloadTips2),
                    Snackbar.LENGTH_SHORT
                ).show()
//                Snackbar.make(downloadButton, "当前播放的音频链接 $audioUrl",Snackbar.LENGTH_SHORT).show()

            }
            .setNegativeButton(context.getString(com.xxh.ringbones.R.string.cancel), null)
            .show()
    }
    return downloadButton
}

fun addFavoriteButton(context: Context): ImageButton {
    // 创建 ImageButton
    val favoriteButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(100, 100).apply {
            gravity = Gravity.BOTTOM or Gravity.END  // 右下角
            setMargins(0, 0, 160, 10) // 距离底部100dp
        }
        setImageResource(com.xxh.ringbones.R.drawable.favorite_24px)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(
            ContextCompat.getColor(context, com.xxh.ringbones.R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }

    favoriteButton.setOnClickListener {
        AlertDialog.Builder(context)
            .setTitle(context.getString(com.xxh.ringbones.R.string.tips))
            .setMessage(context.getString(com.xxh.ringbones.R.string.add_to_fav_list))
            .setPositiveButton(context.getString(com.xxh.ringbones.R.string.confirm)) { _, _ ->
                Snackbar.make(
                    favoriteButton,
                    context.getString(com.xxh.ringbones.R.string.add_to_fav_list2),
                    Snackbar.LENGTH_SHORT
                ).show()

            }
            .setNegativeButton(context.getString(com.xxh.ringbones.R.string.cancel), null)
            .show()
    }

    return favoriteButton
}

fun addSetRingtoneButton(context: Context, player: ExoPlayer?): ImageButton {
    // 创建 ImageButton
    val setRingtoneButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(100, 100).apply {
            gravity = Gravity.BOTTOM or Gravity.END  // 右下角
            setMargins(0, 0, 250, 10) // 距离底部100dp
        }
        setImageResource(com.xxh.ringbones.R.drawable.notification_add_24px)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(
            ContextCompat.getColor(context, com.xxh.ringbones.R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }

    setRingtoneButton.setOnClickListener {
        AlertDialog.Builder(context)
            .setTitle(context.getString(com.xxh.ringbones.R.string.tips))
            .setMessage(context.getString(com.xxh.ringbones.R.string.set_ringtone))
            .setPositiveButton(context.getString(com.xxh.ringbones.R.string.confirm)) { _, _ ->


                //设置铃音

                val currentMediaItem = player?.currentMediaItem
                val audioUrl = currentMediaItem?.localConfiguration?.uri.toString()

                val activity = context as PlayActivity

                val file = File(audioUrl)

                if (file.exists()) {
                    PlayActivity.Utility.setRingtone(activity, audioUrl)

                    Snackbar.make(
                        setRingtoneButton,
                        context.getString(com.xxh.ringbones.R.string.set_ringtone_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }else{

                    Snackbar.make(
                        setRingtoneButton,
                        context.getString(com.xxh.ringbones.R.string.set_ringtone3)+ audioUrl,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }


            }
            .setNegativeButton(context.getString(com.xxh.ringbones.R.string.cancel), null)
            .show()
    }

    return setRingtoneButton
}


@SuppressLint("ResourceAsColor")
@OptIn(UnstableApi::class)
@Composable
fun Media3AndroidView(player: ExoPlayer?) {

    val context = LocalContext.current

    val playView = remember {
        PlayerView(context).apply {
            this.player = player
            this.showController()
            this.controllerShowTimeoutMs = 0
            this.controllerHideOnTouch = false

            this.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
            this.defaultArtwork =
                ContextCompat.getDrawable(context, com.xxh.ringbones.R.drawable.ab1_inversions)

            val defaultTimeBar =
                this.findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress).apply {
                    setPlayedColor(Color.parseColor("#FF6200EE"))
                    setBufferedColor(Color.parseColor("#FFFF4081"))
                }
            val frameLayout =
                this.findViewById<FrameLayout>(androidx.media3.ui.R.id.exo_bottom_bar).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                }

            // 获取屏幕宽度
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            this.layoutParams = ViewGroup.LayoutParams(screenWidth, screenWidth)


        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        AndroidView(
            factory = { context ->


                // 将按钮添加到 PlayerView


                playView
            },
            update = { playerView ->
                playerView.player = player
                playerView.showController()
                playerView.controllerShowTimeoutMs = 0
                playerView.controllerHideOnTouch = false

                (playView as ViewGroup).addView(addDownloadButton(context, player))
                (playView as ViewGroup).addView(addFavoriteButton(context))
                (playView as ViewGroup).addView(addSetRingtoneButton(context, player))

            },
            modifier = Modifier.fillMaxWidth()
        )


    }

}


//fun main() {
//
//    val ringtone_url = "https://www.compocore.com/ringtones/test.mp3"
//    val fileName = ringtone_url.split("/").last()
//    println(fileName)
//}

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


@OptIn(UnstableApi::class)
@Composable
fun XMLLayoutWithPlayerView(context: Context, exoPlayer: ExoPlayer) {

    // 使用 LayoutInflater 加载 XML 布局
    val layoutInflater = LayoutInflater.from(context)
    val viewGroup = layoutInflater.inflate(com.xxh.ringbones.R.layout.activity_main, null)

    val isPlaying = remember { mutableStateOf(exoPlayer.isPlaying) }

    // 监听播放器状态变化（这里只简单监听 isPlaying 状态）
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying.value = isPlayingNow
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }


    // 使用 AndroidView 来显示加载的 XML 布局
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = {
                val playerView =
                    viewGroup.findViewById<PlayerView>(com.xxh.ringbones.R.id.player_view).apply {
                        this.player = exoPlayer
                        this.showController()
                        this.controllerShowTimeoutMs = 0
                        this.controllerHideOnTouch = false

                        this.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
                        this.defaultArtwork =
                            ContextCompat.getDrawable(
                                context,
                                com.xxh.ringbones.R.drawable.ab1_inversions
                            )

                        val defaultTimeBar =
                            this.findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
                                .apply {
                                    setPlayedColor(Color.parseColor("#FF6200EE"))
                                    setBufferedColor(Color.parseColor("#FFFF4081"))
                                }
                        val frameLayout =
                            this.findViewById<FrameLayout>(androidx.media3.ui.R.id.exo_bottom_bar)
                                .apply {
                                    setBackgroundColor(Color.TRANSPARENT)
                                }
                    }

                viewGroup
            },
            modifier = Modifier.fillMaxSize(),
            update = { viewGroup ->
                val playerView =
                    viewGroup.findViewById<PlayerView>(com.xxh.ringbones.R.id.player_view).apply {
                        this.player = exoPlayer

                        this.showController()
                        this.controllerShowTimeoutMs = 0
                        this.controllerHideOnTouch = false
                    }
            }
        )

    }

}

fun printAllChildViews(view: View, depth: Int = 0) {
    val indent = " ".repeat(depth * 2) // 控制缩进，方便查看层级关系
    Log.d("ViewTree", "${indent}${view.javaClass.simpleName} (id=${view.id})")

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            printAllChildViews(view.getChildAt(i), depth + 1)
        }
    }
}

fun printAllChildViewsAndBackground(view: View, depth: Int = 0) {
    val indent = " ".repeat(depth * 2) // 控制缩进，方便查看层级关系
    val viewName = view.javaClass.simpleName
    val viewId =
        if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else "NO_ID"

    // 获取背景颜色
    val background = view.background
    val backgroundColor = if (background is ColorDrawable) {
        String.format("#%06X", 0xFFFFFF and background.color)
    } else {
        "No Color (Drawable or Null)"
    }

    // 打印 View 信息
    Log.d("ViewTree", "$indent$viewName (id=$viewId) - Background: $backgroundColor")

    // 递归遍历 ViewGroup 内的所有子 View
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            printAllChildViewsAndBackground(view.getChildAt(i), depth + 1)
        }
    }
}