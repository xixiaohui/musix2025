package com.xxh.ringbones.presentation.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Collapsing gradient banner for the category detail / search results page.
 *
 * Displays the category name, track count, and a decorative music icon.
 * Visually contracts as the user scrolls the list down.
 *
 * @param category Category display name
 * @param count Number of ringtones in this category
 * @param visible Whether the banner is expanded (hidden when scrolled down)
 * @param modifier External modifier
 */
@Composable
fun CollapsingHeader(
    category: String,
    count: Int,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gradientColors = remember(category) {
        val hue1 = Math.floorMod(category.hashCode() * 37L, 360).toInt()
        val hue2 = (hue1 + 40) % 360
        listOf(
            androidx.compose.ui.graphics.Color.hsl(hue1.toFloat(), 0.6f, 0.45f),
            androidx.compose.ui.graphics.Color.hsl(hue2.toFloat(), 0.5f, 0.35f),
            surfaceColor
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp, 16.dp, 0.dp)
                .height(110.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.horizontalGradient(gradientColors)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Decorative icon
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.CenterEnd)
                    .padding(0.dp, 0.dp, 20.dp, 0.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp, 0.dp, 24.dp, 0.dp)
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$count ringtones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCollapsingHeader() {
    MaterialTheme {
        CollapsingHeader(
            category = "Music",
            count = 132,
            visible = true
        )
    }
}