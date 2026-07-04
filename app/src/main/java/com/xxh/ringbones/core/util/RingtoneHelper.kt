package com.xxh.ringbones.core.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object RingtoneHelper {

    private const val TAG = "musixDownload"

    fun downloadMusic(activity: Activity, url: String, onComplete: (() -> Unit)? = null) {
        val fileName = url.split("/").last()
        val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)

        if (file.exists()) {
            Log.e(TAG, "File already exists: ${file.absolutePath}")
            onComplete?.invoke()
            return
        }

        val request = Request.Builder()
            .url(url)
            .build()

        com.xxh.ringbones.core.network.HttpClient.instance.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Download failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    val outputFile = File(
                        activity.getExternalFilesDir(Environment.DIRECTORY_RINGTONES),
                        fileName
                    )
                    val outputStream = FileOutputStream(outputFile)

                    try {
                        inputStream?.copyTo(outputStream)
                        activity.runOnUiThread {
                            Log.d(TAG, "Music downloaded: ${outputFile.absolutePath}")
                            onComplete?.invoke()
                        }
                    } catch (e: IOException) {
                        activity.runOnUiThread {
                            Log.e(TAG, "Error saving: ${e.message}")
                        }
                    } finally {
                        outputStream.close()
                        inputStream?.close()
                    }
                } else {
                    activity.runOnUiThread {
                        Log.e(TAG, "Download failed: ${response.code}")
                    }
                }
            }
        })
    }

    fun isFileDownloaded(context: Context, url: String): Boolean {
        val fileName = url.split("/").last()
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)
        return file.exists()
    }

    fun getLocalFile(context: Context, url: String): File {
        val fileName = url.split("/").last()
        return File(context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)
    }

    /**
     * Sets the given audio file as the device ringtone.
     *
     * Requires [android.Manifest.permission.WRITE_SETTINGS] on Android 6+.
     * If the permission is not granted, throws [SecurityException] so the
     * caller can guide the user to enable it via [openWriteSettingsIntent].
     */
    fun setRingtone(context: Context, sourceFilePath: String) {
        val sourceFile = File(sourceFilePath)
        if (!sourceFile.exists()) return

        // Check WRITE_SETTINGS permission (required from API 23+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            throw SecurityException("WRITE_SETTINGS permission not granted")
        }

        try {
            val ringtoneUri = if (sourceFilePath.startsWith("/")) {
                insertToMediaStore(context, sourceFile)
            } else {
                Uri.parse(sourceFilePath)
            }

            ringtoneUri?.let { uri ->
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    uri
                )
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error setting ringtone: ${e.message}")
        }
    }

    /**
     * Returns an [Intent] that opens the system settings page where the user
     * can grant the WRITE_SETTINGS permission to this app.
     */
    fun openWriteSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }

    private fun insertToMediaStore(context: Context, sourceFile: File): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
            put(MediaStore.Audio.Media.IS_ALARM, false)
            put(MediaStore.Audio.Media.IS_MUSIC, false)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        return uri
    }
}
