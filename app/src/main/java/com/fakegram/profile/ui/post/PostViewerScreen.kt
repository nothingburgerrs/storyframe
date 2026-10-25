package com.fakegram.profile.ui.post

import com.fakegram.profile.ui.theme.inAppFont
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.fakegram.profile.ui.phone.LocalEditMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.R
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.AccountEntity
import com.fakegram.profile.data.CommentEntity
import com.fakegram.profile.ui.accounts.AccountPickerDialog
import com.fakegram.profile.data.MediaType
import com.fakegram.profile.data.PostEntity
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.profile.captureWindowRegion
import com.fakegram.profile.ui.profile.findActivity
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import com.fakegram.profile.ui.theme.StoryframeColors
import com.fakegram.profile.ui.theme.StoryframeTheme
import kotlinx.coroutines.launch

private val IgRepostGreen = Color(0xFF50AB4E)

/**
 * The opened-post screen: Instagram's "Posts" feed, scrolled to the tapped post. Everything a
 * viewer sees (caption, counts, date, comments) is editable from the app bar menu.
 */
@Composable
fun PostViewerScreen(profileId: Long, postId: Long, onBack: () -> Unit, openDetails: Boolean = false) {
    val context = LocalContext.current
    val app = context.applicationContext as StoryframeApp
    val viewModel: PostViewModel = viewModel(factory = PostViewModel.factory(app, app.repository, profileId))
    val state by viewModel.state.collectAsState()
    val isDark by app.themePreferences.isDarkMode.collectAsState(initial = true)

    // The feed continues with the posts of the same tab the post was opened from.
    val feed = remember(state.posts, postId) {
        val tapped = state.posts.firstOrNull { it.id == postId }
        if (tapped?.isTagged == true) state.posts.filter { it.isTagged }.sortedBy { it.position }
        else state.posts.filter { !it.isTagged }
            .sortedWith(compareByDescending<PostEntity> { it.isPinned }.thenBy { it.position })
    }
    val startIndex = remember(postId, feed.isEmpty()) { feed.indexOfFirst { it.id == postId }.coerceAtLeast(0) }
    val listState = rememberLazyListState()
    // The posts load after the screen appears, so jump to the tapped one once they are there (once only).
    var jumped by remember(postId) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(feed.isNotEmpty(), postId) {
        if (feed.isNotEmpty() && !jumped) { listState.scrollToItem(startIndex); jumped = true }
    }
    val currentPost = feed.getOrNull(listState.firstVisibleItemIndex)

    val edit = LocalEditMode.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    // Coming from the post's edit sheet: open the details form as soon as the post is on screen.
    androidx.compose.runtime.LaunchedEffect(jumped) { if (jumped && openDetails && edit) showDetails = true }
    var showManageComments by remember { mutableStateOf(false) }
    var commentsFor by remember { mutableStateOf<PostEntity?>(null) }

    var captureBounds by remember { mutableStateOf<android.graphics.Rect?>(null) }
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    fun saveScreenshot() {
        scope.launch {
            val bounds = captureBounds
            val bitmap = if (activity != null && bounds != null) captureWindowRegion(activity, bounds) else null
            val ok = bitmap != null &&
                MediaStorage.saveBitmapToGallery(context, bitmap, "storyframe_post_${System.currentTimeMillis()}.jpg")
            MediaStorage.toast(context, if (ok) "Saved to gallery" else "Failed to save screenshot")
        }
    }

    StoryframeTheme(darkTheme = isDark) {
        val colors = LocalStoryframeColors.current
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (edit) {
                                DropdownMenuItem(text = { Text("Edit post details") }, onClick = { menuExpanded = false; showDetails = true })
                                DropdownMenuItem(text = { Text("Manage comments") }, onClick = { menuExpanded = false; showManageComments = true })
                            }
                            DropdownMenuItem(text = { Text("Save screenshot") }, onClick = { menuExpanded = false; saveScreenshot() })
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(colors.background)
                    .onGloballyPositioned { c ->
                        val b = c.boundsInWindow()
                        captureBounds = android.graphics.Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                    }
            ) {
                PostsHeader(colors, onBack)
                if (feed.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No posts", color = colors.secondaryText)
                    }
                } else {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(feed, key = { it.id }) { post ->
                            PostItem(
                                post = post,
                                profile = state.profile,
                                carousel = state.carousels[post.id].orEmpty(),
                                colors = colors,
                                onCommentsClick = { commentsFor = post }
                            )
                        }
                    }
                }
            }
        }

        if (showDetails && currentPost != null) {
            PostDetailsDialog(
                post = currentPost,
                onDismiss = { showDetails = false },
                onSave = { viewModel.updatePost(it); showDetails = false }
            )
        }
        if (showManageComments && currentPost != null) {
            ManageCommentsSheet(viewModel, currentPost, colors) { showManageComments = false }
        }
        commentsFor?.let { post ->
            CommentsSheet(viewModel, post, state.profile, colors) { commentsFor = null }
        }
    }
}

@Composable
private fun PostsHeader(colors: StoryframeColors, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primaryText,
            modifier = Modifier.clickable(onClick = onBack)
        )
        Spacer(Modifier.width(24.dp))
        Text("Posts", color = colors.primaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PostItem(
    post: PostEntity,
    profile: ProfileEntity,
    carousel: List<String>,
    colors: StoryframeColors,
    onCommentsClick: () -> Unit
) {
    var liked by remember(post.id) { mutableStateOf(false) }
    var reposted by remember(post.id) { mutableStateOf(false) }
    var snackbar by remember(post.id) { mutableStateOf(false) }
    var captionExpanded by remember(post.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ---- author row ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(profile.avatarUri, 32.dp, colors)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.username.ifBlank { "username" },
                        color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(painterResource(R.drawable.ic_badge_verified), null, tint = Color.Unspecified, modifier = Modifier.size(13.dp))
                    }
                }
                if (post.musicText.isNotBlank()) {
                    Text("♫ ${post.musicText}", color = colors.primaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(IgIcons.Menu, null, tint = colors.primaryText, modifier = Modifier.size(22.dp))
        }

        // ---- media ----
        val uris = if (post.mediaType == MediaType.CAROUSEL && carousel.isNotEmpty()) carousel else listOfNotNull(post.primaryMediaUri)
        val pagerState = rememberPagerState(pageCount = { uris.size.coerceAtLeast(1) })
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(colors.pillBackground)) {
            if (uris.isNotEmpty()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(uris[page], null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            if (uris.size > 1) {
                Text(
                    "${pagerState.currentPage + 1}/${uris.size}",
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color(0x99000000), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        if (uris.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(uris.size) { i ->
                    Box(
                        Modifier.padding(horizontal = 2.dp).size(6.dp)
                            .background(if (i == pagerState.currentPage) colors.accentBlue else Color(0xFF737373), CircleShape)
                    )
                }
            }
        }

        // ---- actions ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionCount(
                icon = { Icon(if (liked) IgIcons.HeartFilled else IgIcons.Heart, "Like", tint = if (liked) Color(0xFFFF3040) else colors.primaryText, modifier = Modifier.size(24.dp)) },
                count = post.likeCount, colors = colors, onClick = { liked = !liked }
            )
            Spacer(Modifier.width(18.dp))
            ActionCount(
                icon = { Icon(IgIcons.Comment, "Comments", tint = colors.primaryText, modifier = Modifier.size(24.dp)) },
                count = post.commentCount, colors = colors, onClick = onCommentsClick
            )
            Spacer(Modifier.width(18.dp))
            ActionCount(
                icon = { Icon(IgIcons.Repost, "Repost", tint = if (reposted) IgRepostGreen else colors.primaryText, modifier = Modifier.size(24.dp)) },
                count = if (reposted) bumped(post.repostCount) else post.repostCount, colors = colors,
                onClick = { reposted = !reposted; snackbar = reposted }
            )
            Spacer(Modifier.width(18.dp))
            ActionCount(
                icon = { Icon(IgIcons.Share, "Share", tint = colors.primaryText, modifier = Modifier.size(24.dp)) },
                count = post.shareCount, colors = colors, onClick = {}
            )
            Spacer(Modifier.weight(1f))
            Icon(IgIcons.Bookmark, "Save", tint = colors.primaryText, modifier = Modifier.size(24.dp))
        }

        // ---- caption + date ----
        val mention = if (colors.isDark) Color(0xFF7FB4FF) else Color(0xFF00376B)
        if (post.caption.isNotBlank()) {
            CaptionText(
                username = profile.username.ifBlank { "username" },
                caption = post.caption,
                expanded = captionExpanded,
                colors = colors,
                mention = mention,
                onExpand = { captionExpanded = true },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        if (post.dateText.isNotBlank()) {
            Text(
                "${post.dateText}  •  See translation",
                color = colors.secondaryText, fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        if (snackbar) {
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500); snackbar = false }
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .background(if (colors.isDark) Color(0xFF262626) else Color(0xFF262626), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(IgIcons.Repost, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Reposted", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("Undo", color = Color(0xFFB0B0B0), fontSize = 13.sp, modifier = Modifier.clickable { reposted = false; snackbar = false })
            }
        }
    }
}

/** "184" -> "185"; abbreviated counts like "1.2K" are left alone. */
private fun bumped(count: String): String = count.replace(",", "").toLongOrNull()?.plus(1)?.toString() ?: count

@Composable
private fun ActionCount(icon: @Composable () -> Unit, count: String, colors: StoryframeColors, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onClick)) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(count, color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Avatar(uri: String?, size: androidx.compose.ui.unit.Dp, colors: StoryframeColors) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(colors.pillBackground),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Filled.Person, null, tint = colors.secondaryText, modifier = Modifier.size(size * 0.6f))
    }
}

/** Bold username + caption with @mentions / #tags in blue and a "… more" cut at two lines. */
@Composable
private fun CaptionText(
    username: String,
    caption: String,
    expanded: Boolean,
    colors: StoryframeColors,
    mention: Color,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val full = remember(username, caption, mention) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(username) }
            append(" ")
            append(styleTags(caption, mention))
        }
    }
    var collapsed by remember(full) { mutableStateOf<AnnotatedString?>(null) }
    Text(
        text = if (expanded) full else collapsed ?: full,
        color = colors.primaryText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 2,
        onTextLayout = { result ->
            if (!expanded && collapsed == null && result.hasVisualOverflow) {
                val cut = (result.getLineEnd(1, visibleEnd = true) - 9).coerceAtLeast(username.length + 2)
                collapsed = buildAnnotatedString {
                    append(full.subSequence(0, cut))
                    withStyle(SpanStyle(color = colors.secondaryText)) { append("… more") }
                }
            }
        },
        modifier = modifier.clickable(enabled = !expanded, onClick = onExpand)
    )
}

private fun styleTags(text: String, color: Color): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("[@#][\\p{L}\\p{N}_.]+")
    var last = 0
    for (m in pattern.findAll(text)) {
        append(text.substring(last, m.range.first))
        withStyle(SpanStyle(color = color)) { append(m.value) }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

// ------------------------------------------------------------------------------------------
// Comments
// ------------------------------------------------------------------------------------------

@Composable
private fun CommentsSheet(viewModel: PostViewModel, post: PostEntity, author: com.fakegram.profile.data.ProfileEntity, colors: StoryframeColors, onDismiss: () -> Unit) {
    val comments by viewModel.comments(post.id).collectAsState(initial = emptyList())
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<CommentEntity?>(null) }
    val accounts by viewModel.accounts.collectAsState()
    val edit = LocalEditMode.current
    val sheetBg = if (colors.isDark) Color(0xFF1E1E1E) else Color.White
    val mention = if (colors.isDark) Color(0xFF7FB4FF) else Color(0xFF00376B)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp)) {
                Box(Modifier.width(36.dp).height(3.dp).background(colors.secondaryText, RoundedCornerShape(2.dp)))
                Spacer(Modifier.height(12.dp))
                Text("Comments", color = colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(520.dp).imePadding()) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (comments.isEmpty()) {
                    Text("No comments yet.", color = colors.secondaryText, modifier = Modifier.padding(24.dp))
                }
                comments.forEach { c -> CommentRow(c, colors, mention) { if (edit) editing = c } }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("❤️", "🙌", "🔥", "👏", "😢", "😍", "😮", "😂")
                    .forEach { Text(it, fontSize = 24.sp) }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(author.avatarUri, 36.dp, colors)
                Spacer(Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .background(colors.pillBackground, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type the comment right here; "Post" adds it as this profile.
                    androidx.compose.foundation.text.BasicTextField(
                        value = draft, onValueChange = { draft = it }, maxLines = 4,
                        textStyle = androidx.compose.ui.text.TextStyle(color = colors.primaryText, fontSize = 14.sp).inAppFont(),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primaryText),
                        modifier = Modifier.weight(1f).padding(vertical = 11.dp),
                        decorationBox = { inner -> Box { if (draft.isEmpty()) Text("What do you think of this?", color = colors.secondaryText, fontSize = 13.sp); inner() } }
                    )
                    if (draft.isNotBlank() && edit) {
                        Text(
                            "Post", color = Color(0xFF3797F0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                viewModel.postComment(post.id, comments.size, author.username, author.avatarUri, draft.trim())
                                draft = ""
                            }.padding(start = 8.dp)
                        )
                    } else Text("GIF", color = colors.secondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    editing?.let { c ->
        CommentEditDialog(
            comment = c, accounts = accounts,
            onSaveAccount = { name, path, uri, verified -> viewModel.saveAccount(name, path, uri, verified) },
            onDismiss = { editing = null },
            onSave = { updated, avatar -> viewModel.saveComment(updated, avatar); editing = null },
            onDelete = { viewModel.deleteComment(c.id); editing = null }
        )
    }
}

@Composable
private fun CommentRow(c: CommentEntity, colors: StoryframeColors, mention: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(c.avatarUri, 36.dp, colors)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.username, color = colors.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                if (c.isVerified) {
                    Spacer(Modifier.width(3.dp))
                    Icon(painterResource(R.drawable.ic_badge_verified), null, tint = Color.Unspecified, modifier = Modifier.size(11.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(c.timeText, color = colors.secondaryText, fontSize = 12.sp)
            }
            Text(styleTags(c.text, mention), color = colors.primaryText, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 2.dp))
            Row(modifier = Modifier.padding(top = 6.dp)) {
                Text("Reply", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (c.hasTranslation) {
                    Spacer(Modifier.width(16.dp))
                    Text("See translation", color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 10.dp, top = 6.dp)) {
            Icon(IgIcons.Heart, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
            if (c.likeCount.isNotBlank() && c.likeCount != "0") {
                Text(c.likeCount, color = colors.secondaryText, fontSize = 12.sp)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Editing
// ------------------------------------------------------------------------------------------

@Composable
private fun PostDetailsDialog(post: PostEntity, onDismiss: () -> Unit, onSave: (PostEntity) -> Unit) {
    var caption by remember(post.id) { mutableStateOf(post.caption) }
    var likes by remember(post.id) { mutableStateOf(post.likeCount) }
    var comments by remember(post.id) { mutableStateOf(post.commentCount) }
    var reposts by remember(post.id) { mutableStateOf(post.repostCount) }
    var shares by remember(post.id) { mutableStateOf(post.shareCount) }
    var date by remember(post.id) { mutableStateOf(post.dateText) }
    var music by remember(post.id) { mutableStateOf(post.musicText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post details") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(caption, { caption = it }, label = { Text("Caption") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(likes, { likes = it }, label = { Text("Likes") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(comments, { comments = it }, label = { Text("Comments") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(reposts, { reposts = it }, label = { Text("Reposts") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(shares, { shares = it }, label = { Text("Shares") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(date, { date = it }, label = { Text("Date (September 13)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(music, { music = it }, label = { Text("Music (JENNIE · FALLEN ANGEL)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(post.copy(caption = caption, likeCount = likes, commentCount = comments, repostCount = reposts, shareCount = shares, dateText = date, musicText = music))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ManageCommentsSheet(viewModel: PostViewModel, post: PostEntity, colors: StoryframeColors, onDismiss: () -> Unit) {
    val comments by viewModel.comments(post.id).collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<CommentEntity?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Comments", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                comments.forEach { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { editing = c }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(c.avatarUri, 36.dp, colors)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(c.username, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(c.text.ifBlank { "(empty)" }, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { viewModel.addComment(post.id, comments.size) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add comment")
            }
        }
    }
    val accounts by viewModel.accounts.collectAsState()
    editing?.let { c ->
        CommentEditDialog(
            comment = c,
            accounts = accounts,
            onSaveAccount = { name, path, uri, verified -> viewModel.saveAccount(name, path, uri, verified) },
            onDismiss = { editing = null },
            onSave = { updated, avatar -> viewModel.saveComment(updated, avatar); editing = null },
            onDelete = { viewModel.deleteComment(c.id); editing = null }
        )
    }
}

@Composable
private fun CommentEditDialog(
    comment: CommentEntity,
    accounts: List<AccountEntity>,
    onSaveAccount: (String, String?, android.net.Uri?, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CommentEntity, android.net.Uri?) -> Unit,
    onDelete: () -> Unit
) {
    var username by remember(comment.id) { mutableStateOf(comment.username) }
    var text by remember(comment.id) { mutableStateOf(comment.text) }
    var time by remember(comment.id) { mutableStateOf(comment.timeText) }
    var likes by remember(comment.id) { mutableStateOf(comment.likeCount) }
    var verified by remember(comment.id) { mutableStateOf(comment.isVerified) }
    var translation by remember(comment.id) { mutableStateOf(comment.hasTranslation) }
    var pickedAvatar by remember(comment.id) { mutableStateOf<android.net.Uri?>(null) }
    // Path of an avatar that already lives in app storage (existing comment or a saved account).
    var avatarPath by remember(comment.id) { mutableStateOf(comment.avatarUri) }
    var showPicker by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) pickedAvatar = uri }
    val colors = LocalStoryframeColors.current

    if (showPicker) {
        AccountPickerDialog(
            accounts = accounts,
            onDismiss = { showPicker = false },
            onPick = { a ->
                username = a.username; verified = a.isVerified; avatarPath = a.avatarUri; pickedAvatar = null
                showPicker = false
            }
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit comment") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Avatar(pickedAvatar?.toString() ?: avatarPath, 48.dp, colors)
                    }
                    Text("Tap to change avatar", fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) { Text("Saved account", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = { if (username.isNotBlank()) onSaveAccount(username.trim(), avatarPath, pickedAvatar, verified) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save to accounts", fontSize = 12.sp) }
                }
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(text, { text = it }, label = { Text("Comment") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(time, { time = it }, label = { Text("Time (1w)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(likes, { likes = it }, label = { Text("Likes") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Verified", modifier = Modifier.weight(1f)); Switch(verified, { verified = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\"See translation\"", modifier = Modifier.weight(1f)); Switch(translation, { translation = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(comment.copy(username = username, text = text, timeText = time, likeCount = likes, isVerified = verified, hasTranslation = translation, avatarUri = avatarPath), pickedAvatar)
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
