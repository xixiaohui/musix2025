package com.xxh.ringbones.presentation.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.core.player.model.EqPreset

/**
 * EQ preset selector list.
 *
 * Displays all [EqPreset] values as tappable rows with a checkmark
 * next to the currently active preset. Tapping a row selects it.
 *
 * @param currentPreset Currently active EQ preset
 * @param onPresetSelected Callback with the selected preset
 * @param modifier External modifier
 */
@Composable
fun EqSelector(
    currentPreset: EqPreset,
    onPresetSelected: (EqPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.padding(16.dp, 0.dp, 16.dp, 0.dp)) {
        Text(
            text = "Equalizer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        EqPreset.entries.forEach { preset ->
            val isSelected = preset == currentPreset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPresetSelected(preset) }
                    .padding(0.dp, 14.dp, 0.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatPresetName(preset),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * Formats [EqPreset] enum name into a human-readable string.
 */
private fun formatPresetName(preset: EqPreset): String = when (preset) {
    EqPreset.FLAT -> "Flat"
    EqPreset.BASS_BOOST -> "Bass Boost"
    EqPreset.TREBLE_BOOST -> "Treble Boost"
    EqPreset.VOCAL -> "Vocal"
    EqPreset.CLASSICAL -> "Classical"
    EqPreset.ROCK -> "Rock"
    EqPreset.POP -> "Pop"
    EqPreset.CUSTOM -> "Custom"
}

@Preview(showBackground = true)
@Composable
private fun PreviewEqSelector() {
    MaterialTheme {
        EqSelector(currentPreset = EqPreset.FLAT, onPresetSelected = {})
    }
}