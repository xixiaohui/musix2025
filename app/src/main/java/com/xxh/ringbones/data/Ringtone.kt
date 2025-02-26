package com.xxh.ringbones.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ringtones")
@Parcelize
data class Ringtone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val time: String,
    val url: String,
    val type: String
):Serializable,Parcelable