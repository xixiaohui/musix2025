package com.xxh.ringbones

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.xxh.ringbones.gson.MusixRingtonesList
import com.xxh.ringbones.gson.Ringtone
import com.xxh.ringbones.helper.SongHelper
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.Serializable


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Musix2025Theme {
                MainScreen()
            }

        }
    }

    @Preview
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Musix Ringtone") }, navigationIcon = {
                    IconButton(onClick = {

                    }) {
                        Icon(
                            Icons.Filled.Menu, contentDescription = "Localized description"
                        )
                    }
                })
            },
        ) { innerPadding ->
            MainScreen(innerPadding)
        }
    }


    @Composable
    fun MainScreen(innerPadding: PaddingValues) {

        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
//            Spacer(modifier = Modifier.size(8.dp))
//            TopMenu()
//            Spacer(modifier = Modifier.size(8.dp))

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

        var ringtoneUrl by remember { mutableStateOf(MusixRingtonesList.ringtoneURL) }

        val itemList = MusixRingtonesList.ringtoneUrlList

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
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(itemList) { index, it ->
                    Button(modifier = Modifier.padding(horizontal = 4.dp), onClick = {

                        val ringtonesUrl = StringBuilder().append(MusixRingtonesList.URL)
                            .append(MusixRingtonesList.ringtoneUrlList[index])

                        ringtoneUrl = ringtonesUrl.toString()
                        loading = true
//                            Log.v("MainActivity", ringtoneUrl)

                    }) {
                        Text(text = it.dropLast(5))
                    }
                }
            }
        }

        if (loading) {
            IndeterminateCircularIndicator()
            return
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
        ) {

            LazyColumn {
                items(ringtoneList) { ringtone ->
                    RingtoneCard(ringtone = ringtone, ::navigateToPlayActivity)
                }
            }
        }
    }

    @Preview
    @Composable
    private fun ShowRingtoneCard() {

        val ringtone = Ringtone(
            title = "New Ringtone Mp3 2020",
            des = "2020 ringtone",
            url = "https://2020/new-ringtone-mp3-2020.mp3"
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
            modifier = Modifier.fillMaxWidth(),
            onClick = {

                navigateToPlay(ringtone)

                SongHelper.stopStream()
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ab1_inversions),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(140.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp)
                ) {
                    Text(
                        text = ringtone.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = ringtone.des,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Icon(
                        modifier = Modifier.padding(start = 200.dp, top = 20.dp),
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "play"
                    )
                }
            }
        }
    }

    suspend fun mySuspendFunction(url: String): String {
        delay(1000)

        val musixRingtonesList = MusixRingtonesList()
        val result = musixRingtonesList.sendRequestWithOkHttp(url)

        return result
    }

    /**
     * 横向的导航菜单列表
     */
//@Preview
    @Composable
    private fun TopMenu() {

        val itemList = MusixRingtonesList.ringtoneUrlList

        Musix2025Theme {
            LazyRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(itemList) { index, it ->
                    Button(modifier = Modifier.padding(horizontal = 4.dp), onClick = {


                        val ringtonesUrl = StringBuilder().append(MusixRingtonesList.URL)
                            .append(MusixRingtonesList.ringtoneUrlList[index])

//                        currentRingtoneUrl = ringtonesUrl.toString()
//                        Log.v("MainActivity", currentRingtoneUrl)


                    }) {
                        Text(text = it.dropLast(5))
                    }
                }
            }
        }
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





