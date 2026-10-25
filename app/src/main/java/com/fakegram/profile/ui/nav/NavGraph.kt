package com.fakegram.profile.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.ui.home.HomeScreen
import com.fakegram.profile.ui.profile.ProfileScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_PROFILE = "profile/{profileId}"

@Composable
fun StoryframeNavHost(repository: ProfileRepository) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                repository = repository,
                onOpenProfile = { id -> navController.navigate("profile/$id") }
            )
        }
        composable(
            ROUTE_PROFILE,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            ProfileScreen(
                profileId = profileId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
