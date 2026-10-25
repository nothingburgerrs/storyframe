package com.fakegram.profile.ui.bubble

import com.fakegram.profile.ui.theme.inAppFont
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas as DrawCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.fakegram.profile.ui.phone.LocalEditMode
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.AccountEntity
import com.fakegram.profile.data.BubbleChatEntity
import com.fakegram.profile.data.BubbleMessageEntity
import com.fakegram.profile.data.BubbleMessageKind
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.accounts.AccountPickerDialog
import com.fakegram.profile.ui.profile.captureWindowRegion
import com.fakegram.profile.ui.profile.findActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One Bubble chat. The area under the app bar is what gets screenshotted: Bubble's own top bar, the
 * conversation and Bubble's input bar. The composer below it belongs to Storyframe and is not captured.
 */
@Composable
fun BubbleChatScreen(chatId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as StoryframeApp
    val viewModel: BubbleChatViewModel = viewModel(factory = BubbleChatViewModel.factory(app, app.repository, chatId))
    val state by viewModel.state.collectAsState()
    val chat = state.chat
    val edit = LocalEditMode.current
    val messages = remember(state.messages) { state.messages.sortedBy { it.position } }
    val accounts by app.repository.accounts.collectAsState(initial = emptyList())

    var menuExpanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDivider by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BubbleMessageEntity?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    // What is typed straight into Bubble's own input bar, and who it is sent as.
    var draft by remember { mutableStateOf("") }
    var translationDraft by remember { mutableStateOf("") }
    var asArtist by remember { mutableStateOf(true) }

    var captureBounds by remember { mutableStateOf<android.graphics.Rect?>(null) }
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    // Animation mode: the app chrome hides and the messages "arrive" one by one, ready to screen-record.
    var playing by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableFloatStateOf(1f) }
    var playCount by remember { mutableIntStateOf(0) }
    var showPlayDialog by remember { mutableStateOf(false) }
    val shown = if (playing) messages.take(playCount) else messages
    BackHandler(enabled = playing) { playing = false }
    LaunchedEffect(playing) {
        if (playing) {
            playCount = 0
            delay((700 / playSpeed).toLong())
            for (m in messages) {
                val wait = when (m.kind) {
                    BubbleMessageKind.DATE -> 450L
                    BubbleMessageKind.IMAGE -> 1300L
                    else -> (650L + m.text.length * 40L + m.translation.length * 15L).coerceAtMost(2800L)
                }
                delay((wait / playSpeed).toLong())
                playCount++
            }
        }
    }
    val density = LocalDensity.current
    var topBarPx by remember { mutableIntStateOf(0) }
    var inputBarPx by remember { mutableIntStateOf(0) }
    var viewportPx by remember { mutableIntStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    // Follow the conversation: jump to the newest message whenever one is added.
    LaunchedEffect(shown.size) {
        withFrameNanos { }
        withFrameNanos { }
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    fun saveScreenshot() {
        scope.launch {
            val bounds = captureBounds
            val bitmap = if (activity != null && bounds != null) captureWindowRegion(activity, bounds) else null
            val ok = bitmap != null &&
                MediaStorage.saveBitmapToGallery(context, bitmap, "storyframe_bubble_${System.currentTimeMillis()}.jpg")
            MediaStorage.toast(context, if (ok) "Saved to gallery" else "Failed to save screenshot")
        }
    }

    /**
     * Scrolls through the whole conversation, grabs the screen at each step and stitches the slices
     * together. [paged] = false saves one tall image; true saves phone-shaped screenshots (Bubble's
     * top bar and input bar on every one) so a long chat comes out as a few normal-looking screens.
     */
    fun saveStitched(paged: Boolean) {
        scope.launch {
            val bounds = captureBounds
            if (activity == null || bounds == null || viewportPx <= 0) {
                MediaStorage.toast(context, "Failed to save screenshot")
                return@launch
            }
            val before = scrollState.value
            val shots = mutableListOf<Pair<Bitmap, Int>>()
            val outputs = mutableListOf<Bitmap>()
            var content: Bitmap? = null
            try {
                isExporting = true // custom wallpapers switch to one image stretched over the whole chat
                repeat(4) { withFrameNanos { } }
                val maxV = scrollState.maxValue
                var y = 0
                while (true) {
                    scrollState.scrollTo(y)
                    repeat(3) { withFrameNanos { } }
                    val shot = captureWindowRegion(activity, bounds) ?: error("capture failed")
                    shots += shot to scrollState.value
                    if (scrollState.value >= maxV) break
                    y += viewportPx
                }
                val w = shots.first().first.width
                val total = maxV + viewportPx
                val tall = Bitmap.createBitmap(w, total, Bitmap.Config.ARGB_8888)
                content = tall
                val contentCanvas = Canvas(tall)
                for ((shot, actual) in shots) {
                    contentCanvas.drawBitmap(shot, Rect(0, topBarPx, w, topBarPx + viewportPx), Rect(0, actual, w, actual + viewportPx), null)
                }
                val topBar = Bitmap.createBitmap(shots.first().first, 0, 0, w, topBarPx)
                val lastShot = shots.last().first
                val inputBar = Bitmap.createBitmap(lastShot, 0, lastShot.height - inputBarPx, w, inputBarPx)

                fun page(start: Int, height: Int): Bitmap {
                    val out = Bitmap.createBitmap(w, topBarPx + height + inputBarPx, Bitmap.Config.ARGB_8888)
                    val c = Canvas(out)
                    c.drawBitmap(topBar, 0f, 0f, null)
                    c.drawBitmap(tall, Rect(0, start, w, start + height), Rect(0, topBarPx, w, topBarPx + height), null)
                    c.drawBitmap(inputBar, 0f, (topBarPx + height).toFloat(), null)
                    return out
                }
                if (!paged) {
                    outputs += page(0, total)
                } else {
                    // Same shape as a real phone screenshot (about 1 : 1.95).
                    val slice = (w * 1.95f).toInt() - topBarPx - inputBarPx
                    if (total <= slice) {
                        outputs += page(0, total)
                    } else {
                        val starts = mutableListOf<Int>()
                        var st = 0
                        while (st + slice < total) { starts += st; st += slice }
                        starts += total - slice // the last one always ends on the newest messages
                        starts.forEach { outputs += page(it, slice) }
                    }
                }
                topBar.recycle(); inputBar.recycle()
                val stamp = System.currentTimeMillis()
                var ok = true
                outputs.forEachIndexed { i, bmp ->
                    ok = ok && MediaStorage.saveBitmapToGallery(context, bmp, "storyframe_bubble_${stamp}_${i + 1}.jpg")
                }
                MediaStorage.toast(
                    context,
                    if (!ok) "Failed to save screenshot" else if (outputs.size == 1) "Saved screenshot" else "Saved ${outputs.size} screenshots"
                )
            } catch (t: Throwable) {
                MediaStorage.toast(context, "Failed to save screenshot")
            } finally {
                shots.forEach { it.first.recycle() }
                outputs.forEach { it.recycle() }
                content?.recycle()
                scrollState.scrollTo(before)
                isExporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            if (!playing) TopAppBar(
                title = { Text(chat?.title?.ifBlank { "Chat" } ?: "Chat", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (edit) {
                            DropdownMenuItem(text = { Text("Chat settings") }, onClick = { menuExpanded = false; showSettings = true })
                            DropdownMenuItem(text = { Text("Add date divider") }, onClick = { menuExpanded = false; showDivider = true })
                        }
                        DropdownMenuItem(text = { Text("Save screenshot") }, onClick = { menuExpanded = false; saveScreenshot() })
                        DropdownMenuItem(text = { Text("Save phone-size screenshots") }, onClick = { menuExpanded = false; saveStitched(paged = true) })
                        DropdownMenuItem(text = { Text("Save one long image") }, onClick = { menuExpanded = false; saveStitched(paged = false) })
                        DropdownMenuItem(text = { Text("Play animation") }, onClick = { menuExpanded = false; showPlayDialog = true })
                        if (edit) DropdownMenuItem(text = { Text("Clear chat") }, onClick = { menuExpanded = false; confirmClear = true })
                    }
                }
            )
        }
    ) { padding ->
        if (chat == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Loading…") }
            return@Scaffold
        }
        val theme = BubbleThemes.byId(chat.theme)
        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { c ->
                        val b = c.boundsInWindow()
                        captureBounds = android.graphics.Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                    }
                    .background(theme.background)
            ) {
                Box(Modifier.onSizeChanged { topBarPx = it.height }) { BubbleTopBar(theme) }
                val wallpaper = rememberAsyncImagePainter(model = chat.backgroundUri)
                Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { viewportPx = it.height }) {
                    // Normally the wallpaper stays put while messages scroll over it. While exporting a
                    // long screenshot it is instead painted onto the (tall) content so it spans the whole image.
                    if (chat.backgroundUri != null && !isExporting) {
                        Image(wallpaper, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        var content = Modifier.fillMaxWidth().heightIn(min = with(density) { viewportPx.toDp() })
                        if (chat.backgroundUri != null && isExporting) content = content.paint(wallpaper, contentScale = ContentScale.Crop)
                        Column(
                            modifier = content.padding(vertical = 14.dp),
                            // Bottom-anchored like a real chat: new messages appear next to the input bar
                            // and push the older ones upward.
                            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Bottom)
                        ) {
                            shown.forEach { m ->
                                key(m.id) {
                                    if (playing) PopIn { MessageRow(m, chat, theme) {} }
                                    else MessageRow(m, chat, theme) { if (edit) editing = m }
                                }
                            }
                        }
                    }
                }
                Box(Modifier.onSizeChanged { inputBarPx = it.height }) {
                    BubbleInputBar(theme, draft, { draft = it }) {
                        if (edit && draft.isNotBlank()) {
                            viewModel.sendText(asArtist, draft.trim(), translationDraft.trim())
                            draft = ""; translationDraft = ""
                        }
                    }
                }
            }
            if (!playing && edit) Composer(viewModel, chat, asArtist, { asArtist = it }, translationDraft, { translationDraft = it })
        }
    }

    if (showSettings && chat != null) {
        ChatSettingsDialog(
            chat = chat,
            accounts = accounts,
            onDismiss = { showSettings = false },
            onSave = { transform, avatar, background -> viewModel.saveSettings(transform, avatar, background); showSettings = false }
        )
    }
    if (showPlayDialog) {
        AlertDialog(
            onDismissRequest = { showPlayDialog = false },
            title = { Text("Play animation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("The chat plays from the start, one message at a time - screen-record it. Press back to stop.", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Slow" to 0.6f, "Normal" to 1f, "Fast" to 1.7f).forEach { (label, v) ->
                            FilterChip(selected = playSpeed == v, onClick = { playSpeed = v }, label = { Text(label) })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPlayDialog = false; playing = true }) { Text("Play") } },
            dismissButton = { TextButton(onClick = { showPlayDialog = false }) { Text("Cancel") } }
        )
    }
    if (showDivider) {
        val default = remember { SimpleDateFormat("yyyy.MM.dd (EEE)", Locale.ENGLISH).format(Date()) }
        var text by remember { mutableStateOf(default) }
        AlertDialog(
            onDismissRequest = { showDivider = false },
            title = { Text("Date divider") },
            text = { OutlinedTextField(text, { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { viewModel.addDivider(text); showDivider = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showDivider = false }) { Text("Cancel") } }
        )
    }
    editing?.let { m ->
        MessageEditDialog(
            message = m,
            onDismiss = { editing = null },
            onSave = { viewModel.updateMessage(it); editing = null },
            onDelete = { viewModel.deleteMessage(m.id); editing = null },
            onMove = { delta -> viewModel.moveMessage(m, delta) }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear chat?") },
            text = { Text("All messages in this chat will be removed.") },
            confirmButton = { TextButton(onClick = { viewModel.clearChat(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

// ------------------------------------------------------------------------------------------
// Bubble's own chrome
// ------------------------------------------------------------------------------------------

@Composable
private fun BubbleTopBar(theme: BubbleTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().background(theme.topBar).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.topBarIcon, modifier = Modifier.size(22.dp))
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Search, null, tint = theme.topBarIcon, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(22.dp))
        Icon(Icons.Filled.MoreVert, null, tint = theme.topBarIcon, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun BubbleInputBar(theme: BubbleTheme, text: String, onText: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(theme.inputBar).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .border(1.dp, theme.divider, RoundedCornerShape(50))
                .background(theme.inputField, RoundedCornerShape(50))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = text, onValueChange = onText, maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(color = theme.topBarIcon, fontSize = 15.sp).inAppFont(),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.topBarIcon),
                modifier = Modifier.weight(1f).padding(vertical = 10.dp)
            )
            Icon(Icons.Outlined.SentimentSatisfiedAlt, null, tint = theme.topBarIcon, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.AutoMirrored.Outlined.Send, "Send", tint = if (text.isNotBlank()) theme.topBarIcon else theme.time,
            modifier = Modifier.size(30.dp).clickable(onClick = onSend)
        )
    }
}

// ------------------------------------------------------------------------------------------
// Messages
// ------------------------------------------------------------------------------------------

@Composable
private fun MessageRow(m: BubbleMessageEntity, chat: BubbleChatEntity, theme: BubbleTheme, onClick: () -> Unit) {
    when {
        m.kind == BubbleMessageKind.DATE -> {
            Box(Modifier.fillMaxWidth().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
                Text(
                    m.text, color = theme.dateText, fontSize = 12.sp,
                    modifier = Modifier.background(theme.dateChip, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }
        }
        m.fromArtist -> ArtistMessage(m, chat, theme, onClick)
        else -> FanMessage(m, theme, onClick)
    }
}

@Composable
private fun ArtistMessage(m: BubbleMessageEntity, chat: BubbleChatEntity, theme: BubbleTheme, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp), verticalAlignment = Alignment.Top) {
        ArtistAvatar(chat.artistAvatarUri, theme, 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.badgeText.isNotBlank()) {
                    ArtistBadge(chat.badgeText, theme)
                    Spacer(Modifier.width(6.dp))
                }
                Text(chat.artistName, color = theme.name, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                // The time is measured first, so the bubble only takes the width that is left.
                Box(Modifier.weight(1f, fill = false)) { MessageBubble(m, theme, theme.bubble, theme.text, onClick) }
                Spacer(Modifier.width(8.dp))
                Text(m.timeText, color = theme.time, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun FanMessage(m: BubbleMessageEntity, theme: BubbleTheme, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(m.timeText, color = theme.time, fontSize = 12.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(end = 8.dp, bottom = 2.dp))
        Box(Modifier.weight(1f, fill = false)) {
            MessageBubble(m, theme, theme.meBubble, theme.meText, onClick, translationColor = theme.meText.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun MessageBubble(
    m: BubbleMessageEntity,
    theme: BubbleTheme,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    translationColor: Color = theme.translation
) {
    val shape = RoundedCornerShape(20.dp)
    if (m.kind == BubbleMessageKind.IMAGE) {
        AsyncImage(
            model = m.imageUri,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.width(250.dp).clip(shape).background(theme.divider).clickable(onClick = onClick)
        )
        return
    }
    Column(
        modifier = Modifier
            .widthIn(max = 270.dp)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(m.text, color = textColor, fontSize = 16.sp, lineHeight = 22.sp)
        if (m.translation.isNotBlank()) {
            DashedDivider(theme.divider.takeIf { background == theme.bubble } ?: textColor.copy(alpha = 0.35f))
            Text(m.translation, color = translationColor, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun DashedDivider(color: Color) {
    DrawCanvas(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp)) {
        drawLine(
            color = color, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1.5f * density,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density))
        )
    }
}

@Composable
private fun ArtistBadge(text: String, theme: BubbleTheme) {
    Text(
        text = text.uppercase(),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Brush.horizontalGradient(listOf(theme.badgeStart, theme.badgeEnd)), RoundedCornerShape(theme.badgeRadius.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun ArtistAvatar(uri: String?, theme: BubbleTheme, size: Dp) {
    // Thin coloured ring, a hairline gap, then the photo - like Bubble's artist avatars.
    Box(
        modifier = Modifier.size(size).border(1.5.dp, theme.ring, CircleShape).padding(3.dp).clip(CircleShape).background(theme.divider),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Filled.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

// ------------------------------------------------------------------------------------------
// Composer (Storyframe's own controls, outside the screenshot)
// ------------------------------------------------------------------------------------------

@Composable
private fun Composer(
    viewModel: BubbleChatViewModel, chat: BubbleChatEntity,
    asArtist: Boolean, onAsArtist: (Boolean) -> Unit, translation: String, onTranslation: (String) -> Unit
) {
    var showTranslation by remember { mutableStateOf(translation.isNotBlank()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.sendImage(asArtist, uri)
    }

    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Send as", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(selected = asArtist, onClick = { onAsArtist(true) }, label = { Text(chat.artistName.ifBlank { "Artist" }, maxLines = 1) })
                FilterChip(selected = !asArtist, onClick = { onAsArtist(false) }, label = { Text("Me") })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showTranslation = !showTranslation; if (!showTranslation) onTranslation("") }) {
                    Icon(
                        Icons.Filled.Translate, "Translation line",
                        tint = if (showTranslation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.Image, "Send photo")
                }
            }
            if (showTranslation) {
                OutlinedTextField(
                    value = translation, onValueChange = onTranslation, singleLine = true,
                    placeholder = { Text("Translation for the next message") }, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Dialogs
// ------------------------------------------------------------------------------------------

@Composable
private fun MessageEditDialog(
    message: BubbleMessageEntity,
    onDismiss: () -> Unit,
    onSave: (BubbleMessageEntity) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit
) {
    var text by remember(message.id) { mutableStateOf(message.text) }
    var translation by remember(message.id) { mutableStateOf(message.translation) }
    var time by remember(message.id) { mutableStateOf(message.timeText) }
    var fromArtist by remember(message.id) { mutableStateOf(message.fromArtist) }
    val isText = message.kind == BubbleMessageKind.TEXT
    val isDate = message.kind == BubbleMessageKind.DATE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDate) "Date divider" else "Edit message") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isText || isDate) {
                    OutlinedTextField(text, { text = it }, label = { Text(if (isDate) "Text" else "Message") }, minLines = if (isDate) 1 else 2, modifier = Modifier.fillMaxWidth())
                }
                if (isText) {
                    OutlinedTextField(translation, { translation = it }, label = { Text("Translation (optional)") }, minLines = 1, modifier = Modifier.fillMaxWidth())
                }
                if (!isDate) {
                    OutlinedTextField(time, { time = it }, label = { Text("Time (19:17 or 02:19 AM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("From the artist", modifier = Modifier.weight(1f)); Switch(fromArtist, { fromArtist = it })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onMove(-1) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ArrowUpward, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Up")
                    }
                    OutlinedButton(onClick = { onMove(1) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ArrowDownward, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Down")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(message.copy(text = text, translation = translation, timeText = time, fromArtist = fromArtist))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun ChatSettingsDialog(
    chat: BubbleChatEntity,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: ((BubbleChatEntity) -> BubbleChatEntity, Uri?, Uri?) -> Unit
) {
    var title by remember(chat.id) { mutableStateOf(chat.title) }
    var artistName by remember(chat.id) { mutableStateOf(chat.artistName) }
    var badge by remember(chat.id) { mutableStateOf(chat.badgeText) }
    var themeId by remember(chat.id) { mutableStateOf(chat.theme) }
    var use24h by remember(chat.id) { mutableStateOf(chat.use24h) }
    var avatarPath by remember(chat.id) { mutableStateOf(chat.artistAvatarUri) }
    var pickedAvatar by remember(chat.id) { mutableStateOf<Uri?>(null) }
    var backgroundPath by remember(chat.id) { mutableStateOf(chat.backgroundUri) }
    var pickedBackground by remember(chat.id) { mutableStateOf<Uri?>(null) }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) pickedBackground = uri }
    var showPicker by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) pickedAvatar = uri }

    if (showPicker) {
        AccountPickerDialog(
            accounts = accounts,
            onDismiss = { showPicker = false },
            onPick = { a -> artistName = a.username; avatarPath = a.avatarUri; pickedAvatar = null; showPicker = false }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat settings") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Color(0x33808080))
                            .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        val shown = pickedAvatar?.toString() ?: avatarPath
                        if (shown != null) AsyncImage(shown, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Icons.Filled.Person, null)
                    }
                    Column {
                        Text("Artist avatar", fontSize = 13.sp)
                        TextButton(onClick = { showPicker = true }) { Text("Use saved account") }
                    }
                }
                OutlinedTextField(artistName, { artistName = it }, label = { Text("Artist name (shown in chat)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(badge, { badge = it }, label = { Text("Tag next to the name (empty = none)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(title, { title = it }, label = { Text("Chat name (only in your list)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(width = 44.dp, height = 64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x33808080))
                            .clickable { backgroundLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        val shown = pickedBackground?.toString() ?: backgroundPath
                        if (shown != null) AsyncImage(shown, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Icons.Filled.Image, null)
                    }
                    Column {
                        Text("Custom background", fontSize = 13.sp)
                        if (pickedBackground != null || backgroundPath != null) {
                            TextButton(onClick = { pickedBackground = null; backgroundPath = null }) { Text("Remove") }
                        } else {
                            Text("Tap to pick an image", fontSize = 11.sp)
                        }
                    }
                }
                Text("Style", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScrollCompat()) {
                    BubbleThemes.all.forEach { t ->
                        FilterChip(selected = themeId == t.id, onClick = { themeId = t.id }, label = { Text(t.label, maxLines = 1) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("24-hour time (19:17)", modifier = Modifier.weight(1f)); Switch(use24h, { use24h = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave({ c -> c.copy(title = title, artistName = artistName, badgeText = badge, theme = themeId, use24h = use24h, artistAvatarUri = avatarPath, backgroundUri = backgroundPath) }, pickedAvatar, pickedBackground)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier = this.then(Modifier.horizontalScroll(rememberScrollState()))


/** A message rises in from below the chat and pushes the earlier messages upward. */
@Composable
private fun PopIn(content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(200)) +
            slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } +
            expandVertically(tween(320, easing = FastOutSlowInEasing), expandFrom = Alignment.Bottom)
    ) { content() }
}
