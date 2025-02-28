package com.xxh.ringbones.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey
import javax.annotation.Nonnull

@Entity(tableName = "ringtones")
@Parcelize
data class Ringtone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @Nonnull
    val author: String,

    @Nonnull
    val title: String,

    @Nonnull
    val time: String,

    @Nonnull
    val url: String,

    @Nonnull
    val type: String

) : Serializable, Parcelable