package com.xxh.ringbones.gson

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


class MusixRingtonesList {



    private val ringtoneURL = "https://www.compocore.com/ringtones/rings/2020.json"

    private val ringtoneUrlList = listOf(
        "https://www.compocore.com/ringtones/rings/2020.json",
        "https://www.compocore.com/ringtones/rings/Airtel.json",
        "https://www.compocore.com/ringtones/rings/Alarm.json",
        "https://www.compocore.com/ringtones/rings/Animal.json",
        "https://www.compocore.com/ringtones/rings/Arabic.json",
        "https://www.compocore.com/ringtones/rings/Attitude.json",
        "https://www.compocore.com/ringtones/rings/Bengali.json"
    )

    private val ringtoneFile ="https://www.compocore.com/ringtones/test.mp3"

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





}