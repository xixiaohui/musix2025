package com.xxh.ringbones.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.presentation.common.SearchBar
import com.xxh.ringbones.presentation.home.components.CategoryGrid
import com.xxh.ringbones.presentation.home.components.CategoryRow
import com.xxh.ringbones.presentation.home.components.DrawableStringPair

/**
 * Type name mapping: string resource ID -> database type string.
 * Used to map category clicks to the type value stored in Room.
 */
val TypeNameToText = mapOf(
    R.string.funny to "Funny",
    R.string.devotional to "Devotional",
    R.string.tamil to "Tamil",
    R.string.audio_mpeg to "audio/mpeg",
    R.string.iphone to "Iphone",
    R.string.baby to "Baby",
    R.string.sound to "Sound Effects",
    R.string.music to "Music",
    R.string.hindi_bollywood to "Bollywood / Hindi",
    R.string.sms to "SMS  / Message Alert",
    R.string.miscellaneous to "Miscellaneous",
    R.string.malayalam to "Malayalam",
)

private val alignYourBodyData = listOf(
    R.drawable.erik to R.string.hindi_bollywood,
    R.drawable.james to R.string.tamil,
    R.drawable.artem to R.string.sms,
    R.drawable.dushawn to R.string.music,
    R.drawable.hanny to R.string.malayalam,
    R.drawable.james to R.string.funny,
    R.drawable.johanna to R.string.sound,
    R.drawable.leticia to R.string.miscellaneous,
    R.drawable.mohammad to R.string.devotional,
    R.drawable.simon to R.string.baby,
    R.drawable.anthony to R.string.iphone,
).map { DrawableStringPair(it.first, it.second) }

private val favoriteCollectionsData = listOf(
    R.drawable.james to R.string.funny,
    R.drawable.johanna to R.string.sound,
    R.drawable.leticia to R.string.miscellaneous,
    R.drawable.mohammad to R.string.devotional,
    R.drawable.simon to R.string.baby,
    R.drawable.anthony to R.string.iphone,
    R.drawable.erik to R.string.hindi_bollywood,
    R.drawable.james to R.string.tamil,
    R.drawable.artem to R.string.sms,
    R.drawable.dushawn to R.string.music,
    R.drawable.hanny to R.string.malayalam
).map { DrawableStringPair(it.first, it.second) }

@Composable
fun HomeSection(
    title: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(top = 40.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp)
        )
        content()
    }
}

@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        SearchBar(
            onSearch = onSearch,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HomeSection(title = R.string.recommended_collections) {
            CategoryRow(
                items = alignYourBodyData,
                onCategoryClick = onCategoryClick,
                typeNameMap = viewModel.categories
            )
        }
        HomeSection(title = R.string.favorite_collections) {
            CategoryGrid(
                items = favoriteCollectionsData,
                onCategoryClick = onCategoryClick,
                typeNameMap = viewModel.categories
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
