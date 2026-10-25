package com.fakegram.profile.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.ui.bubble.BubbleChatScreen
import com.fakegram.profile.ui.bubble.BubbleHomeScreen
import com.fakegram.profile.ui.google.GoogleHomeScreen
import com.fakegram.profile.ui.google.PanelScreen
import com.fakegram.profile.ui.home.HomeScreen
import com.fakegram.profile.ui.phone.LocalEditMode
import com.fakegram.profile.ui.phone.MessagesScreen
import com.fakegram.profile.ui.phone.NotesScreen
import com.fakegram.profile.ui.phone.PhoneHomeScreen
import com.fakegram.profile.ui.phone.PhoneRoutes
import com.fakegram.profile.ui.phone.PhotosScreen
import com.fakegram.profile.ui.phone.SettingsScreen
import com.fakegram.profile.ui.pinterest.PinterestHost
import com.fakegram.profile.ui.post.PostViewerScreen
import com.fakegram.profile.ui.profile.ProfileScreen
import com.fakegram.profile.ui.youtube.YoutubeListScreen
import com.fakegram.profile.ui.youtube.YoutubeWatchScreen
import kotlin.math.roundToInt

private const val ROUTE_PROFILE = "profile/{profileId}"
private const val ROUTE_PANEL = "panel/{panelId}"
private const val ROUTE_POST = "post/{profileId}/{postId}"
private const val ROUTE_CHAT = "bubble/{chatId}"
private const val ROUTE_YT_VIDEO = "video/{videoId}"

/** The phone has no characters or profiles: one home screen, one set of apps, everything searchable from any of them. */
@Composable
fun StoryframeNavHost(repository: ProfileRepository) {
    val navController: NavHostController = rememberNavController()
    var editing by rememberSaveable { mutableStateOf(true) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val onHome = currentRoute == null || currentRoute == PhoneRoutes.HOME

    fun goHome() {
        navController.popBackStack(PhoneRoutes.HOME, inclusive = false)
    }

    CompositionLocalProvider(LocalEditMode provides editing) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = PhoneRoutes.HOME,
                // Apps grow in from the home screen and shrink back out, a bit like a real phone.
                enterTransition = { fadeIn(tween(220)) + scaleIn(tween(300), initialScale = 0.94f) },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(260), targetScale = 0.94f) }
            ) {
                composable(PhoneRoutes.HOME) {
                    PhoneHomeScreen(
                        editing = editing, onEditingChange = { editing = it },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(PhoneRoutes.SETTINGS) {
                    SettingsScreen(editing = editing, onEditingChange = { editing = it }, onBack = { navController.popBackStack() })
                }
                composable(PhoneRoutes.MESSAGES) { MessagesScreen(characterId = 0L, onBack = { navController.popBackStack() }) }
                composable(PhoneRoutes.PHOTOS) { PhotosScreen(characterId = 0L, onBack = { navController.popBackStack() }) }
                composable(PhoneRoutes.NOTES) { NotesScreen(characterId = 0L, onBack = { navController.popBackStack() }) }

                composable(PhoneRoutes.INSTAGRAM) {
                    HomeScreen(
                        repository = repository,
                        onOpenProfile = { id -> navController.navigate("profile/$id") },
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

                composable(PhoneRoutes.GOOGLE) {
                    GoogleHomeScreen(
                        repository = repository,
                        onOpenPanel = { id -> navController.navigate("panel/$id") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    ROUTE_PANEL,
                    arguments = listOf(navArgument("panelId") { type = NavType.LongType })
                ) { backStackEntry ->
                    PanelScreen(panelId = backStackEntry.arguments?.getLong("panelId") ?: 0L, onBack = { navController.popBackStack() })
                }

                composable(PhoneRoutes.BUBBLE) {
                    BubbleHomeScreen(
                        repository = repository,
                        onOpenChat = { id -> navController.navigate("bubble/$id") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    ROUTE_CHAT,
                    arguments = listOf(navArgument("chatId") { type = NavType.LongType })
                ) { backStackEntry ->
                    BubbleChatScreen(chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L, onBack = { navController.popBackStack() })
                }

                composable(PhoneRoutes.YOUTUBE) {
                    YoutubeListScreen(
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

                composable(PhoneRoutes.PINTEREST) { PinterestHost(onBack = { navController.popBackStack() }) }
            }

            if (!onHome) {
                ModeButton(editing = editing, onToggle = { editing = !editing })
                HomeBar(onHome = { goHome() })
            }
        }
    }
}

/** A small round button, always on top and draggable out of the way: tap it to switch between Edit and View. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.ModeButton(editing: Boolean, onToggle: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    Box(
        Modifier.align(Alignment.CenterEnd)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .alpha(0.85f)
            .size(40.dp)
            .clip(CircleShape)
            .background(if (editing) Color(0xFF7A6CF0) else Color(0xCC2B2B2E))
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggle() }) }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    offsetX += drag.x
                    offsetY += drag.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (editing) Icons.Filled.Edit else Icons.Filled.Visibility,
            contentDescription = if (editing) "Edit mode (tap for View)" else "View mode (tap for Edit)",
            tint = Color.White, modifier = Modifier.size(20.dp)
        )
    }
}

/** The home bar at the bottom of a real phone: swipe it up (or tap it) to go back to the home screen. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.HomeBar(onHome: () -> Unit) {
    Box(
        Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(12.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onHome() }) }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = { if (total < -40f) onHome() }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier.width(110.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.Gray.copy(alpha = 0.55f))
        )
    }
}
