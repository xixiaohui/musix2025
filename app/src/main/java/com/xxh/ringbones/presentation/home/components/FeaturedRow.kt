package com.xxh.ringbones.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone

/** Card width for each featured item. */
private val CARD_WIDTH = 156.dp

/** Unified horizontal padding. */
private val FEATURED_HORIZONTAL = 24.dp

/** Gap between cards. */
private val CARD_GAP = 14.dp

/** Collection of gradient color pairs for featured cards. */
private val featuredGradientPalette = listOf(
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
 * Returns a deterministic gradient brush for a given category.
 */
private fun featuredGradient(category: String): Brush {
    val idx = kotlin.math.abs(category.hashCode()) % featuredGradientPalette.size
    val colors = featuredGradientPalette[idx]
    return Brush.verticalGradient(colors)
}

/**
 * Horizontally scrollable row of featured ringtone cards.
 *
 * @param ringtones List of featured ringtones to display
 * @param onRingtoneClick Callback when a card is tapped
 * @param modifier External modifier
 */
@Composable
fun FeaturedRow(
    ringtones: List<Ringtone>,
    onRingtoneClick: (Ringtone, queueIds: List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = FEATURED_HORIZONTAL),
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP)
    ) {
        items(ringtones) { ringtone ->
            FeaturedCard(
                ringtone = ringtone,
                onClick = { onRingtoneClick(ringtone, ringtones.map { it.id }) }
            )
        }
    }
}

/**
 * Individual featured ringtone card with gradient avatar, title, author,
 * and a play button.
 */
@Composable
private fun FeaturedCard(
    ringtone: Ringtone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = remember(ringtone.category) {
        featuredGradient(ringtone.category)
    }

    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gradient cover art circle
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(brush = gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.play),
                modifier = Modifier.size(30.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = ringtone.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Author
        Text(
            text = ringtone.author,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFeaturedRow() {
    MaterialTheme {
        FeaturedRow(
            ringtones = listOf(
                Ringtone(
                    id = 1, title = "Sample Track", author = "Artist",
                    duration = "3:45", url = "", mimeType = "audio/mpeg", category = "Music"
                )
            ),
            onRingtoneClick = { _, _ -> }
        )
    }
}
