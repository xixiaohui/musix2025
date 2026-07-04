package com.xxh.ringbones.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.presentation.common.ENTER_DELAY_MS

/** Grid column count. */
private const val COLUMNS = 2

/** Height per grid cell. */
private val CELL_HEIGHT = 112.dp

/** Unified horizontal padding. */
private val GRID_HORIZONTAL = 24.dp

/** Gap between grid cells. */
private val GRID_GAP = 14.dp

/**
 * 2-column grid of category cards with staggered entrance animations.
 *
 * Each card shows a gradient background, category name, decorative icon,
 * and the number of ringtones in that category.
 *
 * @param categories Category display data (name → count)
 * @param onCategoryClick Callback when a category card is tapped
 * @param modifier External modifier
 */
@Composable
fun CategoryGrid(
    categories: Map<String, Int>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = categories.entries.toList()
    val rowCount = (entries.size + COLUMNS - 1) / COLUMNS
    val totalHeight = (CELL_HEIGHT + GRID_GAP) * rowCount + 8.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentPadding = PaddingValues(horizontal = GRID_HORIZONTAL),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP),
        userScrollEnabled = false
    ) {
        itemsIndexed(entries) { index, (category, count) ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 350,
                        delayMillis = index * ENTER_DELAY_MS
                    )
                ) + scaleIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 350,
                        delayMillis = index * ENTER_DELAY_MS
                    )
                )
            ) {
                CategoryCard(
                    category = category,
                    count = count,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

/**
 * Individual category grid card with gradient background, icon, name, and count.
 *
 * @param category Category display name
 * @param count Number of ringtones in this category
 * @param onClick Click callback
 * @param modifier External modifier
 */
@Composable
private fun CategoryCard(
    category: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = remember(category) {
        categoryCardGradient(category)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CELL_HEIGHT)
            .clip(RoundedCornerShape(20.dp))
            .background(brush = gradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background icon — subtle, large, offset to top-end
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopEnd)
                .padding(0.dp, 6.dp, 6.dp, 0.dp),
            tint = Color.White.copy(alpha = 0.18f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            // Category name
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // Track count
            Text(
                text = "$count ${if (count == 1) "track" else "tracks"}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Gradient palette for category cards — deterministic by category name. */
private val categoryGradientPalette = listOf(
    listOf(Color(0xFF7F4D7A), Color(0xFFB388B0)),
    listOf(Color(0xFF3A608F), Color(0xFF5C8AC9)),
    listOf(Color(0xFF825246), Color(0xFFB87A6C)),
    listOf(Color(0xFF2E5A3E), Color(0xFF5C9A6E)),
    listOf(Color(0xFF8E4A2C), Color(0xFFCC7A52)),
    listOf(Color(0xFF4A4080), Color(0xFF7A70B8)),
    listOf(Color(0xFF2E6B5E), Color(0xFF56A898)),
    listOf(Color(0xFF8C3A4A), Color(0xFFC46072)),
    listOf(Color(0xFF3E5C8A), Color(0xFF6688C0)),
    listOf(Color(0xFF6B3A7A), Color(0xFFA060B8)),
    listOf(Color(0xFF4A7A5C), Color(0xFF72B088))
)

/**
 * Returns a deterministic vertical gradient for the given category.
 */
private fun categoryCardGradient(category: String): Brush {
    val idx = kotlin.math.abs(category.hashCode()) % categoryGradientPalette.size
    val colors = categoryGradientPalette[idx]
    return Brush.verticalGradient(colors)
}

@Preview(showBackground = true)
@Composable
private fun PreviewCategoryGrid() {
    MaterialTheme {
        CategoryGrid(
            categories = mapOf("Music" to 132, "Funny" to 48, "Baby" to 35, "iPhone" to 52),
            onCategoryClick = {}
        )
    }
}
