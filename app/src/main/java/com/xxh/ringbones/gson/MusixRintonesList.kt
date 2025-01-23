package com.xxh.ringbones.gson

import android.icu.text.CaseMap.Title
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


class MusixRintonesList {

    data class Ringtone(val title: String,val des:String,val url:String)

    private val ringtoneURL = "https://www.compocore.com/ringtones/rings/2020.json"

    private var client: OkHttpClient = OkHttpClient()

    @Throws(IOException::class)
    fun read(url: String): String {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            return response.body!!.string()
        }
    }

    fun read(): String {
        return read(ringtoneURL)
    }




}