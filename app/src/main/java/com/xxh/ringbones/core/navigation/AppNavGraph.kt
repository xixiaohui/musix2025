package com.xxh.ringbones.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

/**
 * Root navigation graph that wires all top-level destinations.
 * Screens are filled in during presentation layer tasks.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home
    ) {
        composable<Route.Home> {
            // Placeholder — replaced by Task 15
            androidx.compose.material3.Text("Home")
        }
        composable<Route.Search> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Search>()
            // Placeholder — replaced by Task 16
            androidx.compose.material3.Text("Search: ${route.query}")
        }
        composable<Route.Player> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Player>()
            // Placeholder — replaced by Task 17
            androidx.compose.material3.Text("Player: ${route.ringtoneId}")
        }
    }
}
