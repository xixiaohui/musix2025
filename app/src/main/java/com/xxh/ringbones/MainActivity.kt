package com.xxh.ringbones

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.NameValueTable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xxh.ringbones.gson.MusixRingtonesList
import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.state.WellnessTaskItem
import com.xxh.ringbones.ui.theme.AppTheme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log

class MainActivity : ComponentActivity() {

    val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
//                MainScreen()

                NavigateButton(this)
            }

        }
    }

    @Composable
    fun MainScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                val intent = Intent(this@MainActivity, LandActivity::class.java)
                startActivity(intent)
            }) {
                Text("Navigate")
            }
        }
    }

    //    @Preview
    @Composable
    fun NavigateScreen() {
        NavigateButton(this)
    }
}


@Composable
fun NavigateButton(activity: MainActivity) {

    var ringtoneList by remember { mutableStateOf(listOf<Ringtone>()) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            mySuspendFunction()
        }

        val gson = Gson()

        ringtoneList = gson.fromJson(result, Array<Ringtone>::class.java).toList()

        val length = ringtoneList.size

        Log.i("MainActivity", "size = $length")
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {

        Button(
            onClick = {

            }
        ) {
            Text("点击开始")
        }
        LazyColumn {
            items(ringtoneList) { ringtone ->
                RingtoneCard(ringtone = ringtone)
            }
        }
    }
}

@Preview
@Composable
private fun ShowRingtoneCard() {

    val ringtone = Ringtone(
        title = "New Ringtone Mp3 2020",
        des = "2020 ringtone",
        url = "https://2020/new-ringtone-mp3-2020.mp3"
    )

    RingtoneCard(ringtone = ringtone)
}

@Composable
fun RingtoneCard(ringtone: Ringtone) {
    val paddingModifier = Modifier.fillMaxWidth()
    OutlinedCard(
        modifier = paddingModifier,
        onClick = {

        }

    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = paddingModifier.padding(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ab1_inversions),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(140.dp)
                )

                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = ringtone.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = ringtone.des,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Icon(
                        modifier = Modifier.padding( start = 200.dp,top = 20.dp),
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "play"
                    )
                }

            }
        }
    }
}

suspend fun mySuspendFunction(): String {
    delay(1000)

    val musixRingtonesList = MusixRingtonesList()
    val result = musixRingtonesList.sendRequestWithOkHttp()

    return result
}



