package com.xxh.ringbones.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.common.LoadingIndicator
import com.xxh.ringbones.presentation.search.components.CollapsingHeader
import com.xxh.ringbones.presentation.search.components.RankedRingtoneCard

/**
 * Immersive search / category detail result screen.
 *
 * Features a collapsing gradient header banner, sticky category title,
 * ranked ringtone cards with hot indicators, and staggered entrance animations.
 *
 * @param onRingtoneClick Called when a card is tapped, navigates to Player
 * @param onBackClick Called when the back arrow is pressed
 * @param viewModel Injected ViewModel providing ringtone data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    onRingtoneClick: (Ringtone) -> Unit,
    onBackClick: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val ringtones by viewModel.ringtones.collectAsState()
    val categoryName by viewModel.categoryName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        if (isLoading) {
            LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else if (ringtones.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_ringtones_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Collapsing gradient banner
                item(key = "banner") {
                    CollapsingHeader(
                        category = categoryName,
                        count = ringtones.size,
                        visible = true
                    )
                }

                // Sticky category header
                stickyHeader(key = "sticky_header") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                            .padding(20.dp, 12.dp, 20.dp, 12.dp)
                    ) {
                        Text(
                            text = "${ringtones.size} ${stringResource(R.string.ringtones)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Ranked ringtone cards
                itemsIndexed(ringtones) { index, ringtone ->
                    RankedRingtoneCard(
                        ringtone = ringtone,
                        rank = if (index < 10) index + 1 else 0,
                        index = index,
                        onClick = { onRingtoneClick(ringtone) }
                    )
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSearchResultScreen() {
    MaterialTheme {
        SearchResultScreen(
            onRingtoneClick = {},
            onBackClick = {}
        )
    }
}