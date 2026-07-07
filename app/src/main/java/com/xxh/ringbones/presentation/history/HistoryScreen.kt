package com.xxh.ringbones.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.common.LoadingIndicator
import com.xxh.ringbones.presentation.common.RingtoneCard

/** Vertical spacing between ringtone cards in the list. */
private val CARD_SPACING = 4.dp

/**
 * Full list screen displaying recently played ringtones.
 *
 * Shows a top app bar with back navigation, a loading indicator while data
 * loads, an empty state message if no ringtones have been played, and a
 * scrollable list of [RingtoneCard] items when data is available.
 *
 * Long-pressing a card shows a confirmation dialog to remove it from history.
 *
 * @param onRingtoneClick Called when a ringtone card is tapped, navigates to Player
 * @param onBackClick Called when the back arrow is pressed
 * @param viewModel Hilt-injected ViewModel providing play history data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onRingtoneClick: (Ringtone) -> Unit,
    onBackClick: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtones by viewModel.ringtones.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Dialog state for long-press removal confirmation
    var ringtoneToRemove by remember { mutableStateOf<Ringtone?>(null) }

    // Removal confirmation dialog
    ringtoneToRemove?.let { ringtone ->
        AlertDialog(
            onDismissRequest = { ringtoneToRemove = null },
            title = {
                Text(
                    text = stringResource(R.string.remove_from_history_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.remove_from_history_message, ringtone.title),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFromHistory(ringtone.id)
                    ringtoneToRemove = null
                }) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { ringtoneToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.play_history),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            ringtones.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_play_history_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(CARD_SPACING)
                ) {
                    itemsIndexed(ringtones) { _, ringtone ->
                        RingtoneCard(
                            ringtone = ringtone,
                            onClick = { onRingtoneClick(ringtone) },
                            onLongClick = { ringtoneToRemove = ringtone }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHistoryScreen() {
    MaterialTheme {
        HistoryScreen(
            onRingtoneClick = {},
            onBackClick = {}
        )
    }
}