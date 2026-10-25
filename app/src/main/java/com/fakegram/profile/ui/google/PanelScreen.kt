package com.fakegram.profile.ui.google

import androidx.compose.foundation.layout.imePadding
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.PanelFactEntity
import com.fakegram.profile.data.PanelItemEntity
import com.fakegram.profile.data.PanelItemKind
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.profile.captureWindowRegion
import com.fakegram.profile.ui.profile.findActivity
import kotlinx.coroutines.launch

/** One screen per panel: Google's results page IS the editor. Tap any text, photo, card, song or member to change it. */
@Composable
fun PanelScreen(panelId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as StoryframeApp
    val viewModel: PanelViewModel = viewModel(factory = PanelViewModel.factory(app, app.repository, panelId))
    val state by viewModel.uiState.collectAsState()
    val isDark by app.themePreferences.isDarkMode.collectAsState(initial = true)

    var previewDark by remember { mutableStateOf(true) }
    // While a screenshot is taken the editing hints (placeholders, + cards) are hidden.
    var exporting by remember { mutableStateOf(false) }
    var captureBounds by remember { mutableStateOf<android.graphics.Rect?>(null) }
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    // Preview follows the app theme until the user flips it with the toggle.
    LaunchedEffect(isDark) { previewDark = isDark }

    fun saveScreenshot() {
        scope.launch {
            exporting = true
            kotlinx.coroutines.delay(200)
            val bounds = captureBounds
            val bitmap: Bitmap? = if (activity != null && bounds != null) captureWindowRegion(activity, bounds) else null
            exporting = false
            val ok = bitmap != null &&
                MediaStorage.saveBitmapToGallery(context, bitmap, "storyframe_google_${System.currentTimeMillis()}.jpg")
            MediaStorage.toast(context, if (ok) "Saved to gallery" else "Failed to save screenshot")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.panel?.title?.ifBlank { "Panel" } ?: "Panel", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButtonLabel(if (previewDark) "Light" else "Dark") { previewDark = !previewDark }
                    IconButton(onClick = { saveScreenshot() }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save screenshot")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (state.panel == null) {
                Text("Loading...", modifier = Modifier.align(Alignment.Center))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { c ->
                            val b: ComposeRect = c.boundsInWindow()
                            captureBounds = android.graphics.Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                        }
                ) {
                    GooglePanelPreview(state = state, isDark = previewDark, edit = if (exporting) null else viewModel)
                }
            }
        }
    }
}

@Composable
private fun TextButtonLabel(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}
