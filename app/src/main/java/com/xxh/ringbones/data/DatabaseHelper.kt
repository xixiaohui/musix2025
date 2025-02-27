package com.xxh.ringbones.data

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DatabaseHelper(private val context: Context) {

    private val dbName = "ringtones.db"
    private val dbPath = context.getDatabasePath(dbName).absolutePath


    // 复制数据库文件
    @Throws(IOException::class)
    fun copyDatabase() {
        val dbFile = File(dbPath)
        if (!dbFile.exists()) {
            val assetManager: AssetManager = context.assets
            val inputStream: InputStream = assetManager.open("databases/$dbName")
            val outputStream: OutputStream = FileOutputStream(dbFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }
    }

    // 打开数据库
    fun openDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
    }

}