package com.xxh.ringbones.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 *
 * Each route is a @Serializable data class or object that the Navigation
 * Compose library uses for compile-time safe route resolution.
 */
@Serializable
sealed class Route {
    /** Home screen — category browsing and search entry. */
    @Serializable
    data object Home : Route()

    /** Search results screen. */
    @Serializable
    data class Search(
        val query: String,
        val byCategory: Boolean = true
    ) : Route()

    /** Player screen — loads ringtone by ID from the repository, with optional queue. */
    @Serializable
    data class Player(
        val ringtoneId: Long,
        val queueIds: List<Long> = emptyList(),
    ) : Route()

    /** Prokerala ringtone list screen. */
    @Serializable
    data object ProkeralaList : Route()
}
