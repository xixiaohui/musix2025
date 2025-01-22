package com.xxh.ringbones

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.NameValueTable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xxh.ringbones.ui.theme.Musix2025Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Musix2025Theme {
//                MainScreen()

                NavigateButton(this)
            }

        }
    }

    @Composable
    fun MainScreen(){
        Column (
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Button(
                onClick = {
                    val intent = Intent(this@MainActivity,LandActivity::class.java)
                    startActivity(intent)
                }
            ) {
                Text("Navigate")
            }
        }
    }

    @Preview
    @Composable
    fun NavigateScreen(){
        NavigateButton(this)
    }
}


@Composable
fun NavigateButton(activity: MainActivity){

    Column (
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ){
        Button(
            onClick = {
                val intent = Intent(activity,LandActivity::class.java)
                activity.startActivity(intent)
            }
        ) {
            Text("Navigate")
        }
    }
}


