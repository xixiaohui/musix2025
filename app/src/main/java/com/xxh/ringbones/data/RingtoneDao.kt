package com.xxh.ringbones.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RingtoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ringtone: Ringtone)

    @Query("SELECT * FROM ringtones ORDER BY id ASC")
    fun getAllRingtones(): kotlinx.coroutines.flow.Flow<List<Ringtone>>

    @Query("SELECT * FROM ringtones LIMIT :limited")
    fun searchRingtonesLimited(limited: Int): kotlinx.coroutines.flow.Flow<List<Ringtone>>

    //查询铃音 当类型完全匹配时候
    @Query("SELECT * FROM ringtones WHERE type = :typeName ORDER BY id ASC")
    fun searchRingtoneByTypeExactly(typeName:String): kotlinx.coroutines.flow.Flow<List<Ringtone>>

    //通过类似的铃音类型匹配
    @Query("SELECT * FROM ringtones WHERE type LIKE :searchType")
    fun searchRingtoneByTypeLike(searchType:String): kotlinx.coroutines.flow.Flow<List<Ringtone>>

    //通过ID查询铃音在一个给定的list里面
    @Query("SELECT * FROM ringtones WHERE id IN (:ringtonesIds)")
    fun searchRingtoneByIds(ringtonesIds:List<Int>): kotlinx.coroutines.flow.Flow<List<Ringtone>>


    @Delete
    suspend fun delete(ringtone: Ringtone)

    @Query("SELECT * FROM ringtones")
    fun getAllRingtonesPaging(): PagingSource<Int, Ringtone>
}

