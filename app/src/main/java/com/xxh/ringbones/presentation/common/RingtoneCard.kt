package com.xxh.ringbones.presentation.common

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

/** Collection of gradient pairs for category avatar backgrounds. */
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
 * Modern glassmorphism ringtone card for use in lists and search results.
 *
 * Displays a gradient avatar circle, title, author, duration, and a play button.
 * Uses a semi-transparent surface for a frosted-glass aesthetic.
 *
 * @param ringtone Domain model with ringtone metadata
 * @param onClick Callback when the card is tapped
 * @param avatarModifier Additional modifier for the avatar circle (e.g. SharedTransition)
 * @param modifier External modifier
 */
@Composable
fun RingtoneCard(
    ringtone: Ringtone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier
) {
    val gradient = remember(ringtone.category) {
        avatarGradient(ringtone.category)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Gradient avatar circle
        Box(
            modifier = avatarModifier
                .size(52.dp)
                .clip(CircleShape)
                .background(brush = gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Metadata column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ringtone.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = ringtone.author,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = ringtone.duration,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }

        // Play button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.play),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewRingtoneCard() {
    val ringtone = Ringtone(
        id = 1, title = "Kailasanadan", author = "Sanu",
        duration = "Dec 30, 2014",
        url = "https://dl.prokerala.com/downloads/ringtones/files/mp3/sample.mp3",
        mimeType = "audio/mpeg", category = "Music"
    )
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            RingtoneCard(ringtone = ringtone, onClick = {})
        }
    }
}