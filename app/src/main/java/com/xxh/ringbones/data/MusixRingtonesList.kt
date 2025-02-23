package com.xxh.ringbones.data

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

        val firstPageRingtones = listOf(
            Ringtone(
                title = "Hollywood Song",
                author = "Sai",
                time = "Jan 8",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/instagram-1636600194526-320kbps-65491.mp3",
                type = "audio/mpeg"
            ),
            Ringtone(
                title = "Tamil Love Song",
                author = "Mulleswari",
                time = "Jan 10",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/roja-flute-instrumental-65045-1-65521.mp3",
                type = "audio/mpeg"
            ),
            Ringtone(
                title = "Ringtone Bgm",
                author = "Shibom",
                time = "Jan 4",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/taqdeer-violin-bgm-ringtone-taqdeer-instrumental-ringtone-256k-65433.mp3",
                type = "audio/mpeg"
            ),
            Ringtone(
                title = "Hindi Song",
                author = "ring",
                time = "Jan 10",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/bulbuli-bengali-song-65520.mp3",
                type = "audio/mpeg"
            ),
            Ringtone(
                title = "Music",
                author = "Durga prasad",
                time = "Jan 2",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/rishika-65412.mp3",
                type = "audio/mpeg"
            ),
            Ringtone(
                title = "Music",
                author = "Havoq",
                time = "Jan 1",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/preview-1249-65392.mp3",
                type = "audio/mpeg"
            ),
            Ringtone(
                title = "Music",
                author = "Ayush Kumar",
                time = "Jan 8",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/d0185a1f45b0ec792c6cb7afff2c581b-1-65486.mp3",
                type = "audio/mpeg"
            ),
            Ringtone(
                title = "Music",
                author = "Saurabh",
                time = "Jan 2",
                url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/unknown-artist-police-remix-14463-1-65411.mp3",
                type = "audio/mpeg"
            ),
        )

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