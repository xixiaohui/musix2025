package com.xxh.ringbones.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.xxh.ringbones.presentation.favorites.FavoritesScreen
import com.xxh.ringbones.presentation.history.HistoryScreen
import com.xxh.ringbones.presentation.home.HomeScreen
import com.xxh.ringbones.presentation.player.PlayerScreen
import com.xxh.ringbones.presentation.prokerala.ProkeralaListScreen
import com.xxh.ringbones.presentation.search.SearchResultScreen

/**
 * Root navigation graph that wires all top-level destinations.
 *
 * Navigation flow:
 * - Home → Search/Category (via query param)
 * - Home → ProkeralaList / Favorites / PlayHistory (via See All)
 * - Any list → Player (via ringtone ID)
 * - Player can go back to previous screen or enter PiP
 *
 * @param navController Host controller for type-safe navigation
 * @param isInPiPMode Whether the activity is currently in Picture-in-Picture mode
 * @param onEnterPiP Callback to request entering Picture-in-Picture mode
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    isInPiPMode: Boolean = false,
    onEnterPiP: () -> Unit = {},
) {
    NavHost(navController = navController, startDestination = Route.Home) {
        // ── Home Screen ──
        composable<Route.Home> {
            HomeScreen(
                onSearch = { query ->
                    navController.navigate(Route.Search(query = query, byCategory = false))
                },
                onCategoryClick = { type ->
                    navController.navigate(Route.Search(query = type, byCategory = true))
                },
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onProkeralaSeeAll = {
                    navController.navigate(Route.ProkeralaList)
                },
                onFavoritesSeeAll = {
                    navController.navigate(Route.Favorites)
                },
                onPlayHistorySeeAll = {
                    navController.navigate(Route.PlayHistory)
                }
            )
        }

        // ── Search / Category Detail Screen ──
        composable<Route.Search> { backStackEntry ->
            backStackEntry.toRoute<Route.Search>()
            SearchResultScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Prokerala List Screen ──
        composable<Route.ProkeralaList> {
            ProkeralaListScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Player Screen ──
        composable<Route.Player> { backStackEntry ->
            backStackEntry.toRoute<Route.Player>()
            PlayerScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                isInPiPMode = isInPiPMode,
                onEnterPiP = onEnterPiP,
            )
        }

        // ── Favorites Screen ──
        composable<Route.Favorites> {
            FavoritesScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // ── Play History Screen ──
        composable<Route.PlayHistory> {
            HistoryScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}