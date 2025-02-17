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
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.xxh.ringbones.gson.MusixRingtonesList
import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.helper.SongHelper
import com.xxh.ringbones.media3.Media3PlayerView
import com.xxh.ringbones.media3.getCurrentActivity
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlinx.coroutines.delay
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

        // 请求存储权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        }

        // 请求读取权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }

        //获取ringtone
        val bundle: Bundle? = intent.extras
        var ringtone: Ringtone? = null
        bundle?.let {
            bundle.apply {
//                ringtone = getSerializable("EXTRA_INFO") as Ringtone
                ringtone = getParcelable("EXTRA_INFO")!!
            }
        }

        setContent {
            ExoPlayerView()
        }

//        val url = MusixRingtonesList.audioURL
//        downloadMusic(url)
    }



    object Utility{
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
                Log.e("musixDownload","文件已经存在：${file.absolutePath}")
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
                        val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)
                        val outputStream = file.outputStream()

                        try {
                            inputStream?.copyTo(outputStream)
                            activity.runOnUiThread {
                                Log.d("musixDownload", "Music downloaded successfully: ${file.absolutePath}")
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
                            Log.e("musixDownload", "Download failed with status code: ${response.code}")
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
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

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
                    context, RingtoneManager.TYPE_RINGTONE, ringtoneUri)
            } else {
                // For lower versions, you can use the legacy way to set the ringtone
                val ringtoneUri = Uri.fromFile(ringtoneFile)
                RingtoneManager.setActualDefaultRingtoneUri(
                    context, RingtoneManager.TYPE_RINGTONE, ringtoneUri)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

    }
}

@Composable
private fun MyMediaPlayerView(ringtone: Ringtone?) {
    Musix2025Theme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MediaPlayerScreen(
                modifier = Modifier.padding(innerPadding),
                ringtone = ringtone!!
            )
        }
    }
}

@Preview
@Composable
private fun ExoPlayerView() {
    var videoUrl = MusixRingtonesList.audioURL
    val fileName = videoUrl.split("/").last()
    val file = File(LocalActivity.current?.getExternalFilesDir(Environment.DIRECTORY_RINGTONES), fileName)
    if(file.exists()){
        videoUrl = file.absolutePath

        Log.e("musixDownload","播放本地存在的文件：${file.absolutePath}")
    }else{
        Log.e("musixDownload","播放网络文件：$videoUrl")
    }

    Musix2025Theme {

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Media3PlayerView(
                modifier = Modifier.padding(innerPadding),
                videoUrl = videoUrl
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
private fun ShowMediaPlayerScreen(
    modifier: Modifier = Modifier,
    ringtone: Ringtone = Ringtone(title = "ringtone test", des = "", url = "")
) {
    Musix2025Theme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MediaPlayerScreen(
                modifier = Modifier.padding(innerPadding),
                ringtone = ringtone
            )
        }
    }
}



//@Preview
@Composable
fun PlayingScreen(modifier: Modifier = Modifier, ringtone: Ringtone) {

    var currentProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Create a coroutine scope

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(text = ringtone.title)

        Spacer(Modifier.size(24.dp))

        OutlinedButton(
            onClick = {

//                loading = true
//                scope.launch {
//                    loadProgress { progress ->
//                        currentProgress = progress
//                    }
//                    loading = false
//                }

            },
            modifier = Modifier.size(150.dp),
            shape = CircleShape,
            border = BorderStroke(3.dp, Color.Blue),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Blue)

        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.MinWidth)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Play")
        }

        if (loading) {
            LinearProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            )
        }
        Spacer(Modifier.size(240.dp))

        Row {
            Button(
                onClick = {


                }
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Download")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Like")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Set")
            }
        }
    }
}

/** Iterate the progress value */
suspend fun loadProgress(updateProgress: (Float) -> Unit) {
    for (i in 1..100) {
        updateProgress(i.toFloat() / 100)
        delay(100)
    }
}


@Composable
fun MediaPlayerScreen(modifier: Modifier = Modifier, ringtone: Ringtone) {
    var ringtoneState by remember { mutableStateOf(false) }


    if (ringtoneState) {
        val audioTestUrl = MusixRingtonesList.audioURL

//        SongHelper.playStream(ringtone.url)
        SongHelper.playStream(audioTestUrl)

    } else {
        SongHelper.pauseStream()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(Modifier.size(24.dp))

        Text(text = ringtone.title)

        Spacer(Modifier.size(24.dp))

        OutlinedButton(
            onClick = {
                ringtoneState = !ringtoneState
            },
            modifier = Modifier.size(150.dp),
            shape = CircleShape,
            border = BorderStroke(4.dp, Color.Blue),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Blue)

        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (ringtoneState) {
                        Icons.Filled.Pause

                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = "Play/Pause",
                    modifier = Modifier
                        .size(ButtonDefaults.MinWidth)

                )

                Text(
                    text = if (ringtoneState) {
                        "Pause"
                    } else {
                        "Play"
                    }
                )
            }

        }

//        Spacer(Modifier.size(240.dp))
        LinearProgressIndicator(
            progress = {
                1.0f
            },
            trackColor = Color.Red,
            modifier = Modifier.padding(16.dp, 72.dp),
            color = Color.Blue,

            )

        Row(modifier = Modifier.padding(top = 10.dp)) {
            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Download")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Like")
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = {

                }
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Set")
            }
        }
    }

    DisposableEffect(ringtone.url) {
        onDispose {
            ringtoneState = false
            SongHelper.releasePlayer()
        }
    }


}


