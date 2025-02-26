package com.xxh.ringbones.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Ringtone::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ringtoneDao(): RingtoneDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ringtone_database"
                )
                    .fallbackToDestructiveMigration()//数据库升级时销毁旧的数据
                    .build().also { INSTANCE = it }
            }
        }


    }

}