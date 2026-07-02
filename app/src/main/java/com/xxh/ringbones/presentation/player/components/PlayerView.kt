package com.xxh.ringbones.presentation.player.components

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView as Media3PlayerView
import com.google.android.material.snackbar.Snackbar
import com.xxh.ringbones.R
import com.xxh.ringbones.core.util.RingtoneHelper
import com.xxh.ringbones.core.util.findActivity

@OptIn(UnstableApi::class)
@Composable
fun PlayerView(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val playView = remember {
        Media3PlayerView(context).apply {
            this.player = exoPlayer
            this.showController()
            this.controllerShowTimeoutMs = 0
            this.controllerHideOnTouch = false

            setBackgroundColor(Color.TRANSPARENT)

            this.artworkDisplayMode = Media3PlayerView.ARTWORK_DISPLAY_MODE_FILL
            this.defaultArtwork =
                ContextCompat.getDrawable(context, R.drawable.erik)

            val defaultTimeBar =
                this.findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress).apply {
                    setPlayedColor(Color.parseColor("#FF6200EE"))
                    setBufferedColor(Color.parseColor("#FFFF4081"))
                }
            val frameLayout =
                this.findViewById<FrameLayout>(androidx.media3.ui.R.id.exo_bottom_bar).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        AndroidView(
            factory = { context ->
                playView
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.showController()
                playerView.controllerShowTimeoutMs = 0
                playerView.controllerHideOnTouch = false

                (playView as ViewGroup).addView(addDownloadButton(context, exoPlayer))
                (playView as ViewGroup).addView(addSetRingtoneButton(context, exoPlayer))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun addDownloadButton(context: Context, player: ExoPlayer?): ImageButton {
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
        setImageResource(R.drawable.download_24px)
        setBackgroundColor(Color.TRANSPARENT)

        setColorFilter(
            ContextCompat.getColor(context, R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }

    downloadButton.setOnClickListener {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.tips))
            .setMessage(context.getString(R.string.downloadTips))
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                val currentMediaItem = player?.currentMediaItem
                val audioUrl = currentMediaItem?.localConfiguration?.uri.toString()

                if (audioUrl.startsWith("file")) {
                    Snackbar.make(
                        downloadButton,
                        context.getString(R.string.downloadTips3),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    val activity = context.findActivity()
                    if (activity != null) {
                        RingtoneHelper.downloadMusic(activity, audioUrl)
                        Snackbar.make(
                            downloadButton,
                            context.getString(R.string.downloadTips2),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }
    return downloadButton
}

private fun addSetRingtoneButton(context: Context, player: ExoPlayer?): ImageButton {
    val setRingtoneButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER or Gravity.BOTTOM

            val marginBottomPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics
            ).toInt()

            val displayMetrics = context.resources.displayMetrics
            val marginLeftPx = (displayMetrics.widthPixels / displayMetrics.density / 2)

            setMargins(marginLeftPx.toInt(), 0, 0, marginBottomPx)
        }
        setImageResource(R.drawable.notification_add_24px)
        setBackgroundColor(Color.TRANSPARENT)
        setColorFilter(
            ContextCompat.getColor(context, R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }

    setRingtoneButton.setOnClickListener {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.tips))
            .setMessage(context.getString(R.string.set_ringtone))
            .setPositiveButton(context.getString(R.string.confirm)) { _, _ ->
                val currentMediaItem = player?.currentMediaItem
                val audioUrl = currentMediaItem?.localConfiguration?.uri.toString()

                if (audioUrl.startsWith("file")) {
                    val path = audioUrl.removePrefix("file://")
                    RingtoneHelper.setRingtone(context, path)

                    Snackbar.make(
                        setRingtoneButton,
                        context.getString(R.string.set_ringtone_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        setRingtoneButton,
                        context.getString(R.string.set_ringtone3) + audioUrl,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    return setRingtoneButton
}
