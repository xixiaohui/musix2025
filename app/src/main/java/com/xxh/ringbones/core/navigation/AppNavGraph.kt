package com.xxh.ringbones.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.xxh.ringbones.presentation.home.HomeScreen
import com.xxh.ringbones.presentation.player.PlayerScreen
import com.xxh.ringbones.presentation.search.SearchResultScreen

/**
 * Root navigation graph that wires all top-level destinations.
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Home) {
        composable<Route.Home> {
            HomeScreen(
                onSearch = { query ->
                    navController.navigate(Route.Search(query = query, byCategory = false))
                },
                onCategoryClick = { type ->
                    navController.navigate(Route.Search(query = type, byCategory = true))
                }
            )
        }
        composable<Route.Search> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Search>()
            SearchResultScreen(
                onRingtoneClick = { ringtone ->
                    navController.navigate(Route.Player(ringtoneId = ringtone.id))
                }
            )
        }
        composable<Route.Player> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Player>()
            PlayerScreen()
        }
    }
}
