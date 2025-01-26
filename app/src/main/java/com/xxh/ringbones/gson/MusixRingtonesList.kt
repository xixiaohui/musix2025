package com.xxh.ringbones.gson

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


class MusixRingtonesList {


    companion object {
        const val ringtoneURL = "https://www.compocore.com/ringtones/rings/2020.json"

        const val URL = "https://www.compocore.com/ringtones/rings/"

        val ringtoneUrlList = listOf(
            "2020.json",
            "Airtel.json",
            "Alarm.json",
            "Animal.json",
            "Arabic.json",
            "Attitude.json",
            "Bengali.json"
        )

        const val ringtoneFile = "https://www.compocore.com/ringtones/test.mp3"
    }


    private var client: OkHttpClient = OkHttpClient()

    @Throws(IOException::class)
    fun read(url: String): String {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body!!.string()
            return text
        }
    }

    fun sendRequestWithOkHttp(): String {
        return read(ringtoneURL)
    }

    fun sendRequestWithOkHttp(url: String): String {
        return read(url)
    }

    @Composable
    fun Greeting(name: String) {
        var state by remember {
            mutableStateOf(1)
        }
        var resp by remember {
            mutableStateOf("hello $name!")
        }
        LaunchedEffect(state) {
            delay(400)
            resp = "state:${state}\n这个block执行在协程${Thread.currentThread().name}中"
        }
        Column {
            Text(text = resp)
            Button(
                onClick = { ++state },
                modifier = Modifier
                    .height(50.dp)
                    .width(100.dp)
            ) {
                Text(text = "点一点")
            }
        }
    }


}