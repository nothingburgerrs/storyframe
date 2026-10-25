package com.fakegram.profile.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.ui.accounts.AccountsScreen
import com.fakegram.profile.ui.bubble.BubbleChatScreen
import com.fakegram.profile.ui.bubble.BubbleHomeScreen
import com.fakegram.profile.ui.google.PanelScreen
import com.fakegram.profile.ui.google.PanelsHomeScreen
import com.fakegram.profile.ui.home.HomeScreen
import com.fakegram.profile.ui.post.PostViewerScreen
import com.fakegram.profile.ui.pinterest.PinterestScreen
import com.fakegram.profile.ui.profile.ProfileScreen
import com.fakegram.profile.ui.shell.CalendarDayScreen
import com.fakegram.profile.ui.shell.CalendarScreen
import com.fakegram.profile.ui.shell.GroupChatsScreen
import com.fakegram.profile.ui.shell.GroupPanelsScreen
import com.fakegram.profile.ui.shell.LocalEditMode
import com.fakegram.profile.ui.shell.PhoneHomeScreen
import com.fakegram.profile.ui.shell.ShellRoutes
import com.fakegram.profile.ui.shell.ShellTabs
import com.fakegram.profile.ui.shell.ShellViewModel
import com.fakegram.profile.ui.youtube.YoutubeListScreen
import com.fakegram.profile.ui.youtube.YoutubeWatchScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_PROFILE = "profile/{profileId}"
private const val ROUTE_PANEL = "panel/{panelId}"
private const val ROUTE_POST = "post/{profileId}/{postId}"
private const val ROUTE_CHAT = "bubble/{chatId}"
private const val ROUTE_PINTEREST = "pinterest/{pageId}"
private const val ROUTE_YT_LIST = "youtubeList/{characterId}"
private const val ROUTE_YT_VIDEO = "video/{videoId}"
private const val ROUTE_GROUP_GOOGLE = "groupGoogle/{characterId}"
private const val ROUTE_GROUP_BUBBLE = "groupBubble/{characterId}"

/**
 * Welcome -> Home (Hub / Apps tabs) -> the platform screens. Screens cross-fade instead of the
 * default hard cut, which is what made switching between them feel jumpy.
 */
@Composable
fun StoryframeNavHost(repository: ProfileRepository) {
    val navController: NavHostController = rememberNavController()
    val app = LocalContext.current.applicationContext as StoryframeApp
    // One shell state (characters + who is active) shared by every screen.
    val shell: ShellViewModel = viewModel(factory = ShellViewModel.factory(app, repository, app.themePreferences))

    val editing by shell.editMode.collectAsState()

    CompositionLocalProvider(LocalEditMode provides editing) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        // Apps grow in from the home screen and shrink back out, a bit like a real phone.
        enterTransition = { fadeIn(tween(220)) + scaleIn(tween(300), initialScale = 0.94f) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(260), targetScale = 0.94f) }
    ) {
        composable(ROUTE_HOME) {
            PhoneHomeScreen(shell = shell, onNavigate = { route -> navController.navigate(route) })
        }
        composable(ShellRoutes.STUDIO) {
            ShellTabs(shell = shell, onNavigate = { route -> navController.navigate(route) })
        }

        // ---- platform sub-screens, reached from the Apps tab ----
        composable(ShellRoutes.INSTAGRAM_LIST) {
            HomeScreen(
                repository = repository,
                onOpenProfile = { id -> navController.navigate("profile/$id") },
                onBack = { navController.popBackStack() },
                onProfileCreated = { id -> shell.linkInstagramIfMissing(id) }
            )
        }
        composable(ShellRoutes.GOOGLE_LIST) {
            PanelsHomeScreen(
                repository = repository,
                onOpenPanel = { id -> navController.navigate("panel/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(ShellRoutes.BUBBLE_LIST) {
            BubbleHomeScreen(
                repository = repository,
                onOpenChat = { id -> navController.navigate("bubble/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            ROUTE_PINTEREST,
            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
        ) { backStackEntry ->
            PinterestScreen(
                pageId = backStackEntry.arguments?.getLong("pageId") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }
        composable(ShellRoutes.CALENDAR) {
            CalendarScreen(
                shell = shell,
                onOpenDay = { date -> navController.navigate("calendarDay/$date") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "calendarDay/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            CalendarDayScreen(
                shell = shell,
                date = backStackEntry.arguments?.getString("date").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            ROUTE_YT_LIST,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) { backStackEntry ->
            YoutubeListScreen(
                characterId = backStackEntry.arguments?.getLong("characterId") ?: 0L,
                shell = shell,
                onOpenVideo = { id -> navController.navigate("video/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            ROUTE_YT_VIDEO,
            arguments = listOf(navArgument("videoId") { type = NavType.LongType })
        ) { backStackEntry ->
            YoutubeWatchScreen(
                videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L,
                onBack = { navController.popBackStack() },
                onOpenVideo = { id -> navController.navigate("video/$id") }
            )
        }
        composable(ShellRoutes.ACCOUNTS) {
            AccountsScreen(repository = repository, onBack = { navController.popBackStack() })
        }

        composable(
            ROUTE_GROUP_GOOGLE,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) { backStackEntry ->
            GroupPanelsScreen(
                shell = shell, repository = repository,
                groupId = backStackEntry.arguments?.getLong("characterId") ?: 0L,
                onOpenPanel = { id -> navController.navigate("panel/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            ROUTE_GROUP_BUBBLE,
            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
        ) { backStackEntry ->
            GroupChatsScreen(
                shell = shell, repository = repository,
                groupId = backStackEntry.arguments?.getLong("characterId") ?: 0L,
                onOpenChat = { id -> navController.navigate("bubble/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            ROUTE_PROFILE,
            arguments = listOf(navArgument("profileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L
            ProfileScreen(
                profileId = profileId,
                onBack = { navController.popBackStack() },
                onOpenPost = { postId -> navController.navigate("post/$profileId/$postId") }
            )
        }
        composable(
            ROUTE_POST,
            arguments = listOf(
                navArgument("profileId") { type = NavType.LongType },
                navArgument("postId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            PostViewerScreen(
                profileId = backStackEntry.arguments?.getLong("profileId") ?: 0L,
                postId = backStackEntry.arguments?.getLong("postId") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            ROUTE_CHAT,
            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
        ) { backStackEntry ->
            BubbleChatScreen(
                chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            ROUTE_PANEL,
            arguments = listOf(navArgument("panelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val panelId = backStackEntry.arguments?.getLong("panelId") ?: 0L
            PanelScreen(panelId = panelId, onBack = { navController.popBackStack() })
        }
    }
    }
}
