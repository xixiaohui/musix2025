package com.xxh.ringbones.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ringtones")
@Parcelize
data class Ringtone(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo
    val title: String,

    @ColumnInfo
    val author: String,

    @ColumnInfo
    val time: String,

    @ColumnInfo
    val url: String,

    @ColumnInfo
    val type: String
):Serializable,Parcelable