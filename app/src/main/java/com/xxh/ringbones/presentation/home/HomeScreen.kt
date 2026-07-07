package com.xxh.ringbones.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.home.components.CategoryChipRow
import com.xxh.ringbones.presentation.home.components.CategoryGrid
import com.xxh.ringbones.presentation.home.components.FeaturedRow
import com.xxh.ringbones.presentation.home.components.HeroHeader

/** Unified horizontal padding for all home sections. */
private val HOME_HORIZONTAL = 24.dp

/** Vertical spacing between major sections. */
private val SECTION_SPACING = 28.dp

/**
 * Modern glassmorphism HomeScreen with dynamic gradient hero, animated category chips,
 * snap-scroll featured ringtones, prokerala section, favorites section, play history
 * section, and staggered category grid.
 *
 * All sections use a unified 24dp horizontal padding and consistent vertical
 * spacing rhythm for a polished, professional feel.
 *
 * @param onSearch Callback when user submits a search query
 * @param onCategoryClick Callback when a category chip or grid card is clicked
 * @param onRingtoneClick Callback when a featured ringtone card is tapped → navigates to Player
 * @param onProkeralaSeeAll Callback when user taps "See All" in the prokerala section
 * @param onFavoritesSeeAll Callback when user taps "See All" in the favorites section
 * @param onPlayHistorySeeAll Callback when user taps "See All" in the play history section
 * @param viewModel Hilt-injected ViewModel providing dynamic data
 */
@Composable
fun HomeScreen(
    onSearch: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
    onProkeralaSeeAll: () -> Unit,
    onFavoritesSeeAll: () -> Unit,
    onPlayHistorySeeAll: () -> Unit,
    onDownloadsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val availableCategories by viewModel.availableCategories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val featuredRingtones by viewModel.featuredRingtones.collectAsState()
    val prokeralaRingtones by viewModel.prokeralaRingtones.collectAsState()
    val favoriteRingtones by viewModel.favoriteRingtones.collectAsState()
    val recentPlays by viewModel.recentPlays.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Hero Header ──
        item(key = "hero") {
            HeroHeader(onSearch = onSearch)
        }

        // ── Downloads entry ──
        item(key = "downloads_entry") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = true, onClick = onDownloadsClick)
                    .padding(horizontal = HOME_HORIZONTAL, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Manage →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // ── Category Chips ──
        if (availableCategories.isNotEmpty()) {
            item(key = "section_categories") {
                SectionHeader(
                    title = stringResource(R.string.browse_categories),
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "chip_row") {
                CategoryChipRow(
                    categories = availableCategories,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        // ── Featured / Popular Now ──
        if (featuredRingtones.isNotEmpty()) {
            item(key = "section_popular") {
                SectionHeader(
                    title = stringResource(R.string.popular_now),
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "featured_row") {
                FeaturedRow(
                    ringtones = featuredRingtones,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Prokerala Ringtones ──
        if (prokeralaRingtones.isNotEmpty()) {
            item(key = "section_prokerala") {
                SectionHeader(
                    title = stringResource(R.string.prokerala),
                    onSeeAll = onProkeralaSeeAll,
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "prokerala_featured_row") {
                FeaturedRow(
                    ringtones = prokeralaRingtones,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Favorites ──
        if (favoriteRingtones.isNotEmpty()) {
            item(key = "section_favorites") {
                SectionHeader(
                    title = stringResource(R.string.favorites),
                    onSeeAll = onFavoritesSeeAll,
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "favorites_row") {
                FeaturedRow(
                    ringtones = favoriteRingtones,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Recently Played ──
        if (recentPlays.isNotEmpty()) {
            item(key = "section_recent_plays") {
                SectionHeader(
                    title = stringResource(R.string.play_history),
                    onSeeAll = onPlayHistorySeeAll,
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "recent_plays_row") {
                FeaturedRow(
                    ringtones = recentPlays,
                    onRingtoneClick = onRingtoneClick
                )
            }
        }

        // ── Browse All Categories Grid ──
        if (categoryCounts.isNotEmpty()) {
            item(key = "section_browse") {
                SectionHeader(
                    title = stringResource(R.string.browse_all),
                    modifier = Modifier.padding(top = SECTION_SPACING)
                )
            }
            item(key = "category_grid") {
                CategoryGrid(
                    categories = categoryCounts,
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

/**
 * Consistent section title with unified horizontal padding.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HOME_HORIZONTAL)
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = stringResource(R.string.see_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
