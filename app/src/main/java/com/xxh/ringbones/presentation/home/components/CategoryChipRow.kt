package com.xxh.ringbones.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Unified horizontal content padding matching other home sections. */
private val CHIP_ROW_HORIZONTAL = 24.dp

/**
 * Horizontally scrollable row of filter chips for ringtone categories.
 *
 * Uses unified 24dp horizontal padding matching SectionHeader and other
 * home sections for visual consistency.
 *
 * @param categories List of category names to display
 * @param onCategoryClick Callback with the selected category name
 * @param modifier External modifier
 */
@Composable
fun CategoryChipRow(
    categories: List<String>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = CHIP_ROW_HORIZONTAL),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it }) { category ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn()
            ) {
                FilterChip(
                    selected = false,
                    onClick = { onCategoryClick(category) },
                    label = {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCategoryChipRow() {
    MaterialTheme {
        CategoryChipRow(
            categories = listOf("Music", "Funny", "Baby", "iPhone", "Electronic"),
            onCategoryClick = {}
        )
    }
}
