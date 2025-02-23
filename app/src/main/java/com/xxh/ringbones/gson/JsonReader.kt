package com.xxh.ringbones.gson

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.Charset

object JsonReader {

    // 读取并解析 assets 中的 JSON 文件
    /**
     * {
     *   "name": "Android",
     *   "version": "1.0",
     *   "description": "A simple Android app."
     * }
     */
    fun readJsonFromAssets(context: Context) {
        val assetManager: AssetManager = context.assets
        var inputStream: InputStream? = null

        try {
            // 打开 assets 中的 JSON 文件
            inputStream = assetManager.open("data.json")

            // 将 InputStream 转换为字符串
            val json = convertStreamToString(inputStream)

            // 解析 JSON 数据
            val jsonObject = JSONObject(json)

            val result = jsonObject.optString("name")


            val name = jsonObject.optString("name")
            val version = jsonObject.optString("version")
            val description = jsonObject.optString("description")

            Log.d("JSON Data", "Name: $name")
            Log.d("JSON Data", "Version: $version")
            Log.d("JSON Data", "Description: $description")

        } catch (e: Exception) {
            Log.e("JsonReader", "Error reading JSON from assets", e)
        } finally {
            // 关闭 InputStream
            inputStream?.close()
        }
    }

    // 读取并解析 assets 中的 JSON 文件为对象列表
    fun readJsonFromAssetsToList(context: Context): List<Ringtone> {
        val assetManager: AssetManager = context.assets
        var inputStream: InputStream? = null

        return try {
            // 打开 assets 中的 JSON 文件
            inputStream = assetManager.open("music.json")

            // 将 InputStream 转换为字符串
            val json = convertStreamToString(inputStream)

            // 使用 Gson 解析 JSON 字符串为对象列表
            val gson = Gson()
            val listType = object : TypeToken<List<Ringtone>>() {}.type
            gson.fromJson<List<Ringtone>>(json, listType)

        } catch (e: Exception) {
            Log.e("JsonReader", "Error reading JSON from assets", e)
            emptyList<Ringtone>()  // 如果出错返回空列表
        } finally {
            // 关闭 InputStream
            inputStream?.close()
        }
    }

    // 将 InputStream 转换为字符串
    private fun convertStreamToString(inputStream: InputStream): String {
        return inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
    }
}