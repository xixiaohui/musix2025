package com.xxh.ringbones.presentation.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xxh.ringbones.R
import com.xxh.ringbones.presentation.common.GradientBackground
import kotlinx.coroutines.delay

/** Duration in ms before the search query triggers auto-search (debounce). */
private const val SEARCH_DEBOUNCE_MS = 300L

/** Horizontal padding for hero content. */
private val HERO_HORIZONTAL = 24.dp

/**
 * Hero header section with dynamic gradient background, pulsing app icon,
 * branding text, and a glass-style search bar with debounce auto-search.
 *
 * Uses compact vertical spacing to show more content above the fold.
 *
 * @param onSearch Callback when user submits or debounce triggers
 * @param modifier External modifier
 */
@Composable
fun HeroHeader(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    val heroColors = remember(primaryColor, primaryContainer, surfaceColor) {
        listOf(primaryColor, primaryContainer, surfaceColor)
    }

    GradientBackground(
        modifier = modifier.fillMaxWidth(),
        colors = heroColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HERO_HORIZONTAL, 20.dp, HERO_HORIZONTAL, 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App icon with pulsing ring
            PulsingAppIcon()

            Spacer(modifier = Modifier.height(12.dp))

            // App name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Tagline
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Glass search bar with debounce
            GlassSearchBar(
                onSearch = onSearch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * App icon with animated pulsing ring background.
 *
 * Uses the launcher icon (musicology) for brand consistency.
 */
@Composable
private fun PulsingAppIcon(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        PulseRing(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
        )

        // App launcher icon
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.mipmap.musicology),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Animated pulsing ring rendered behind the music icon.
 */
@Composable
private fun PulseRing(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            visible = !visible
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(1200)),
        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(1200))
    ) {
        Box(modifier = modifier)
    }
}

/**
 * Glass-effect search bar with automatic debounce search.
 */
@Composable
internal fun GlassSearchBar(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MS)
        if (query.isNotBlank()) {
            onSearch(query.trim())
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .height(56.dp),
        placeholder = {
            Text(
                text = stringResource(R.string.placeholder_search),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onSearch(query.trim()) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewHeroHeader() {
    MaterialTheme {
        HeroHeader(onSearch = {})
    }
}
