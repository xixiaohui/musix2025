package com.xxh.ringbones.presentation.search.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.common.LoadingIndicator
import com.xxh.ringbones.presentation.common.RingtoneCard

@Composable
fun RingtoneList(
    ringtoneList: List<Ringtone>,
    loading: Boolean,
    onRingtoneClick: (Ringtone) -> Unit,
    modifier: Modifier = Modifier
) {
    if (loading) {
        LoadingIndicator()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp, 24.dp, 0.dp, 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyColumn {
            items(ringtoneList) { ringtone ->
                RingtoneCard(
                    ringtone = ringtone,
                    onClick = { onRingtoneClick(ringtone) }
                )
                Spacer(Modifier.size(2.dp))
            }
        }
    }
}
