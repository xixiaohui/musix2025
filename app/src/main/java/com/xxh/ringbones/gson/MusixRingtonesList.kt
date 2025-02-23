package com.xxh.ringbones.gson

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException



class MusixRingtonesList {


    companion object {
        const val ringtoneURL = "https://www.compocore.com/ringtones/rings/malayalam.json"

//        const val URL = "https://www.compocore.com/ringtones/rings/"
        const val URL = "https://compocore.com/wp-content/uploads/2025/02/"

        val ringtoneUrlMap = mapOf(
            "Hindi-bollywood" to "hindi-bollywood-ringtones.json",
            "Tamil" to "tamil.json",
            "Sms" to "sms.json",
            "Music" to "music.json",
            "Malayalam" to "malayalam.json",
            "Funny" to "funny.json",
            "Sound" to "sound_effects.json",
            "Miscellaneous" to "miscellaneous_ringtones.json",
            "Devotional" to "devotional_ringtones.json",
            "Baby" to "baby_ringtones.json",
            "Iphone" to "iphone_ringtones.json"
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