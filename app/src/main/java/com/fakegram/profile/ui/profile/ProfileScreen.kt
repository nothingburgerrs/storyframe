package com.fakegram.profile.ui.profile

import android.Manifest
import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.CarouselItemEntity
import com.fakegram.profile.data.MediaType
import com.fakegram.profile.data.PostEntity
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.components.FollowState
import com.fakegram.profile.ui.components.ProfilePreviewContent
import com.fakegram.profile.ui.components.StatField
import com.fakegram.profile.ui.components.next
import com.fakegram.profile.ui.editor.AddPostSheet
import com.fakegram.profile.ui.editor.EditPostSheet
import com.fakegram.profile.ui.editor.EditorViewModel
import com.fakegram.profile.ui.theme.StoryframeTheme
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private fun android.content.Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Captures exactly the on-screen pixels of [bounds] (in window coordinates) using [PixelCopy].
 * This reads directly from the compositor's rendered surface, so — unlike a GraphicsLayer
 * re-record — it always reflects exactly what the user sees, with no GPU-driver-dependent
 * blank/black-frame risk.
 */
private suspend fun captureWindowRegion(activity: Activity, bounds: Rect): Bitmap? {
    if (bounds.width() <= 0 || bounds.height() <= 0) return null
    val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
    return try {
        suspendCancellableCoroutine { cont ->
            try {
                PixelCopy.request(
                    activity.window,
                    bounds,
                    bitmap,
                    { result ->
                        if (cont.isActive) cont.resume(if (result == PixelCopy.SUCCESS) bitmap else null)
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * The single full-screen route for a profile. It renders the real, full-scale
 * [ProfilePreviewContent] and simultaneously IS the editing surface: every tap target on it
 * opens a small in-place dialog. There is no separate "Editor" route or mini-preview anymore —
 * this screen is also the export source (see [isExporting]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as StoryframeApp
    val viewModel: EditorViewModel = viewModel(
        factory = EditorViewModel.factory(app, app.repository, app.themePreferences, profileId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val profile = uiState.profile

    var menuExpanded by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var statDialogField by remember { mutableStateOf<StatField?>(null) }
    var showHighlightDialog by remember { mutableStateOf(false) }
    var editingHighlight by remember { mutableStateOf<com.fakegram.profile.data.HighlightEntity?>(null) }
    var showFollowedByManager by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAddPostSheet by remember { mutableStateOf(false) }
    var addPostForTagged by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<PostEntity?>(null) }

    // "Preview as visitor" toggle (ephemeral UI-only state; resets when leaving the screen).
    var isVisitorMode by remember { mutableStateOf(false) }
    var followState by remember { mutableStateOf(FollowState.FOLLOW) }

    // read-only info dialogs from the overflow menu (kept from the old Preview screen)
    var showAvatarViewer by remember { mutableStateOf(false) }
    var showFollowedByInfo by remember { mutableStateOf(false) }
    var showHighlightsInfo by remember { mutableStateOf(false) }
    var carouselViewerPost by remember { mutableStateOf<PostEntity?>(null) }
    var carouselItems by remember { mutableStateOf<List<CarouselItemEntity>>(emptyList()) }
    var videoViewerPost by remember { mutableStateOf<PostEntity?>(null) }

    var isExporting by remember { mutableStateOf(false) }
    var captureBounds by remember { mutableStateOf<Rect?>(null) }
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    fun doSaveScreenshot() {
        scope.launch {
            try {
                isExporting = true
                // Wait for a couple of real composed+drawn frames after hiding the add-affordances
                // so PixelCopy reads back the "clean" state.
                withFrameNanos { }
                withFrameNanos { }
                withFrameNanos { }
                val bounds = captureBounds
                val bitmap = if (activity != null && bounds != null) captureWindowRegion(activity, bounds) else null
                if (bitmap != null) {
                    val ok = MediaStorage.saveBitmapToGallery(context, bitmap, "storyframe_${System.currentTimeMillis()}.jpg")
                    MediaStorage.toast(context, if (ok) "Saved to gallery" else "Failed to save")
                } else {
                    MediaStorage.toast(context, "Failed to save screenshot")
                }
            } catch (e: Exception) {
                MediaStorage.toast(context, "Failed to save screenshot")
            } finally {
                isExporting = false
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) doSaveScreenshot() else MediaStorage.toast(context, "Storage permission needed to save")
    }

    fun requestSaveScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            doSaveScreenshot()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) doSaveScreenshot() else storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    StoryframeTheme(darkTheme = isDark) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Followed by") }, onClick = { menuExpanded = false; showFollowedByManager = true })
                            DropdownMenuItem(text = { Text("Highlights") }, onClick = { menuExpanded = false; showHighlightsInfo = true })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { menuExpanded = false; showSettingsSheet = true })
                            DropdownMenuItem(
                                text = { Text(if (isVisitorMode) "Preview as yourself" else "Preview as visitor") },
                                onClick = { menuExpanded = false; isVisitorMode = !isVisitorMode }
                            )
                            DropdownMenuItem(
                                text = { Text("Save screenshot") },
                                onClick = { menuExpanded = false; requestSaveScreenshot() }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .onGloballyPositioned { coordinates ->
                        val b = coordinates.boundsInWindow()
                        captureBounds = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                    }
            ) {
                ProfilePreviewContent(
                    state = uiState,
                    isExporting = isExporting,
                    isVisitorMode = isVisitorMode,
                    followState = followState,
                    onEditProfileClick = { showEditProfileDialog = true },
                    onFollowClick = { followState = followState.next() },
                    onAvatarClick = { showEditProfileDialog = true },
                    onStatClick = { field -> statDialogField = field },
                    onFollowedByClick = { showFollowedByManager = true },
                    onHighlightClick = { id ->
                        editingHighlight = uiState.highlights.find { it.id == id }
                        showHighlightDialog = true
                    },
                    onAddHighlightClick = { editingHighlight = null; showHighlightDialog = true },
                    // Tap opens the edit sheet; long-press-then-drag (handled inside
                    // ProfilePreviewContent) is reserved solely for reordering now.
                    onPostClick = { post -> editingPost = post },
                    onAddPostClick = { tagged -> addPostForTagged = tagged; showAddPostSheet = true },
                    onReorderPosts = { newOrder -> viewModel.reorderPosts(newOrder) }
                )
            }
        }
    }

    // ---- Edit dialogs (on-canvas triggers) ----

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { ringType, displayName, username, bio, newAvatarPath, clearAvatar ->
                showEditProfileDialog = false
                viewModel.saveProfileEdits(ringType, displayName, username, bio, newAvatarPath, clearAvatar)
            }
        )
    }

    statDialogField?.let { field ->
        val (label, current) = when (field) {
            StatField.POSTS -> "Posts" to profile.postCount
            StatField.FOLLOWERS -> "Followers" to profile.followerCount
            StatField.FOLLOWING -> "Following" to profile.followingCount
        }
        SimpleTextDialog(
            title = label,
            initialValue = current,
            onDismiss = { statDialogField = null },
            onConfirm = { value ->
                statDialogField = null
                viewModel.updateProfile {
                    when (field) {
                        StatField.POSTS -> it.copy(postCount = value)
                        StatField.FOLLOWERS -> it.copy(followerCount = value)
                        StatField.FOLLOWING -> it.copy(followingCount = value)
                    }
                }
            }
        )
    }

    if (showHighlightDialog) {
        HighlightEditDialog(
            existing = editingHighlight,
            onDismiss = { showHighlightDialog = false; editingHighlight = null },
            onSave = { title, path ->
                val existing = editingHighlight
                showHighlightDialog = false
                editingHighlight = null
                if (existing == null) viewModel.addHighlight(title, path)
                else viewModel.editHighlight(existing.id, title, path)
            },
            onDelete = {
                editingHighlight?.let { viewModel.deleteHighlight(it.id) }
                showHighlightDialog = false
                editingHighlight = null
            }
        )
    }

    if (showFollowedByManager) {
        FollowedByManagerDialog(
            entries = uiState.followedBy,
            onDismiss = { showFollowedByManager = false },
            onAdd = { username, uri -> viewModel.addFollowedByPending(username, uri) },
            onEditUsername = { id, username -> viewModel.updateFollowedByUsername(id, username) },
            onDelete = { id -> viewModel.deleteFollowedBy(id) },
            onMove = { entry, delta -> viewModel.moveFollowedBy(entry, delta) }
        )
    }

    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }, sheetState = sheetState) {
            SettingsSheetContent(
                profile = profile,
                onToggleVerified = { checked -> viewModel.updateProfile { it.copy(isVerified = checked) } },
                onToggleActiveStory = { checked -> viewModel.updateProfile { it.copy(hasActiveStory = checked) } },
                onToggleShowReels = { checked -> viewModel.updateProfile { it.copy(showReelsTab = checked) } },
                onToggleShowReposts = { checked -> viewModel.updateProfile { it.copy(showRepostsTab = checked) } }
            )
        }
    }

    if (showAddPostSheet) {
        AddPostSheet(viewModel = viewModel, forTagged = addPostForTagged, onDismiss = { showAddPostSheet = false })
    }

    editingPost?.let { post ->
        EditPostSheet(viewModel = viewModel, post = post, onDismiss = { editingPost = null })
    }

    // ---- Overflow read-only dialogs ----

    if (showAvatarViewer) {
        AlertDialog(
            onDismissRequest = { showAvatarViewer = false },
            confirmButton = { TextButton(onClick = { showAvatarViewer = false }) { Text("Close") } },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (profile.avatarUri != null) {
                        AsyncImage(model = profile.avatarUri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("No avatar set")
                    }
                }
            }
        )
    }

    if (showFollowedByInfo) {
        AlertDialog(
            onDismissRequest = { showFollowedByInfo = false },
            title = { Text("Followed by") },
            text = {
                Column {
                    val names = uiState.followedBy.sortedBy { it.position }.map { it.username }
                    if (names.isEmpty()) Text("No accounts listed.") else names.forEach { Text("@$it") }
                }
            },
            confirmButton = { TextButton(onClick = { showFollowedByInfo = false }) { Text("Close") } }
        )
    }

    if (showHighlightsInfo) {
        AlertDialog(
            onDismissRequest = { showHighlightsInfo = false },
            title = { Text("Highlights") },
            text = {
                Column {
                    val titles = uiState.highlights.sortedBy { it.position }.map { it.title }
                    if (titles.isEmpty()) Text("No highlights yet.") else titles.forEach { Text(it) }
                }
            },
            confirmButton = { TextButton(onClick = { showHighlightsInfo = false }) { Text("Close") } }
        )
    }

    carouselViewerPost?.let { post ->
        val uris = remember(carouselItems, post) {
            if (carouselItems.isNotEmpty()) carouselItems.sortedBy { it.position }.map { it.mediaUri }
            else listOfNotNull(post.primaryMediaUri)
        }
        val pagerState = rememberPagerState(pageCount = { uris.size.coerceAtLeast(1) })
        AlertDialog(
            onDismissRequest = { carouselViewerPost = null },
            confirmButton = { TextButton(onClick = { carouselViewerPost = null }) { Text("Close") } },
            text = {
                if (uris.isEmpty()) {
                    Text("No media in this post.")
                } else {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                        AsyncImage(model = uris[page], contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        )
    }

    videoViewerPost?.let { post ->
        AlertDialog(
            onDismissRequest = { videoViewerPost = null },
            confirmButton = { TextButton(onClick = { videoViewerPost = null }) { Text("Close") } },
            title = { Text("Video post (thumbnail)") },
            text = {
                if (post.primaryMediaUri != null) {
                    AsyncImage(model = post.primaryMediaUri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("No thumbnail available.")
                }
            }
        )
    }
}
