package com.xxh.ringbones.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.BuildConfig
import com.xxh.ringbones.R
import com.xxh.ringbones.data.Ringtone

@Composable
fun RingtoneCard(
    ringtone: Ringtone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(start = 2.dp, end = 2.dp),
        onClick = onClick
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
                        .background(Color(0xFF3700B3), shape = CircleShape)
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

                    if (BuildConfig.ENABLE_FEATURE) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = ringtone.type + " " + ringtone.id,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewRingtoneCard() {
    val ringtone = Ringtone(
        title = "Kailasanadan",
        author = "Sanu",
        time = "Dec 30, 2014",
        url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/satis-song-5294.mp3",
        type = "audio/mpeg"
    )
    RingtoneCard(ringtone = ringtone, onClick = {})
}
