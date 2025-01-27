package com.xxh.ringbones

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ToggleButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        //获取ringtone
        val bundle:Bundle? = intent.extras

        var ringtone: Ringtone? = null
        bundle?.let {
            bundle.apply {
//                ringtone = getSerializable("EXTRA_INFO") as Ringtone
                ringtone = getParcelable("EXTRA_INFO")!!
            }
        }

        setContent {
            Musix2025Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlayingScreen(
                        modifier = Modifier.padding(innerPadding),
                        ringtone = ringtone!!
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShowPlayingScreen(
    modifier: Modifier = Modifier,
    ringtone: Ringtone = Ringtone(title = "ringtone title", des = "", url = "")
) {

    Musix2025Theme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            PlayingScreen(
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
                loading = true
                scope.launch {
                    loadProgress { progress ->
                        currentProgress = progress
                    }
                    loading = false // Reset loading when the coroutine finishes
                }
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
                modifier = Modifier.size(ButtonDefaults.IconSize)
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
                    Icons.Filled.Build,
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
                    Icons.Filled.Build,
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


