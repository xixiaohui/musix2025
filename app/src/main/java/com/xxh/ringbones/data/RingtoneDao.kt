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


    // 获取固定数量的铃音
    @Query("SELECT * FROM ringtones LIMIT :limit")
    fun getAllRingtonesLimited(limit: Int): kotlinx.coroutines.flow.Flow<List<Ringtone>>


    @Query("SELECT * FROM ringtones")
    fun getAllRingtones(): kotlinx.coroutines.flow.Flow<List<Ringtone>>

    @Query("SELECT * FROM ringtones WHERE id = :ringtoneId")
    suspend fun  getRingtoneById(ringtoneId: Int):Ringtone?

    @Delete
    suspend fun delete(ringtone: Ringtone)

    @Query("SELECT * FROM ringtones")
    fun getAllRingtonesPaging(): PagingSource<Int, Ringtone>


    @Query("SELECT * FROM ringtones LIMIT :limit OFFSET :offset")
    fun getRingtonesPaged(limit: Int, offset: Int): PagingSource<Int, Ringtone>
}

