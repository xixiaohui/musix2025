package com.xxh.ringbones

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.xxh.ringbones.gson.MusixRingtonesList
import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Musix2025Theme {
                MainScreen()
            }
        }
    }


    @Preview
    @Composable
    fun MainScreen() {

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                MainScreenCenter(innerPadding)
            }
        }
    }


    @Composable
    fun MainScreenCenter(innerPadding: PaddingValues) {

        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

            RingtonesList()
        }
    }

    private fun Context.findActivity(): Activity {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        throw IllegalStateException("Permissions should be called in the context of an Activity")
    }


    @Composable
    fun RingtonesList() {

        var ringtoneList by remember { mutableStateOf(listOf<Ringtone>()) }

        var loading by remember { mutableStateOf(true) }


        val itemTitle = MusixRingtonesList.ringtoneUrlMap.keys.toList()
        val itemListJsonFileName = MusixRingtonesList.ringtoneUrlMap.values.toList()

        var ringtoneUrl by remember { mutableStateOf(MusixRingtonesList.URL + itemListJsonFileName.first()) }

        LaunchedEffect(ringtoneUrl) {
            val result = withContext(Dispatchers.IO) {
                mySuspendFunction(ringtoneUrl)
            }
            val gson = Gson()
            ringtoneList = gson.fromJson(result, Array<Ringtone>::class.java).toList()
            loading = false
        }

        Musix2025Theme {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                itemsIndexed(itemTitle) { index, it ->
                    Button(modifier = Modifier.padding(horizontal = 5.dp), onClick = {

                        val ringtonesUrl = StringBuilder().append(MusixRingtonesList.URL)
                            .append(itemListJsonFileName[index])

                        ringtoneUrl = ringtonesUrl.toString()
                        loading = true

                    }) {
                        Text(text = it)
                    }
                }
            }
        }

        if (loading) {
            IndeterminateCircularIndicator()
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            LazyColumn {
                items(ringtoneList) { ringtone ->
                    RingtoneCard(ringtone = ringtone, ::navigateToPlayActivity)
                    Spacer(Modifier.size(2.dp))
                }
            }

        }
    }

    @Preview
    @Composable
    private fun ShowRingtoneCard() {

        val ringtone = Ringtone(
            title = "Kailasanadan",
            author = "Sanu",
            time = "Dec 30, 2014",
            url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/satis-song-5294.mp3",
            type = "audio/mpeg"
        )

        RingtoneCard(ringtone = ringtone, ::navigateToPlayActivity)
    }

    fun navigateToPlayActivity(ringtone: Ringtone) {
        //跳转到下一个activity
        val currentActivity = findActivity()
        val intent = Intent(currentActivity, PlayActivity::class.java).apply {
//                    putExtra("EXTRA_INFO", ringtone as Serializable)
            putExtra("EXTRA_INFO", ringtone as Parcelable)
        }
        currentActivity.startActivity(intent)
    }

    @Composable
    fun RingtoneCard(ringtone: Ringtone, navigateToPlay: (Ringtone) -> Unit) {

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(start = 2.dp, end = 2.dp),

            onClick = {
                navigateToPlay(ringtone)
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.exo_styled_controls_play),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)

                            .background(
                                androidx.compose.ui.graphics.Color(0xFF3700B3),
                                shape = CircleShape
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp)
                    ) {
                        Text(
                            text = ringtone.title + "   ringtone by",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = ringtone.author,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "on " + ringtone.time,
                            style = MaterialTheme.typography.labelSmall,
                        )

                    }
                }
            }

        }
    }

    private suspend fun mySuspendFunction(url: String): String {
        delay(1000)

        val musixRingtonesList = MusixRingtonesList()
        val result = musixRingtonesList.sendRequestWithOkHttp(url)

        return result
    }

    @Preview
    @Composable
    fun IndeterminateCircularIndicator() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

    }

}





