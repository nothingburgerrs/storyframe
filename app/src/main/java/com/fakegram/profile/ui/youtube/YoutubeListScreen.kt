package com.fakegram.profile.ui.youtube

import com.fakegram.profile.ui.theme.inAppFont
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.YoutubeVideoEntity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.SolidColor
import com.fakegram.profile.ui.phone.LocalEditMode
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.outlined.VideoLibrary

private val YtChips = listOf("All", "Mixes", "Music", "Gaming", "Live", "Podcasts", "Recently uploaded", "Watched")

/** What opening YouTube shows: the home feed of every video. The search icon looks through them; in Edit mode + adds one. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YoutubeListScreen(onOpenVideo: (Long) -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val vm: YoutubeListViewModel = viewModel(key = "ytList0", factory = YoutubeListViewModel.factory(app.repository, 0L))
    val videos by vm.videos.collectAsState()
    val edit = LocalEditMode.current
    var searching by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) videos else videos.filter {
        it.title.lowercase().contains(q) || it.channelName.lowercase().contains(q)
    }

    Column(Modifier.fillMaxSize().background(YtBg).imePadding()) {
        // top bar
        if (searching) {
            Row(Modifier.statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { searching = false; query = "" }, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close search", tint = YtText, modifier = Modifier.size(20.dp)) }
                Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF272727)).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = YtText, fontSize = 16.sp).inAppFont(), cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search YouTube", color = YtSub, fontSize = 16.sp); inner() } }
                    )
                    if (query.isNotEmpty()) Icon(Icons.Filled.Close, "Clear", tint = YtText, modifier = Modifier.size(20.dp).clickable { query = "" })
                }
            }
        } else {
            Row(Modifier.statusBarsPadding().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = YtText, modifier = Modifier.size(20.dp)) }
                Box(Modifier.size(width = 28.dp, height = 20.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF0000)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("YouTube", color = YtText, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Cast, null, tint = YtText, modifier = Modifier.padding(horizontal = 10.dp).size(21.dp))
                Icon(Icons.Outlined.NotificationsNone, null, tint = YtText, modifier = Modifier.padding(horizontal = 10.dp).size(23.dp))
                Icon(Icons.Filled.Search, "Search", tint = YtText, modifier = Modifier.clickable { searching = true }.padding(horizontal = 10.dp).size(23.dp))
            }
            // topic chips
            LazyRow(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                item {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF272727)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Explore, null, tint = YtText, modifier = Modifier.size(18.dp))
                    }
                }
                items(YtChips) { label ->
                    val on = label == "All"
                    Text(
                        label, color = if (on) Color.Black else YtText, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) Color.White else Color(0xFF272727)).padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // the feed
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (q.isNotEmpty()) "No results for “${query.trim()}”" else if (edit) "Nothing here yet. Tap + below to add a video to the feed." else "No videos",
                        color = YtSub, fontSize = 14.sp, modifier = Modifier.padding(24.dp)
                    )
                }
            }
            items(shown, key = { it.id }) { v -> FeedCard(v, onOpen = { onOpenVideo(v.id) }, onEdit = { editId = v.id }) }
            item { Spacer(Modifier.height(12.dp)) }
        }

        // bottom bar
        Row(Modifier.fillMaxWidth().background(YtBg).navigationBarsPadding().padding(top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
            NavItem(Icons.Filled.Home, "Home", true, Modifier.weight(1f)) {}
            NavItem(Icons.Outlined.PlayCircleOutline, "Shorts", false, Modifier.weight(1f)) {}
            Box(Modifier.weight(1f).clickable { if (edit) vm.create("", null) { id -> editId = id } }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AddCircleOutline, "New video", tint = YtText, modifier = Modifier.size(38.dp))
            }
            NavItem(Icons.Outlined.Subscriptions, "Subscriptions", false, Modifier.weight(1f)) {}
            NavItem(Icons.Outlined.VideoLibrary, "Library", false, Modifier.weight(1f)) {}
        }
    }

    EditHost(editId, videos, vm) { editId = null }
}

@Composable
private fun EditHost(editId: Long?, videos: List<com.fakegram.profile.data.YoutubeVideoEntity>, vm: YoutubeListViewModel, onClose: () -> Unit) {
    val video = videos.firstOrNull { it.id == editId } ?: return
    VideoEditSheet(
        video = video,
        update = { transform -> vm.update(video.id, transform) },
        onDelete = { vm.delete(video.id); onClose() },
        onDismiss = onClose
    )
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected) YtText else YtText.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (selected) YtText else YtSub, fontSize = 10.sp, maxLines = 1)
    }
}
