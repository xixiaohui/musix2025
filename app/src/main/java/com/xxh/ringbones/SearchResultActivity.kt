package com.xxh.ringbones

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxh.ringbones.data.Ringtone
import com.xxh.ringbones.data.RingtoneViewModel
import com.xxh.ringbones.data.RingtoneViewModelFactory
import com.xxh.ringbones.ui.theme.Musix2025Theme
import kotlin.reflect.KFunction2

/**
 * 分类页面
 * 展示查询结果
 */
class SearchResultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bundle: Bundle? = intent.extras
        var index = 0
        bundle?.let {
            bundle.apply {
                index = getInt("EXTRA_INFO")
            }
        }
        Log.v("musixSearchResultActivity",index.toString())

        setContent {
            DatabaseScreen()
        }
    }

    @Composable
    fun DatabaseScreen() {
        val viewModel: RingtoneViewModel =
            viewModel(factory = RingtoneViewModelFactory(application))

//        val ringtones by viewModel.ringtones.collectAsState()

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                RingtonesList(false, viewModel)
            }
        }
    }

}


@Composable
fun RingtonesList(loading: Boolean, ringtoneViewModel: RingtoneViewModel) {

    val ringtoneList by ringtoneViewModel.ringtones.collectAsState()

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


@Composable
fun RingtoneCard(ringtone: Ringtone, navigateToPlay: KFunction2<Context, Ringtone, Unit>) {

    val context = LocalContext.current
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(start = 2.dp, end = 2.dp),

        onClick = {
            navigateToPlay(context,ringtone)
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

fun navigateToPlayActivity(context: Context,ringtone: Ringtone) {
    //跳转到下一个activity
    val intent = Intent(context, PlayActivity::class.java).apply {
//                    putExtra("EXTRA_INFO", ringtone as Serializable)
        putExtra("EXTRA_INFO", ringtone as Parcelable)
    }
    context.startActivity(intent)
}