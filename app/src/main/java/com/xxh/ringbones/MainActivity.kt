package com.xxh.ringbones

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.xxh.ringbones.gson.MusixRingtonesList
import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {


    private val REQUEST_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Musix2025Theme {
                MainScreen()
            }
        }
    }

//    private fun readRingtoneDirectory() {
//        // 检查外部存储是否可用
//        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
//            // 获取外部存储的 Ringtones 目录
//            val ringtoneDirectory = File(getExternalFilesDir(Environment.DIRECTORY_RINGTONES), "")
//
//            // 判断该目录是否存在
//            if (ringtoneDirectory.exists() && ringtoneDirectory.isDirectory) {
//                // 列出该目录下的所有文件
//                val ringtoneFiles = ringtoneDirectory.listFiles { file ->
//                    // 过滤出文件类型为铃音文件（比如 .mp3, .ogg）
//                    file.isFile && (file.extension == "mp3" || file.extension == "ogg")
//                }
//
//                // 打印出所有找到的铃音文件
//                ringtoneFiles?.forEach {
//                    Log.d("musixRingtone", "Found ringtone: ${it.absolutePath}")
//                }
//            } else {
//                Log.e("musixRingtone", "Ringtones directory does not exist or is not a directory")
//            }
//        } else {
//            Log.e("musixRingtone", "External storage is not available")
//        }
//    }
//
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray,
//        deviceId: Int
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
//
//        // 检查请求代码是否匹配
//        if (requestCode == REQUEST_PERMISSION_CODE) {
//            // 如果请求被授权
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // 权限被授权，执行文件操作
//                readRingtoneDirectory()
//            } else {
//                // 权限被拒绝，提示用户
//                Toast.makeText(this, "权限被拒绝，无法读取存储", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }


    @Preview
    @Composable
    fun MainScreen() {

        Scaffold(
//            topBar = {
//                TopAppBar(title = { Text("Musix Ringtone") }, navigationIcon = {
//                    IconButton(onClick = {
//
//                    }) {
//                        Icon(
//                            Icons.Filled.Menu, contentDescription = "Localized description"
//                        )
//                    }
//                })
//            },
        ) { innerPadding ->
            MainScreenCenter(innerPadding)
        }
    }


    @Composable
    fun MainScreenCenter(innerPadding: PaddingValues) {

        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

            RingtonesList()
        }
    }

    private fun Context.findActivity(): Activity {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        throw IllegalStateException("Permissions should be called in the context of an Activity")
    }


    @Composable
    fun RingtonesList() {

        var ringtoneList by remember { mutableStateOf(listOf<Ringtone>()) }

        var loading by remember { mutableStateOf(true) }


        val itemTitle = MusixRingtonesList.ringtoneUrlMap.keys.toList()
        val itemListJsonFileName = MusixRingtonesList.ringtoneUrlMap.values.toList()

        var ringtoneUrl by remember { mutableStateOf(MusixRingtonesList.URL + itemListJsonFileName.first()) }

        LaunchedEffect(ringtoneUrl) {
            val result = withContext(Dispatchers.IO) {
                mySuspendFunction(ringtoneUrl)
            }
            val gson = Gson()
            ringtoneList = gson.fromJson(result, Array<Ringtone>::class.java).toList()
            loading = false
        }

        Musix2025Theme {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                itemsIndexed(itemTitle) { index, it ->
                    Button(modifier = Modifier.padding(horizontal = 5.dp), onClick = {

                        val ringtonesUrl = StringBuilder().append(MusixRingtonesList.URL)
                            .append(itemListJsonFileName[index])

                        ringtoneUrl = ringtonesUrl.toString()
                        loading = true

                    }) {
                        Text(text = it)
                    }
                }
            }
        }

        if (loading) {
            IndeterminateCircularIndicator()
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            LazyColumn {
                items(ringtoneList) { ringtone ->
                    RingtoneCard(ringtone = ringtone, ::navigateToPlayActivity)
                    Spacer(Modifier.size(2.dp))
                }
            }

        }
    }

    @Preview
    @Composable
    private fun ShowRingtoneCard() {

        val ringtone = Ringtone(
            title = "Kailasanadan",
            author = "Sanu",
            time = "Dec 30, 2014",
            url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/satis-song-5294.mp3",
            type = "audio/mpeg"
        )

        RingtoneCard(ringtone = ringtone, ::navigateToPlayActivity)
    }

    fun navigateToPlayActivity(ringtone: Ringtone) {
        //跳转到下一个activity
        val currentActivity = findActivity()
        val intent = Intent(currentActivity, PlayActivity::class.java).apply {
//                    putExtra("EXTRA_INFO", ringtone as Serializable)
            putExtra("EXTRA_INFO", ringtone as Parcelable)
        }
        currentActivity.startActivity(intent)
    }

    @Composable
    fun RingtoneCard(ringtone: Ringtone, navigateToPlay: (Ringtone) -> Unit) {

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(start = 2.dp, end = 2.dp),

            onClick = {
                navigateToPlay(ringtone)
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                        .padding(start = 16.dp)
                    ) {
                    Image(
                        painter = painterResource(R.drawable.exo_styled_controls_play),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)

                            .background(
                                androidx.compose.ui.graphics.Color(0xFF3700B3),
                                shape = CircleShape
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp)
                    ) {
                        Text(
                            text = ringtone.title + "   ringtone by",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = ringtone.author,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "on " + ringtone.time,
                            style = MaterialTheme.typography.labelSmall,
                        )

                    }
                }
            }

        }
    }

    private suspend fun mySuspendFunction(url: String): String {
        delay(1000)

        val musixRingtonesList = MusixRingtonesList()
        val result = musixRingtonesList.sendRequestWithOkHttp(url)

        return result
    }

    @Preview
    @Composable
    fun IndeterminateCircularIndicator() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

    }

}





