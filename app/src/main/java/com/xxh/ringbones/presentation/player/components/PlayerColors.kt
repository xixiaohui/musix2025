package com.xxh.ringbones.presentation.player.components

import androidx.compose.ui.graphics.Color

/**
 * Explicit light-on-dark color palette for the player screen.
 *
 * The player [ImmersiveBackground] is always deep-dark regardless of system
 * theme, so all text and surfaces must use light colors derived from White
 * rather than theme-dependent [androidx.compose.material3.MaterialTheme.colorScheme]
 * values (which would be dark-on-dark in light mode).
 */
object PlayerColors {

    /** Primary text — titles, main labels, button text. */
    val textPrimary = Color.White

    /** Secondary text — artist names, descriptions. */
    val textSecondary = Color.White.copy(alpha = 0.70f)

    /** Tertiary/muted text — small labels, time codes, hints. */
    val textMuted = Color.White.copy(alpha = 0.50f)

    /** Disabled text / inactive icons. */
    val textDisabled = Color.White.copy(alpha = 0.30f)

    /** Glass card and button surface background. */
    val glassSurface = Color.White.copy(alpha = 0.10f)

    /** Lighter glass variant for card overlays. */
    val glassLight = Color.White.copy(alpha = 0.06f)

    /** Track background, dividers, subtle separators. */
    val trackBackground = Color.White.copy(alpha = 0.12f)

    /** Snackbar / elevated surface background. */
    val elevatedSurface = Color.White.copy(alpha = 0.18f)
}