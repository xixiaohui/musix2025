package com.xxh.ringbones

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.xxh.ringbones.MyApplication.Companion.context
import com.xxh.ringbones.data.AppDatabase
import com.xxh.ringbones.data.DatabaseHelper
import com.xxh.ringbones.data.MusixRingtonesList
import com.xxh.ringbones.data.Ringtone
import com.xxh.ringbones.data.RingtoneDao
import com.xxh.ringbones.data.RingtoneViewModel
import com.xxh.ringbones.data.RingtoneViewModelFactory
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

//        val dbPath = context.getDatabasePath("ringtones.db").absolutePath
//        Log.v("musixDatabase",dbPath.toString())


        val databaseHelper = DatabaseHelper(applicationContext)
        try {
            databaseHelper.copyDatabase()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        //获取数据库和DAO
        val database = AppDatabase.getInstance(this)
        val dao = database.ringtoneDao()


        setContent {
            //已发布版本的
//            Musix2025Theme {
//                MainScreen()
//            }

            //待测试UI 界面
//            val size = currentWindowAdaptiveInfo().windowSizeClass
//            Musix2025App(size)

            //测试数据库数据
            DatabaseScreen(dao)
        }
    }


    @Composable
    private fun DatabaseScreen(dao: RingtoneDao) {
        val viewModel: RingtoneViewModel = viewModel(factory = RingtoneViewModelFactory(dao))
        val ringtones by viewModel.ringtones.collectAsState()


        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                RingtonesList(false, ringtones)
            }
        }


    }


    @Preview
    @Composable
    fun MainScreen() {

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                MainScreenCenter(innerPadding)
            }
        }
    }


    @SuppressLint("MutableCollectionMutableState")
    @Composable
    fun MainScreenCenter(innerPadding: PaddingValues) {

        var loading by remember { mutableStateOf(false) }

        val itemTitle = MusixRingtonesList.ringtoneUrlMap.keys.toList()
        val itemListJsonFileName = MusixRingtonesList.ringtoneUrlMap.values.toList()

        var ringtoneUrl by remember { mutableStateOf(MusixRingtonesList.URL + itemListJsonFileName.first()) }


//        val context = LocalContext.current
        var ringtoneList by remember {
            mutableStateOf(MusixRingtonesList.firstPageRingtones)
        }

        //标记横向导航按钮是否被按下
        val isPressedList = remember { mutableStateOf(MutableList(itemTitle.size) { false }) }

        // 记住一个状态来判断是否是第一次
        val isFirstRun = remember { mutableStateOf(true) }

        //标记当前按下的按钮索引
        val isButtonIndexpressed = remember { mutableIntStateOf(-1) }

        LaunchedEffect(loading) {
            if (!isFirstRun.value) {
                val result = withContext(Dispatchers.IO) {
                    mySuspendFunction(ringtoneUrl)
                }
                val gson = Gson()
                ringtoneList = gson.fromJson(result, Array<Ringtone>::class.java).toList()
                loading = false
            }

            isFirstRun.value = false
        }



        Column(
            modifier = Modifier.padding(innerPadding)
        ) {

            Musix2025Theme {

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    itemsIndexed(itemTitle) { index, it ->

                        ButtonWithPressedState(
                            isPressed = isPressedList.value[index],
                            onClickCallback = { state ->

                                for ((i, item) in isPressedList.value.withIndex()) {
                                    isPressedList.value[i] = false
                                }
                                isPressedList.value[index] = true

                                if (isButtonIndexpressed.intValue != index) {
                                    val ringtonesUrl =
                                        StringBuilder().append(MusixRingtonesList.URL)
                                            .append(itemListJsonFileName[index])

                                    ringtoneUrl = ringtonesUrl.toString()
                                    loading = true
                                    isButtonIndexpressed.intValue = index
                                }

                            }, stringResource(it)
                        )
                    }
                }
            }


            RingtonesList(loading, ringtoneList)
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
    fun ButtonWithPressedState(isPressed: Boolean, onClickCallback: (Boolean) -> Unit, it: String) {

        Button(
            modifier = Modifier.padding(horizontal = 5.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = if (isPressed) Color(0xFFFF4081) else Color.White
            ),
            onClick = {
                onClickCallback(isPressed)
            }

        ) {

            Text(text = it)
        }

//        Log.d("musixButton", "Button is pressed: $isPressed")
    }

    @Composable
    fun RingtonesList(loading: Boolean, ringtoneList: List<Ringtone>) {


        if (loading) {
            IndeterminateCircularIndicator()
        }

        Musix2025Theme {
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
                    modifier = Modifier
                        .fillMaxSize()
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

        return result!!
    }

    @Preview
    @Composable
    fun IndeterminateCircularIndicator() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(32.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

    }

}





