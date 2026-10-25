package com.fakegram.profile.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.R
import com.fakegram.profile.data.MediaType
import com.fakegram.profile.data.PostEntity
import com.fakegram.profile.data.StoryRingType
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import com.fakegram.profile.ui.theme.StoryframeColors
import com.fakegram.profile.ui.util.formatCount

enum class StatField { POSTS, FOLLOWERS, FOLLOWING }

/**
 * Cycles Follow -> Follow Back -> Following -> Close Friends -> Follow ... used only in
 * "preview as visitor" mode. FOLLOWING is the normal grey button; CLOSE_FRIENDS is the green one.
 */
enum class FollowState { FOLLOW, FOLLOW_BACK, FOLLOWING, CLOSE_FRIENDS }

fun FollowState.next(): FollowState = when (this) {
    FollowState.FOLLOW -> FollowState.FOLLOW_BACK
    FollowState.FOLLOW_BACK -> FollowState.FOLLOWING
    FollowState.FOLLOWING -> FollowState.CLOSE_FRIENDS
    FollowState.CLOSE_FRIENDS -> FollowState.FOLLOW
}

/**
 * The single, real, full-scale rendering of a profile. Used both as the always-on editing
 * surface (tap targets fire the on*Click callbacks to open small edit dialogs) and, with
 * [isExporting] = true, as the exact source bitmap for the screenshot export — [isExporting]
 * hides every add-affordance (empty-state "+" tiles, placeholder rows) so the captured image
 * looks like a real profile screen with no editor chrome.
 */
@Composable
fun ProfilePreviewContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    isExporting: Boolean = false,
    isVisitorMode: Boolean = false,
    followState: FollowState = FollowState.FOLLOW,
    onEditProfileClick: () -> Unit = {},
    onFollowClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onStatClick: (StatField) -> Unit = {},
    onFollowedByClick: () -> Unit = {},
    onHighlightClick: (Long) -> Unit = {},
    onAddHighlightClick: () -> Unit = {},
    onPostClick: (PostEntity) -> Unit = {},
    onPostHold: (PostEntity) -> Unit = {},
    onAddPostClick: (Boolean) -> Unit = {},
    onReorderPosts: (List<PostEntity>) -> Unit = {}
) {
    val colors = LocalStoryframeColors.current
    var selectedTab by remember { mutableStateOf(ProfileTabKind.POSTS) }
    val profile = state.profile
    val showAddAffordances = !isExporting && !isVisitorMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopBarSection(username = profile.username, isVerified = profile.isVerified, isVisitorMode = isVisitorMode, colors = colors)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            AvatarStatsRow(
                state = state,
                onAvatarClick = onAvatarClick,
                onStatClick = onStatClick
            )

            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = profile.bio,
                    color = colors.primaryText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }

            val followedByNames = state.followedBy.sortedBy { it.position }.map { it.username }
            val summary = followedBySummary(followedByNames)
            if (summary != null) {
                Spacer(Modifier.height(8.dp))
                FollowedByRow(
                    avatars = state.followedBy.sortedBy { it.position }.take(3).map { it.avatarUri },
                    summary = summary,
                    colors = colors,
                    onClick = onFollowedByClick
                )
            }

            Spacer(Modifier.height(12.dp))
            ActionButtonsRow(
                isVisitorMode = isVisitorMode,
                followState = followState,
                onEditProfileClick = onEditProfileClick,
                onFollowClick = onFollowClick
            )

            if (state.highlights.isNotEmpty() || showAddAffordances) {
                Spacer(Modifier.height(14.dp))
                HighlightsRow(
                    highlights = state.highlights.sortedBy { it.position },
                    colors = colors,
                    showAddTile = showAddAffordances,
                    onHighlightClick = onHighlightClick,
                    onAddHighlightClick = onAddHighlightClick
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        TabsRow(
            selected = selectedTab,
            showReels = profile.showReelsTab,
            showReposts = profile.showRepostsTab,
            colors = colors,
            onSelect = { selectedTab = it }
        )

        val gridItems = when (selectedTab) {
            ProfileTabKind.POSTS -> state.gridPosts
            ProfileTabKind.REELS -> state.reelPosts
            ProfileTabKind.REPOSTS -> emptyList()
            ProfileTabKind.TAGGED -> state.taggedPosts
        }

        PostsGridInteractive(
            posts = gridItems,
            emptyLabel = when (selectedTab) {
                ProfileTabKind.POSTS -> "No posts yet"
                ProfileTabKind.REELS -> "No reels yet"
                ProfileTabKind.REPOSTS -> "No reposts yet"
                ProfileTabKind.TAGGED -> "No tagged posts"
            },
            colors = colors,
            carouselCounts = state.carouselCounts,
            allowReorder = selectedTab == ProfileTabKind.POSTS,
            showAddTile = showAddAffordances && (selectedTab == ProfileTabKind.POSTS || selectedTab == ProfileTabKind.TAGGED),
            isTaggedTab = selectedTab == ProfileTabKind.TAGGED,
            onPostClick = onPostClick,
            onPostHold = onPostHold,
            onAddClick = { onAddPostClick(selectedTab == ProfileTabKind.TAGGED) },
            onReorder = onReorderPosts
        )

        Spacer(Modifier.height(8.dp))
        }

        BottomNavBar(colors = colors)
    }
}

@Composable
private fun TopBarSection(username: String, isVerified: Boolean, isVisitorMode: Boolean, colors: StoryframeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = username.ifBlank { "username" },
                color = colors.primaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isVerified) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_badge_verified),
                    contentDescription = "Verified",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        // A visitor viewing someone else's profile sees a back arrow here, never the
        // own-profile chevron dropdown (which implies switching between your own accounts).
        Icon(
            if (isVisitorMode) Icons.Filled.ArrowBack else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = colors.primaryText
        )
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = colors.primaryText, modifier = Modifier.padding(end = 16.dp))
        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = colors.primaryText)
    }
}

@Composable
private fun AvatarStatsRow(
    state: ProfileUiState,
    onAvatarClick: () -> Unit,
    onStatClick: (StatField) -> Unit
) {
    val profile = state.profile
    Row(verticalAlignment = Alignment.Top) {
        AvatarWithRing(
            avatarUri = profile.avatarUri,
            ringType = profile.storyRingType,
            hasStory = profile.hasActiveStory,
            size = 90.dp,
            onClick = onAvatarClick
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.displayName.ifBlank { " " },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = LocalStoryframeColors.current.primaryText
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                StatColumn(profile.postCount, "posts", onClick = { onStatClick(StatField.POSTS) })
                StatColumn(profile.followerCount, "followers", onClick = { onStatClick(StatField.FOLLOWERS) })
                StatColumn(profile.followingCount, "following", onClick = { onStatClick(StatField.FOLLOWING) })
            }
        }
    }
}

@Composable
private fun AvatarWithRing(
    avatarUri: String?,
    ringType: StoryRingType,
    hasStory: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val colors = LocalStoryframeColors.current
    val showRing = ringType != StoryRingType.NONE
    val isGradientRing = ringType == StoryRingType.NORMAL && hasStory
    val ringBorder = when {
        // 3-tone sweep with no yellow: red-orange -> magenta -> purple -> magenta -> back to
        // red-orange, mirrored symmetrically so both sides transition through magenta the same
        // way (round 5 correction — dropped the earlier 5-stop yellow-inclusive gradient).
        // sweepGradient always starts its first color at the 3-o'clock position, so the ring
        // layer below is separately rotated -90° to bring red-orange to the top instead.
        isGradientRing -> Modifier.border(
            width = 3.dp,
            brush = Brush.sweepGradient(
                listOf(
                    Color(0xFFFD5949),
                    Color(0xFFE1306C),
                    Color(0xFF833AB4),
                    Color(0xFFE1306C),
                    Color(0xFFFD5949)
                )
            ),
            shape = CircleShape
        )
        ringType == StoryRingType.CLOSE_FRIENDS -> Modifier.border(width = 3.dp, color = Color(0xFF6DD076), shape = CircleShape)
        else -> Modifier
    }
    // A thin gap of background color sits between the ring stroke and the photo itself,
    // matching real Instagram (the ring and the photo never touch directly).
    val gapPadding = if (showRing) 3.dp else 0.dp
    val innerClearance = if (showRing) 3.dp + gapPadding else 0.dp

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        // Ring layer is a separate, borderless-content box so it can be rotated independently —
        // rotating the whole avatar (photo included) would turn the photo sideways too.
        if (showRing) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(if (isGradientRing) Modifier.rotate(-90f) else Modifier)
                    .then(ringBorder)
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .padding(innerClearance)
                .clip(CircleShape)
                .background(colors.background)
                .padding(gapPadding)
                .clip(CircleShape)
                .background(Color(0xFFDDDDDD))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(size / 2))
            }
        }
    }
}

@Composable
private fun StatColumn(count: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = formatCount(count.ifBlank { "0" }),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = LocalStoryframeColors.current.primaryText
        )
        Text(text = label, fontSize = 13.sp, color = LocalStoryframeColors.current.secondaryText)
    }
}

@Composable
private fun FollowedByRow(
    avatars: List<String?>,
    summary: String,
    colors: StoryframeColors,
    onClick: () -> Unit
) {
    val avatarStackWidth = if (avatars.isEmpty()) 0.dp else (20 + (avatars.size - 1) * 14).dp
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(avatarStackWidth)
                .height(20.dp)
        ) {
            avatars.forEachIndexed { index, uri ->
                Box(
                    modifier = Modifier
                        .padding(start = (index * 14).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCCCCCC))
                        .border(1.dp, colors.background, CircleShape)
                ) {
                    if (uri != null) {
                        AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = summary,
            fontSize = 13.sp,
            color = colors.secondaryText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButtonsRow(
    isVisitorMode: Boolean,
    followState: FollowState,
    onEditProfileClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    val colors = LocalStoryframeColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (!isVisitorMode) {
            Button(
                onClick = onEditProfileClick,
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.pillBackground),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("Edit profile", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.pillBackground),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("Share profile", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            val isBlue = followState == FollowState.FOLLOW || followState == FollowState.FOLLOW_BACK
            Button(
                onClick = onFollowClick,
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlue) colors.accentBlue else colors.pillBackground
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                val label = when (followState) {
                    FollowState.FOLLOW -> "Follow"
                    FollowState.FOLLOW_BACK -> "Follow Back"
                    FollowState.FOLLOWING, FollowState.CLOSE_FRIENDS -> "Following  ▾"
                }
                val labelColor = when (followState) {
                    FollowState.CLOSE_FRIENDS -> Color(0xFF50AB4E)
                    FollowState.FOLLOWING -> colors.primaryText
                    else -> Color.White
                }
                Text(label, color = labelColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { },
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.pillBackground),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("Message", color = colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = { },
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.pillBackground),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = "Add person", tint = colors.primaryText, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HighlightsRow(
    highlights: List<com.fakegram.profile.data.HighlightEntity>,
    colors: StoryframeColors,
    showAddTile: Boolean,
    onHighlightClick: (Long) -> Unit,
    onAddHighlightClick: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(highlights, key = { it.id }) { h ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onHighlightClick(h.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(1.dp, colors.border, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(colors.pillBackground)
                ) {
                    if (h.coverUri != null) {
                        AsyncImage(model = h.coverUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = h.title.ifBlank { "Untitled" },
                    fontSize = 11.sp,
                    color = colors.primaryText,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (showAddTile) {
            item(key = "add_highlight") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(64.dp)
                        .clickable(onClick = onAddHighlightClick)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add highlight", tint = colors.primaryText)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(text = "New", fontSize = 11.sp, color = colors.primaryText, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Posts and Tagged are always shown. Reels shows only when [ProfileEntity.showReelsTab] is on;
 * Reposts only when [ProfileEntity.showRepostsTab] is on. Corrected icon mapping (round 5):
 * the rounded-square-with-play-triangle glyph (`ic_nav_reels`, shared with the bottom nav) is the
 * real Reels tab icon; the loop/double-arrows glyph (`ic_tab_reels`, despite its filename) is
 * actually "Reposts".
 */
private enum class ProfileTabKind { POSTS, REELS, REPOSTS, TAGGED }

@Composable
private fun TabsRow(
    selected: ProfileTabKind,
    showReels: Boolean,
    showReposts: Boolean,
    colors: StoryframeColors,
    onSelect: (ProfileTabKind) -> Unit
) {
    val tabs = buildList {
        add(ProfileTabKind.POSTS)
        if (showReels) add(ProfileTabKind.REELS)
        if (showReposts) add(ProfileTabKind.REPOSTS)
        add(ProfileTabKind.TAGGED)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .drawBehind {
                drawLine(
                    color = colors.border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        tabs.forEach { kind ->
            val isSelected = selected == kind
            val tint = if (isSelected) colors.primaryText else colors.secondaryText
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(kind) }
                    .drawBehind {
                        if (isSelected) {
                            drawLine(
                                color = colors.primaryText,
                                start = Offset(0f, size.height - 1.dp.toPx()),
                                end = Offset(size.width, size.height - 1.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (kind) {
                    ProfileTabKind.POSTS -> Icon(Icons.Filled.GridOn, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    ProfileTabKind.REELS -> Icon(painter = painterResource(R.drawable.ic_nav_reels), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    ProfileTabKind.REPOSTS -> Icon(painter = painterResource(R.drawable.ic_tab_reels), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    ProfileTabKind.TAGGED -> Icon(painter = painterResource(R.drawable.ic_tab_tagged), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** Sentinel post id used for the trailing "+" grid tile; never persisted. */
private const val ADD_TILE_ID = -1L

/** Instagram's current grid tiles are a 4:5 portrait rectangle, not square. */
private const val GRID_ASPECT_RATIO = 4f / 5f

@Composable
private fun PostsGridInteractive(
    posts: List<PostEntity>,
    emptyLabel: String,
    colors: StoryframeColors,
    carouselCounts: Map<Long, Int>,
    allowReorder: Boolean,
    showAddTile: Boolean,
    isTaggedTab: Boolean,
    onPostClick: (PostEntity) -> Unit,
    onPostHold: (PostEntity) -> Unit,
    onAddClick: () -> Unit,
    onReorder: (List<PostEntity>) -> Unit
) {
    var orderedPosts by remember { mutableStateOf(posts) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    // Never let a database emission overwrite the local order while a finger is down.
    LaunchedEffect(posts) { if (draggedId == null) orderedPosts = posts }

    if (posts.isEmpty() && !showAddTile) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyLabel, color = colors.secondaryText, fontSize = 13.sp)
        }
        return
    }

    val density = LocalDensity.current
    val gapPx = with(density) { 1.5.dp.toPx() }
    val totalCells = orderedPosts.size + if (showAddTile) 1 else 0
    val rows = (totalCells + 2) / 3

    // A plain (non-lazy) grid where every cell is absolutely positioned from its index. Because
    // the slot maths is trivial, one drag gesture on the container can turn a finger position
    // straight into a target slot, the lifted tile follows the finger exactly, and the other
    // tiles glide into their new slots.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellW = (constraints.maxWidth - 2 * gapPx) / 3f
        val cellH = cellW / GRID_ASPECT_RATIO
        val cellWDp = with(density) { cellW.toDp() }
        val cellHDp = with(density) { cellH.toDp() }
        val heightDp = with(density) { (rows * cellH + (rows - 1).coerceAtLeast(0) * gapPx).toDp() }

        fun slotOffset(index: Int) = Offset((index % 3) * (cellW + gapPx), (index / 3) * (cellH + gapPx))
        fun slotAt(pos: Offset): Int {
            val col = (pos.x / (cellW + gapPx)).toInt().coerceIn(0, 2)
            val row = (pos.y / (cellH + gapPx)).toInt().coerceAtLeast(0)
            return row * 3 + col
        }

        var fingerPos by remember { mutableStateOf(Offset.Zero) }
        var grabOffset by remember { mutableStateOf(Offset.Zero) }
        var movedPx by remember { mutableStateOf(0f) }
        var heldPost by remember { mutableStateOf<PostEntity?>(null) }

        val dragModifier = if (allowReorder) {
            Modifier.pointerInput(cellW, cellH) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        val slot = slotAt(start)
                        if (slot in orderedPosts.indices) {
                            draggedId = orderedPosts[slot].id
                            grabOffset = start - slotOffset(slot)
                            fingerPos = start
                            movedPx = 0f
                            heldPost = orderedPosts[slot]
                        }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val id = draggedId ?: return@detectDragGesturesAfterLongPress
                        fingerPos += amount
                        movedPx += kotlin.math.abs(amount.x) + kotlin.math.abs(amount.y)
                        val from = orderedPosts.indexOfFirst { it.id == id }
                        val to = slotAt(fingerPos).coerceIn(0, orderedPosts.lastIndex)
                        if (from >= 0 && to != from) {
                            val mutable = orderedPosts.toMutableList()
                            mutable.add(to, mutable.removeAt(from))
                            orderedPosts = mutable
                        }
                    },
                    onDragEnd = {
                        draggedId = null
                        // Held without moving: that is "edit this post"; moving it is a reorder.
                        val held = heldPost
                        if (movedPx < 24f && held != null) { orderedPosts = posts; onPostHold(held) } else onReorder(orderedPosts)
                        heldPost = null
                    },
                    onDragCancel = {
                        draggedId = null
                        orderedPosts = posts
                    }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .then(dragModifier)
        ) {
            orderedPosts.forEachIndexed { index, post ->
                key(post.id) {
                    val isDragged = post.id == draggedId
                    val slot by animateOffsetAsState(slotOffset(index), label = "gridSlot")
                    val pos = if (isDragged) fingerPos - grabOffset else slot
                    Box(
                        modifier = Modifier
                            .size(cellWDp, cellHDp)
                            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer {
                                val s = if (isDragged) 1.06f else 1f
                                scaleX = s
                                scaleY = s
                                alpha = if (isDragged) 0.92f else 1f
                            }
                            .clickable { onPostClick(post) }
                    ) {
                        PostThumb(post = post, carouselCount = carouselCounts[post.id] ?: 0)
                    }
                }
            }
            if (showAddTile) {
                val pos = slotOffset(orderedPosts.size)
                Box(
                    modifier = Modifier
                        .size(cellWDp, cellHDp)
                        .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                        .background(colors.pillBackground)
                        .border(1.dp, colors.border)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = if (isTaggedTab) "Add tagged post" else "Add post", tint = colors.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun PostThumb(post: PostEntity, carouselCount: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (post.primaryMediaUri != null) {
            AsyncImage(
                model = post.primaryMediaUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Type-indicator badge (video/carousel) sits top-LEFT; the pin badge sits top-RIGHT,
        // so a post that's both pinned and video/carousel can show both without overlapping.
        when (post.mediaType) {
            MediaType.VIDEO -> Icon(
                painter = painterResource(R.drawable.ic_badge_video),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
            )
            MediaType.CAROUSEL -> Icon(
                painter = painterResource(R.drawable.ic_badge_carousel),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
            )
            MediaType.IMAGE -> {}
        }
        if (post.isPinned) {
            Icon(
                painter = painterResource(R.drawable.ic_badge_pinned),
                contentDescription = "Pinned",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(14.dp)
            )
        }
    }
}

@Composable
private fun BottomNavBar(colors: StoryframeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = colors.border)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Icon(Icons.Filled.Home, contentDescription = null, tint = colors.primaryText)
        Icon(Icons.Filled.Search, contentDescription = null, tint = colors.primaryText)
        Icon(painter = painterResource(R.drawable.ic_nav_reels), contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(24.dp))
        Icon(painter = painterResource(R.drawable.ic_nav_dm), contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(24.dp))
        Icon(Icons.Filled.Person, contentDescription = null, tint = colors.primaryText)
    }
}
