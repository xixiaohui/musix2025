package com.xxh.ringbones.core.player.model

/**
 * Sealed interface for one-shot side-effects from the player that the UI
 * should react to exactly once (e.g., navigate away, show snackbar).
 *
 * Consumed via [PlayerViewModel.effects] SharedFlow.
 */
sealed interface PlayerEffect {
    /** Display a temporary message in a snackbar. */
    data class ShowSnackbar(val message: String) : PlayerEffect

    /** Navigate back from the player screen. */
    data object NavigateBack : PlayerEffect

    /** Open system settings for the user to grant a permission. */
    data object OpenWriteSettings : PlayerEffect
}