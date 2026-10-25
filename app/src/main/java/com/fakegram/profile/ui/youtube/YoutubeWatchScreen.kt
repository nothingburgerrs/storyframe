package com.fakegram.profile.ui.youtube

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.fakegram.profile.ui.theme.inAppFont
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.key
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.R
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.YoutubeCommentEntity
import com.fakegram.profile.data.YoutubeVideoEntity
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.rememberCropPicker
import com.fakegram.profile.ui.phone.LocalEditMode

val YtBg = Color(0xFF0F0F0F)
val YtText = Color(0xFFFFFFFF)
val YtSub = Color(0xFFAAAAAA)
private val YtCard = Color(0xFF212121)
private val YtChip = Color(0xFF272727)
private val YtRed = Color(0xFFFF0000)
private const val DOT = " • "

/** A glyph cropped from the real YouTube app, tinted to the current colour. */
@Composable
private fun YtIcon(res: Int, size: Dp, tint: Color = YtText, modifier: Modifier = Modifier) {
    Icon(painterResource(res), null, tint = tint, modifier = modifier.size(size))
}

/** A circular photo. When [onPick] is set (and Edit mode is on), tapping it opens the photo picker and a circular crop. */
@Composable
fun YtAvatar(uri: String?, size: Dp, onPick: ((String) -> Unit)? = null) {
    val pick = rememberCropPicker(CropShape.CIRCLE) { onPick?.invoke(it) }
    val edit = LocalEditMode.current
    Box(
        Modifier.size(size).clip(CircleShape).background(Color(0xFF3A3A3A))
            .then(if (onPick != null && edit) Modifier.clickable(onClick = pick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Outlined.Person, null, tint = YtSub, modifier = Modifier.size(size * 0.6f))
    }
}

/** The YouTube watch page. In Edit mode tap the picture, the title or a comment to open its editor. */
@Composable
fun YoutubeWatchScreen(videoId: Long, onBack: () -> Unit, onOpenVideo: (Long) -> Unit = {}) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val vm: YoutubeWatchViewModel = viewModel(key = "ytWatch$videoId", factory = YoutubeWatchViewModel.factory(app.repository, videoId))
    val video by vm.video.collectAsState()
    val comments by vm.comments.collectAsState()
    val edit = LocalEditMode.current
    var showComments by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var editVideo by remember { mutableStateOf(false) }
    var editCommentId by remember { mutableStateOf<Long?>(null) }
    val v = video

    // Back closes the comments first, like the real app.
    BackHandler(enabled = showComments) { if (expanded) expanded = false else showComments = false }

    Box(Modifier.fillMaxSize().background(YtBg)) {
        if (v == null) {
            Text("Video not found", color = YtSub, modifier = Modifier.align(Alignment.Center))
        } else {
            val openEditor = { if (edit) editVideo = true }
            Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                // The player stays on top while comments are open; it only folds away when the panel is pulled all the way up.
                AnimatedVisibility(visible = !(showComments && expanded)) {
                    Player(v, onBack, vm::update, onEdit = openEditor)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 60.dp)) {
                        Column(Modifier.padding(horizontal = 14.dp)) {
                            Spacer(Modifier.height(14.dp))
                            if (v.showPostInfo) {
                                Column(Modifier.fillMaxWidth().clickable(enabled = edit) { editVideo = true }) {
                                    Text(
                                        v.title.ifBlank { "Video title" }, color = YtText, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    MetaLine(v)
                                }
                                Spacer(Modifier.height(16.dp))
                                ActionRow(v, vm::update)
                                Spacer(Modifier.height(16.dp))
                            }
                            CommentsCard(comments, v.commentCount, onClick = { showComments = true })
                        }
                        if (v.showAd) {
                            Spacer(Modifier.height(14.dp))
                            AdBlock(v, onTap = openEditor)
                        }
                        if (v.showRelated) RelatedVideos(v, onOpenVideo)
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showComments,
                        enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
                        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160))
                    ) {
                        CommentsPanel(
                            comments, v.channelName, expanded, { expanded = it },
                            onClose = { showComments = false; expanded = false },
                            onNew = vm::saveComment, onEdit = { editCommentId = it }, modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (v != null && editVideo) {
            VideoEditSheet(
                video = v, update = { t -> vm.update(t) },
                onDelete = { vm.deleteVideo(); editVideo = false; onBack() },
                onDismiss = { editVideo = false }
            )
        }
        editCommentId?.let { id ->
            comments.firstOrNull { it.id == id }?.let { c ->
                CommentEditSheet(
                    comment = c, update = { t -> vm.updateComment(id, t) },
                    onDelete = { vm.deleteComment(id); editCommentId = null },
                    onDismiss = { editCommentId = null }
                )
            }
        }
    }
}

/** Other videos under the comments. In Edit mode + adds another and tapping a video's text edits it. */
@Composable
private fun RelatedVideos(current: YoutubeVideoEntity, onOpenVideo: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val edit = LocalEditMode.current
    val vm: YoutubeListViewModel = viewModel(key = "ytRelated0", factory = YoutubeListViewModel.factory(app.repository, 0L))
    val all by vm.videos.collectAsState()
    var editId by remember { mutableStateOf<Long?>(null) }
    val others = all.filter { it.id != current.id }
    Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        others.forEach { o -> key(o.id) { FeedCard(o, onOpen = { onOpenVideo(o.id) }, onEdit = { editId = o.id }) } }
        if (edit) Row(
            Modifier.fillMaxWidth().clickable { vm.create("", null) { id -> editId = id } }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, null, tint = YtText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Add a recommended video", color = YtText, fontSize = 14.sp)
        }
    }
    editId?.let { id ->
        all.firstOrNull { it.id == id }?.let { vid ->
            VideoEditSheet(
                video = vid, update = { t -> vm.update(id, t) },
                onDelete = { vm.delete(id); editId = null }, onDismiss = { editId = null }
            )
        }
    }
}

/** "@channel  183 likes  6.4K views  4w ago ... more" on ONE line: a long line fades out at the right and "more" stays at the edge. */
@Composable
private fun MetaLine(v: YoutubeVideoEntity) {
    val body = TextStyle(color = YtSub, fontSize = 14.sp)
    val summary = listOfNotNull(
        v.channelName.trim().trimStart('@').takeIf { it.isNotEmpty() }?.let { "@$it" },
        v.likeCount.trim().takeIf { it.isNotEmpty() }?.let { "$it likes" },
        v.viewsText.trim().takeIf { it.isNotEmpty() },
        v.timeText.trim().takeIf { it.isNotEmpty() }
    ).joinToString("  ")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
            Text(summary, style = body, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f, fill = false).fadeRight())
            if (v.autoDubbed) {
                Spacer(Modifier.width(8.dp))
                YtIcon(R.drawable.yt_dubbed, 18.dp, YtSub)
                Spacer(Modifier.width(2.dp))
                Text("Auto-dubbed", style = body.copy(color = YtText, fontWeight = FontWeight.Medium), maxLines = 1)
            }
        }
        Text("  more", style = body.copy(color = YtText, fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

/** Fades the last 40dp of a composable to transparent, the way YouTube fades a long title. */
private fun Modifier.fadeRight(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = 40.dp.toPx()
        drawRect(
            Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - w, endX = size.width),
            blendMode = BlendMode.DstIn
        )
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Player(v: YoutubeVideoEntity, onBack: () -> Unit, update: ((YoutubeVideoEntity) -> YoutubeVideoEntity) -> Unit, onEdit: () -> Unit) {
    val edit = LocalEditMode.current
    val small = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    // While you drag the bar the time follows your finger; it is saved when you let go.
    var live by remember(v.id) { mutableStateOf<Float?>(null) }
    val shown = live ?: v.progress
    val totalText = if ("/" in v.durationText) v.durationText.substringAfter("/").trim() else ""
    val totalSeconds = parseSeconds(totalText)

    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClick = { if (edit) onEdit() }, onLongClick = { if (edit) onEdit() }
            )
    ) {
        if (v.thumbnailUri != null) AsyncImage(v.thumbnailUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (edit) Text("Tap to add a picture", color = YtSub, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))

        if (v.showControls) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Color(0x66000000)))))

            Box(Modifier.align(Alignment.TopStart).padding(4.dp).size(36.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                YtIcon(R.drawable.yt_chevron, 18.dp, Color.White)
            }
            Row(Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 42.dp, height = 24.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x99666666)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Pause, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Filled.Cast, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Outlined.ClosedCaption, "Captions", tint = Color.White, modifier = Modifier.size(21.dp).alpha(if (v.showCaption) 1f else 0.45f))
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerBubble(Icons.Filled.SkipPrevious, 34.dp)
                PlayerBubble(Icons.Filled.PlayArrow, 46.dp)
                PlayerBubble(Icons.Filled.SkipNext, 34.dp)
            }
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 26.dp).background(Color(0x99000000), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatSeconds((shown * totalSeconds).toInt()), style = small)
                Text(" / ", style = small)
                Text(totalText.ifEmpty { "0:00" }, style = small)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 26.dp).size(28.dp).clip(CircleShape).background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Fullscreen, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
        }
        if (v.showCaption && v.subtitleText.isNotBlank()) Text(
            v.subtitleText, style = small.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (v.showControls) 50.dp else 18.dp).background(Color(0xCC000000), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
        )
        // Always there, like in the app: a thin progress line along the bottom edge.
        SeekBar(
            fraction = shown, showThumb = v.showControls, modifier = Modifier.align(Alignment.BottomStart),
            onLive = { live = it },
            onCommit = { f ->
                live = null
                update { cur ->
                    val total = if ("/" in cur.durationText) cur.durationText.substringAfter("/").trim() else ""
                    cur.copy(progress = f, durationText = "${formatSeconds((f * parseSeconds(total)).toInt())} / $total")
                }
            }
        )
    }
}

/** The player's progress line. Tap or drag anywhere along the bottom edge to set the time; the touch area is much taller than the line. */
@Composable
private fun SeekBar(fraction: Float, showThumb: Boolean, modifier: Modifier, onLive: (Float) -> Unit, onCommit: (Float) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.fillMaxWidth().height(34.dp)
            .pointerInput(Unit) { detectTapGestures { off -> onCommit((off.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = { off -> last = (off.x / size.width).coerceIn(0f, 1f); onLive(last) },
                    onHorizontalDrag = { change, _ -> last = (change.position.x / size.width).coerceIn(0f, 1f); onLive(last) },
                    onDragEnd = { onCommit(last) }
                )
            }
    ) {
        val width = maxWidth
        val f = fraction.coerceIn(0f, 1f)
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x55FFFFFF)))
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(f.coerceAtLeast(0.004f)).height(3.dp).background(if (showThumb) YtRed else Color.White))
        if (showThumb) {
            Box(
                Modifier.align(Alignment.BottomStart).offset(x = width * f - 7.dp, y = 5.dp).size(14.dp).clip(CircleShape).background(YtRed)
            )
        }
    }
}

@Composable
private fun PlayerBubble(icon: ImageVector, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0x66000000)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** Avatar, Subscribe pill, then the action icons packed at the right edge, like the real app. */
@Composable
private fun ActionRow(v: YoutubeVideoEntity, update: ((YoutubeVideoEntity) -> YoutubeVideoEntity) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        YtAvatar(v.channelAvatarUri, 34.dp)
        Spacer(Modifier.width(12.dp))
        if (!v.subscribed) {
            Text(
                "Subscribe", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFF1F1F1))
                    .clickable { update { it.copy(subscribed = true) } }.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        } else {
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(YtChip).clickable { update { it.copy(subscribed = false) } }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                YtIcon(R.drawable.yt_bell, 19.dp)
                Spacer(Modifier.width(8.dp))
                YtIcon(R.drawable.yt_chevron, 13.dp)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            YtIcon(R.drawable.yt_like, 24.dp)
            YtIcon(R.drawable.yt_dislike, 24.dp)
            YtIcon(R.drawable.yt_share, 24.dp)
            YtIcon(R.drawable.yt_sparkle, 22.dp)
            YtIcon(R.drawable.yt_more, 20.dp)
        }
    }
}

@Composable
private fun CommentsCard(comments: List<YoutubeCommentEntity>, countText: String, onClick: () -> Unit) {
    val first = comments.firstOrNull()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(YtCard).clickable(onClick = onClick).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comments", color = YtText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            // The number can be anything (e.g. 12K) without writing that many comments; blank = real count.
            Text(countText.ifBlank { comments.size.toString() }, color = YtSub, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(width = 16.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(YtText))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(YtSub))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            YtAvatar(first?.avatarUri, 28.dp)
            Spacer(Modifier.width(12.dp))
            Text(first?.text ?: "No comments yet", color = if (first != null) YtText else YtSub, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AdBlock(v: YoutubeVideoEntity, onTap: () -> Unit) {
    val edit = LocalEditMode.current
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFF26B21)).clickable(enabled = edit, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (v.adImageUri != null) AsyncImage(v.adImageUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (edit) Text("Tap to add the ad picture", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * The comments, shown under the player like YouTube does. Pull the handle up to read them full screen, down to
 * shrink them or close. Type in the bar at the bottom to post; it stays above the keyboard.
 */
@Composable
private fun CommentsPanel(
    comments: List<YoutubeCommentEntity>, defaultUser: String, expanded: Boolean, setExpanded: (Boolean) -> Unit,
    onClose: () -> Unit, onNew: (YoutubeCommentEntity) -> Unit, onEdit: (Long) -> Unit, modifier: Modifier = Modifier
) {
    val edit = LocalEditMode.current
    var newestFirst by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val shown = if (newestFirst) comments.reversed() else comments

    Column(modifier.fillMaxWidth().background(YtBg)) {
        Column(
            Modifier.fillMaxWidth().pointerInput(expanded) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = {
                        if (total < -40f) setExpanded(true)
                        else if (total > 40f) { if (expanded) setExpanded(false) else onClose() }
                    }
                )
            }
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF555555)))
            }
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Comments", color = YtText, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                YtIcon(R.drawable.yt_info, 26.dp)
                Spacer(Modifier.width(22.dp))
                YtIcon(R.drawable.yt_close, 24.dp, modifier = Modifier.clickable(onClick = onClose))
            }
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SortChip("Top", !newestFirst) { newestFirst = false }
                SortChip("Newest", newestFirst) { newestFirst = true }
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.id }) { c -> CommentRow(c, onEdit = { if (edit) onEdit(c.id) }) }
            if (comments.isEmpty()) item { Text("No comments yet.", color = YtSub, modifier = Modifier.padding(20.dp)) }
        }
        if (edit) Row(
            Modifier.fillMaxWidth().background(YtBg).padding(horizontal = 14.dp, vertical = 10.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp)).background(YtChip).padding(start = 20.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draft, onValueChange = { draft = it }, maxLines = 4, cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(color = YtText, fontSize = 16.sp).inAppFont(),
                    modifier = Modifier.weight(1f).padding(vertical = 14.dp).onFocusChanged { if (it.isFocused) setExpanded(true) },
                    decorationBox = { inner -> Box { if (draft.isEmpty()) Text("Add a comment...", color = YtSub, fontSize = 16.sp); inner() } }
                )
                if (draft.isNotBlank()) {
                    IconButton(onClick = {
                        onNew(YoutubeCommentEntity(videoId = 0, username = defaultUser.ifBlank { "user" }, text = draft.trim()))
                        draft = ""
                    }) { Icon(Icons.AutoMirrored.Filled.Send, "Post", tint = YtText) }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label, color = if (selected) Color.Black else YtText, fontSize = 16.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) Color.White else YtChip).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp)
    )
}

/** One comment. In Edit mode tap it to change anything about it. */
@Composable
private fun CommentRow(c: YoutubeCommentEntity, onEdit: () -> Unit) {
    val sub = TextStyle(color = YtSub, fontSize = 13.sp)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        YtAvatar(c.avatarUri, 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("@" + c.username.trimStart('@') + DOT + c.timeText, style = sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(c.text, color = YtText, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth())
            if (c.hasTranslation) Text("Translate to English", color = YtSub, fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                YtIcon(R.drawable.yt_like, 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(c.likeCount.ifBlank { "" }, color = YtText, fontSize = 14.sp, modifier = Modifier.widthIn(min = 18.dp))
                Spacer(Modifier.width(30.dp))
                YtIcon(R.drawable.yt_dislike, 22.dp)
                Spacer(Modifier.width(32.dp))
                YtIcon(R.drawable.yt_comment, 22.dp)
            }
            if (c.replyCount > 0) {
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (c.replyCount == 1) "1 reply" else "${c.replyCount} replies", color = YtText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = YtText, modifier = Modifier.size(20.dp))
                }
            }
        }
        YtIcon(R.drawable.yt_more, 20.dp)
    }
}
