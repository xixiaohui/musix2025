package com.xxh.ringbones.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.media3.Media3PlayerView
import com.xxh.ringbones.core.util.RingtoneHelper
import java.io.File

@Composable
fun PlayerScreen(
    ringtone: Ringtone?,
    modifier: Modifier = Modifier
) {
    if (ringtone == null) return

    var videoUrl = ringtone.url
    Log.d("com.xxh.ringtone---", videoUrl)

    // If local file exists, play locally
    val file = RingtoneHelper.getLocalFile(
        androidx.compose.ui.platform.LocalContext.current,
        videoUrl
    )
    if (file.exists()) {
        videoUrl = "file://" + file.absolutePath
        Log.e("musixDownload", "Playing local file: ${file.absolutePath}")
    } else {
        Log.e("musixDownload", "Playing network file: $videoUrl")
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Media3PlayerView(
                modifier = Modifier.fillMaxSize(),
                videoUrl = videoUrl
            )
        }
    }
}
