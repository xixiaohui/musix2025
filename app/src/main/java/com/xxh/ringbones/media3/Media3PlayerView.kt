package com.xxh.ringbones.media3

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.google.android.material.snackbar.Snackbar
import com.xxh.ringbones.PlayActivity
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
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color(0xFF6200EE))
    ) {
        Media3AndroidView(player)
    }
}

@Composable
fun getCurrentActivity(): PlayActivity? {
    // 获取当前上下文
    val context = LocalContext.current

    // 转换为 Activity 类型
    return context as? PlayActivity
}


/**
 * 获取屏幕像素px ,然后换算成dp
 */
fun getScreenWidthDp(context: Context): Float {
    val displayMetrics = context.resources.displayMetrics
    return displayMetrics.widthPixels / displayMetrics.density
}


fun addDownloadButton(context: Context, player: ExoPlayer?): ImageButton {
    // 创建 ImageButton
    val downloadButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER or Gravity.BOTTOM

            val marginBottomPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics
            ).toInt()

            val marginLeftPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20f, context.resources.displayMetrics
            ).toInt()

            setMargins(marginLeftPx, 0, 0, marginBottomPx)
        }
        setImageResource(com.xxh.ringbones.R.drawable.download_24px)
        setBackgroundColor(Color.TRANSPARENT)
//        setBackgroundColor(android.graphics.Color.GREEN)

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

                if (audioUrl.startsWith("file")) {
                    Snackbar.make(
                        downloadButton,
                        context.getString(com.xxh.ringbones.R.string.downloadTips3),
                        Snackbar.LENGTH_SHORT
                    ).show()

                } else {
                    //从网络下载铃音
                    val activity = context as PlayActivity
                    PlayActivity.Utility.downloadMusic(activity, audioUrl)

                    Snackbar.make(
                        downloadButton,
                        context.getString(com.xxh.ringbones.R.string.downloadTips2),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

            }
            .setNegativeButton(context.getString(com.xxh.ringbones.R.string.cancel), null)
            .show()
    }
    return downloadButton
}

fun addSetRingtoneButton(context: Context, player: ExoPlayer?): ImageButton {
    // 创建 ImageButton
    val setRingtoneButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER or Gravity.BOTTOM

            val marginBottomPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics
            ).toInt()

            val marginLeftPx = getScreenWidthDp(context) / 2

            setMargins(marginLeftPx.toInt(), 0, 0, marginBottomPx)
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

                if (audioUrl.startsWith("file")) {
                    val activity = context as PlayActivity
                    PlayActivity.Utility.setRingtone(activity, audioUrl)

                    Snackbar.make(
                        setRingtoneButton,
                        context.getString(com.xxh.ringbones.R.string.set_ringtone_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        setRingtoneButton,
                        context.getString(com.xxh.ringbones.R.string.set_ringtone3) + audioUrl,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(context.getString(com.xxh.ringbones.R.string.cancel), null)
            .show()
    }

    return setRingtoneButton
}

fun addFavoriteButton(context: Context): ImageButton {
    // 创建 ImageButton
    val favoriteButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(100, 100).apply {
            gravity = Gravity.BOTTOM or Gravity.END  // 右下角
            setMargins(0, 0, 250, 10) // 距离底部100dp
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

            setBackgroundColor(Color.TRANSPARENT)


            this.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
            this.defaultArtwork =
                ContextCompat.getDrawable(context, com.xxh.ringbones.R.drawable.erik)

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
//            val displayMetrics = resources.displayMetrics
//            val screenWidth = displayMetrics.widthPixels
//            this.layoutParams = ViewGroup.LayoutParams(screenWidth, screenWidth)

        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        AndroidView(
            factory = { context ->

                playView
            },
            update = { playerView ->
                playerView.player = player
                playerView.showController()
                playerView.controllerShowTimeoutMs = 0
                playerView.controllerHideOnTouch = false

                (playView as ViewGroup).addView(addDownloadButton(context, player))
                (playView as ViewGroup).addView(addSetRingtoneButton(context, player))
//                (playView as ViewGroup).addView(addFavoriteButton(context))

            },
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