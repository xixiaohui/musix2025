package com.xxh.ringbones.gson

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException



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

        const val audioURL = "https://www.compocore.com/ringtones/test.mp3"
        const val local_audio_url = "file:///storage/emulated/0/Android/data/com.xxh.ringbones/files/Ringtones/test.mp3"
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

    fun sendRequestWithOkHttp(url: String): String {
        return read(url)
    }

}