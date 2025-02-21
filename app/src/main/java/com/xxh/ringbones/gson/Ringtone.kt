package com.xxh.ringbones.gson

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class Ringtone(
    val title: String,
    val author: String,
    val time: String,
    val url: String,
    val type: String
):Serializable,Parcelable