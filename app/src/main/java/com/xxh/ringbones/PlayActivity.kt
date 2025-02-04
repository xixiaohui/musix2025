package com.xxh.ringbones

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.xxh.ringbones.gson.MusixRingtonesList

import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.helper.SongHelper
import com.xxh.ringbones.media3.Media3PlayerView
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        //获取ringtone
        val bundle: Bundle? = intent.extras
        var ringtone: Ringtone? = null
        bundle?.let {
            bundle.apply {
//                ringtone = getSerializable("EXTRA_INFO") as Ringtone
                ringtone = getParcelable("EXTRA_INFO")!!
            }
        }

        setContent {
            ExoPlayerView()
        }
    }
}

@Composable
private fun MyMediaPlayerView(ringtone: Ringtone?) {
    Musix2025Theme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MediaPlayerScreen(
                modifier = Modifier.padding(innerPadding),
                ringtone = ringtone!!
            )
        }
    }
}

@Preview
@Composable
private fun ExoPlayerView() {
    Musix2025Theme {

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Media3PlayerView(
                modifier = Modifier.padding(innerPadding),
                videoUrl = MusixRingtonesList.audioURL
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
private fun ShowMediaPlayerScreen(
    modifier: Modifier = Modifier,
    ringtone: Ringtone = Ringtone(title = "ringtone test", des = "", url = "")
) {
    Musix2025Theme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MediaPlayerScreen(
                modifier = Modifier.padding(innerPadding),
                ringtone = ringtone
            )
        }
    }
}



//@Preview
@Composable
fun PlayingScreen(modifier: Modifier = Modifier, ringtone: Ringtone) {

    var currentProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Create a coroutine scope

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(text = ringtone.title)

        Spacer(Modifier.size(24.dp))

        OutlinedButton(
            onClick = {

//                loading = true
//                scope.launch {
//                    loadProgress { progress ->
//                        currentProgress = progress
//                    }
//                    loading = false
//                }

            },
            modifier = Modifier.size(150.dp),
            shape = CircleShape,
            border = BorderStroke(3.dp, Color.Blue),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Blue)

        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.MinWidth)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Play")
        }

        if (loading) {
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            )
        }
        Spacer(Modifier.size(240.dp))

        Row {
            Button(
                onClick = {


                }
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Download")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Like")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Set")
            }
        }
    }
}

/** Iterate the progress value */
suspend fun loadProgress(updateProgress: (Float) -> Unit) {
    for (i in 1..100) {
        updateProgress(i.toFloat() / 100)
        delay(100)
    }
}


@Composable
fun MediaPlayerScreen(modifier: Modifier = Modifier, ringtone: Ringtone) {
    var ringtoneState by remember { mutableStateOf(false) }


    if (ringtoneState) {
        val audioTestUrl = MusixRingtonesList.audioURL

//        SongHelper.playStream(ringtone.url)
        SongHelper.playStream(audioTestUrl)

    } else {
        SongHelper.pauseStream()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(Modifier.size(24.dp))

        Text(text = ringtone.title)

        Spacer(Modifier.size(24.dp))

        OutlinedButton(
            onClick = {
                ringtoneState = !ringtoneState
            },
            modifier = Modifier.size(150.dp),
            shape = CircleShape,
            border = BorderStroke(4.dp, Color.Blue),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Blue)

        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (ringtoneState) {
                        Icons.Filled.Pause

                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = "Play/Pause",
                    modifier = Modifier
                        .size(ButtonDefaults.MinWidth)

                )

                Text(
                    text = if (ringtoneState) {
                        "Pause"
                    } else {
                        "Play"
                    }
                )
            }

        }

//        Spacer(Modifier.size(240.dp))
        LinearProgressIndicator(
            progress = {
                1.0f
            },
            trackColor = Color.Red,
            modifier = Modifier.padding(16.dp, 72.dp),
            color = Color.Blue,

            )

        Row(modifier = Modifier.padding(top = 10.dp)) {
            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Download")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Like")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Set")
            }
        }
    }

    DisposableEffect(ringtone.url) {
        onDispose {
            ringtoneState = false
            SongHelper.releasePlayer()
        }
    }


}


