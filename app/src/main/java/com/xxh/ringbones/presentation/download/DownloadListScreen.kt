package com.xxh.ringbones.presentation.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.core.download.DownloadStatus
import com.xxh.ringbones.presentation.download.components.DownloadTaskItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        if (state.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No downloads",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                val active = state.tasks.filter { it.status == DownloadStatus.Downloading }
                if (active.isNotEmpty()) {
                    item(key = "header_active") { SectionHeader("Downloading") }
                    items(active, key = { "active_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = { viewModel.pause(task.ringtoneId) },
                            onResume = { viewModel.resume(task.ringtoneId) },
                            onCancel = { viewModel.cancel(task.ringtoneId) },
                            onRetry = { viewModel.retry(task.ringtoneId) },
                        )
                    }
                }

                val paused = state.tasks.filter { it.status == DownloadStatus.Paused }
                if (paused.isNotEmpty()) {
                    item(key = "header_paused") { SectionHeader("Paused") }
                    items(paused, key = { "paused_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = {},
                            onResume = { viewModel.resume(task.ringtoneId) },
                            onCancel = { viewModel.cancel(task.ringtoneId) },
                            onRetry = {},
                        )
                    }
                }

                val pending = state.tasks.filter { it.status == DownloadStatus.Pending }
                if (pending.isNotEmpty()) {
                    item(key = "header_pending") { SectionHeader("Pending") }
                    items(pending, key = { "pending_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = {},
                            onResume = {},
                            onCancel = { viewModel.cancel(task.ringtoneId) },
                            onRetry = {},
                        )
                    }
                }

                val failed = state.tasks.filter { it.status == DownloadStatus.Failed }
                if (failed.isNotEmpty()) {
                    item(key = "header_failed") { SectionHeader("Failed") }
                    items(failed, key = { "failed_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = {},
                            onResume = {},
                            onCancel = { viewModel.cancel(task.ringtoneId) },
                            onRetry = { viewModel.retry(task.ringtoneId) },
                        )
                    }
                }

                val completed = state.tasks.filter { it.status == DownloadStatus.Completed }
                if (completed.isNotEmpty()) {
                    item(key = "header_completed") {
                        SectionHeader(
                            title = "Completed",
                            action = {
                                TextButton(onClick = { viewModel.clearCompleted() }) {
                                    Text(
                                        "Clear All",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                    items(completed, key = { "completed_${it.ringtoneId}" }) { task ->
                        DownloadTaskItem(
                            task = task,
                            onPause = {},
                            onResume = {},
                            onCancel = {},
                            onRetry = {},
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewDownloadListScreen() {
    MaterialTheme {
        DownloadListScreen(onBackClick = {})
    }
}
