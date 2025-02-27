package com.xxh.ringbones.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RingtoneDao {
    @Insert
    suspend fun insert(ringtone: Ringtone)

    @Query("SELECT * FROM ringtones")
    fun getAllRingtones(): kotlinx.coroutines.flow.Flow<List<Ringtone>>

    @Query("SELECT * FROM ringtones WHERE id = :ringtoneId")
    suspend fun  getRingtoneById(ringtoneId: Int):Ringtone?

    @Delete
    suspend fun delete(ringtone: Ringtone)

    @Query("SELECT * FROM ringtones")
    fun getAllRingtonesPaging(): PagingSource<Int, Ringtone>
}

