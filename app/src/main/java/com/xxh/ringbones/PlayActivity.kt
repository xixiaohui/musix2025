package com.xxh.ringbones

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.media3.Media3PlayerView
import com.xxh.ringbones.ui.theme.Musix2025Theme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class PlayActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        //获取ringtone
        val bundle: Bundle? = intent.extras
        var ringtone: Ringtone? = null
        bundle?.let {
            bundle.apply {
                ringtone = getParcelable("EXTRA_INFO")!!
            }
        }


        setContent {
            ExoPlayerView(ringtone)
        }

    }


    private fun readRingtoneDirectory() {
        // 检查外部存储是否可用
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            // 获取外部存储的 Ringtones 目录
            val ringtoneDirectory = File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), "")

            // 判断该目录是否存在
            if (ringtoneDirectory.exists() && ringtoneDirectory.isDirectory) {
                // 列出该目录下的所有文件
                val ringtoneFiles = ringtoneDirectory.listFiles { file ->
                    // 过滤出文件类型为铃音文件（比如 .mp3, .ogg）
                    file.isFile && (file.extension == "mp3" || file.extension == "ogg")
                }

                // 打印出所有找到的铃音文件
                ringtoneFiles?.forEach {
                    Log.d("musixRingtone", "Found ringtone: ${it.absolutePath}")
                }
            } else {
                Log.e("musixRingtone", "Ringtones directory does not exist or is not a directory")
            }
        } else {
            Log.e("musixRingtone", "External storage is not available")
        }
    }


    object Utility {
        private val client = OkHttpClient()

        fun isFileExists(filePath: String): Boolean {
            val file = File(filePath)
            return file.exists()
        }

        fun downloadMusic(activity: Activity, url: String) {

            val fileName = url.split("/").last()

            val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)
            // 检查文件是否已经存在
            if (file.exists()) {
                Log.e("musixDownload", "文件已经存在：${file.absolutePath}")
                return // 文件已存在，跳过下载
            }


            val request = Request.Builder()
                .url(url) // 音乐文件的 URL
                .build()

            // 异步请求
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 请求失败
                    Log.e("musixDownload", "Download failed: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        // 获取响应的 body 内容
                        val inputStream = response.body?.byteStream()

                        // 将文件保存到本地存储
                        val file = File(
                            activity.getExternalFilesDir(Environment.DIRECTORY_RINGTONES),
                            fileName
                        )
                        val outputStream = file.outputStream()

                        try {
                            inputStream?.copyTo(outputStream)
                            activity.runOnUiThread {
                                Log.d(
                                    "musixDownload",
                                    "Music downloaded successfully: ${file.absolutePath}"
                                )
                            }
                        } catch (e: IOException) {
                            activity.runOnUiThread {
                                Log.e("musixDownload", "Error saving music: ${e.message}")
                            }
                        } finally {
                            outputStream.close()
                            inputStream?.close()
                        }
                    } else {
                        activity.runOnUiThread {
                            Log.e(
                                "musixDownload",
                                "Download failed with status code: ${response.code}"
                            )
                        }
                    }
                }
            })
        }


        fun setRingtone(context: Context, sourceFilePath: String) {
            val sourceFile = File(sourceFilePath)

            // 确保文件存在
            if (!sourceFile.exists()) {
                return
            }

            // 将文件复制到铃声目录
            val ringtoneDir = File(context.getExternalFilesDir(null), "Ringtones")
            if (!ringtoneDir.exists()) {
                ringtoneDir.mkdirs()
            }

            val destinationFile = File(ringtoneDir, sourceFile.name)
            try {
                copyFile(sourceFile, destinationFile)

                // 将文件注册为铃声
                addRingtoneToMediaStore(context, destinationFile)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun copyFile(source: File, destination: File) {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }

        private fun addRingtoneToMediaStore(context: Context, ringtoneFile: File) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DATA, ringtoneFile.absolutePath)
                put(MediaStore.MediaColumns.TITLE, ringtoneFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp3")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }

            // 插入铃声到 MediaStore
            val uri: Uri? = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            )

            // 如果需要将其设置为默认铃声
            uri?.let {
                setDefaultRingtone(context, ringtoneFile)
            }
        }

        private fun setDefaultRingtone(context: Context, ringtoneFile: File) {
            val ringtoneUri = Uri.fromFile(ringtoneFile)
            val ringtoneManager = RingtoneManager(context)

            // 设置默认铃声
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12 and above, you need to use the system's ringtone manager.
                RingtoneManager.setActualDefaultRingtoneUri(
                    context, RingtoneManager.TYPE_RINGTONE, ringtoneUri
                )
            } else {
                // For lower versions, you can use the legacy way to set the ringtone
                val ringtoneUri = Uri.fromFile(ringtoneFile)
                RingtoneManager.setActualDefaultRingtoneUri(
                    context, RingtoneManager.TYPE_RINGTONE, ringtoneUri
                )
            }
        }
    }

    /**
     * ringtoneUrl：铃音的网络地址
     * https://www.compocore.com/ringtones/test.mp3
     */
    private fun getRingtoneFileFile(ringtoneUrl: String): File {
        val fileName = ringtoneUrl.split("/").last()
        val file = File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)
        return file
    }


    @Composable
    private fun ExoPlayerView(ringtone: Ringtone?) {

        var videoUrl = ringtone!!.url

        Log.d("com.xxh.ringtone---", videoUrl)

        //如果本地文件存在，就播放本地的音乐文件
        val file = getRingtoneFileFile(videoUrl)
        if (file.exists()) {
            videoUrl = "file://" + file.absolutePath
            Log.e("musixDownload", "播放本地存在的文件：${file.absolutePath}")
        } else {
            Log.e("musixDownload", "播放网络文件：$videoUrl")
        }

        Musix2025Theme {

            Scaffold(
                modifier = Modifier.fillMaxSize()

            )
            { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Media3PlayerView(
                        modifier = Modifier.fillMaxSize(),
                        videoUrl = videoUrl
                    )
                }

            }
        }
    }
}




