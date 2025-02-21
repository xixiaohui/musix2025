package com.xxh.ringbones.gson

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException



class MusixRingtonesList {


    companion object {
        const val ringtoneURL = "https://www.compocore.com/ringtones/rings/malayalam.json"

        const val URL = "https://www.compocore.com/ringtones/rings/"

        val ringtoneUrlMap = mapOf(
            "hindi-bollywood" to "hindi-bollywood-ringtones.json",
            "tamil" to "tamil.json",
            "sms" to "sms.json",
            "music" to "music.json",
            "malayalam" to "malayalam.json",
            "funny" to "funny.json",
            "sound" to "sound_effects.json",
            "miscellaneous" to "miscellaneous_ringtones.json",
            "devotional" to "devotional_ringtones.json",
            "baby" to "baby_ringtones.json",
            "iphone" to "iphone_ringtones.json"
        )

//        const val audioURL = "https://www.compocore.com/ringtones/test.mp3"

        const val audioURL = "https://dl.prokerala.com/downloads/ringtones/files/ogg/alanwalkeravamaxaloneptiilyricsringtoneringtonee-49076.ogg"

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