package com.xxh.ringbones.presentation.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.R
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.presentation.common.ENTER_DELAY_MS

/** Maximum play count considered "hot" — top 20% threshold. */
private const val HOT_PLAY_COUNT_THRESHOLD = 5

/** Gradient palette for category avatar backgrounds. */
private val avatarGradientPalette = listOf(
    listOf(Color(0xFF7F4D7A), Color(0xFFD4A0D0)),
    listOf(Color(0xFF3A608F), Color(0xFF7AACF0)),
    listOf(Color(0xFF825246), Color(0xFFD49480)),
    listOf(Color(0xFF2E5A3E), Color(0xFF6EBE82)),
    listOf(Color(0xFF8E4A2C), Color(0xFFE08860)),
    listOf(Color(0xFF4A4080), Color(0xFF8E80E0)),
    listOf(Color(0xFF2E6B5E), Color(0xFF5CC8B0)),
    listOf(Color(0xFF8C3A4A), Color(0xFFE06880)),
    listOf(Color(0xFF3E5C8A), Color(0xFF789FE0)),
    listOf(Color(0xFF6B3A7A), Color(0xFFC070E0)),
    listOf(Color(0xFF4A7A5C), Color(0xFF80D8A0))
)

/**
 * Returns a deterministic gradient for a category string.
 */
private fun avatarGradient(category: String): Brush {
    val idx = kotlin.math.abs(category.hashCode()) % avatarGradientPalette.size
    val colors = avatarGradientPalette[idx]
    return Brush.linearGradient(colors)
}

/**
 * Ranked ringtone card for the search/category list.
 *
 * Displays a rank badge (#1-#10), a glass-style card with avatar, metadata,
 * hot indicator (fire icon for top-played items), and a play button.
 *
 * @param ringtone Domain model with ringtone metadata
 * @param rank 1-based position in the list (0 = no rank shown)
 * @param index 0-based position for staggered entrance delay
 * @param onClick Callback when the card is tapped
 * @param modifier External modifier
 */
@Composable
fun RankedRingtoneCard(
    ringtone: Ringtone,
    rank: Int = 0,
    index: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = remember(ringtone.category) {
        avatarGradient(ringtone.category)
    }
    val isHot = ringtone.playCount >= HOT_PLAY_COUNT_THRESHOLD

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 300,
                delayMillis = index * ENTER_DELAY_MS
            )
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 5.dp, 16.dp, 5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Rank badge (top 10)
            if (rank in 1..10) {
                RankBadge(rank = rank)
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Gradient avatar circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(brush = gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ringtone.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isHot) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = stringResource(R.string.popular_now),
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFFF6D00)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(4.dp))

                Text(
                    text = ringtone.author,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.size(2.dp))

                Text(
                    text = ringtone.duration,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // Play button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Rank badge showing gold/silver/bronze circle with the rank number.
 *
 * @param rank 1-based rank number
 */
@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (rank) {
        1 -> Color(0xFFFFD700) to Color(0xFF5C3D00) // Gold
        2 -> Color(0xFFC0C0C0) to Color(0xFF3D3D3D) // Silver
        3 -> Color(0xFFCD7F32) to Color(0xFF3D2000) // Bronze
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewRankedRingtoneCard() {
    val ringtone = Ringtone(
        id = 1, title = "Kailasanadan", author = "Sanu",
        duration = "Dec 30, 2014",
        url = "https://example.com/sample.mp3",
        mimeType = "audio/mpeg", category = "Music", playCount = 12
    )
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            RankedRingtoneCard(
                ringtone = ringtone,
                rank = 1,
                index = 0,
                onClick = {}
            )
        }
    }
}