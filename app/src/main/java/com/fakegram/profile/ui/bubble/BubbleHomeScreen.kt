package com.fakegram.profile.ui.bubble

import androidx.compose.foundation.background
import com.fakegram.profile.ui.phone.LocalEditMode
import com.fakegram.profile.ui.phone.PhoneSearchField
import com.fakegram.profile.ui.phone.phoneColors
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.fakegram.profile.data.BubbleChatEntity
import com.fakegram.profile.data.ProfileRepository
import kotlinx.coroutines.launch

/** The "Bubble" tab: your fake artist chats. */
@Composable
fun BubbleHomeScreen(repository: ProfileRepository, onOpenChat: (Long) -> Unit, onBack: () -> Unit) {
    val viewModel: BubbleHomeViewModel = viewModel(factory = BubbleHomeViewModel.factory(repository))
    val allChats by viewModel.chats.collectAsState()
    val previews by viewModel.lastMessages.collectAsState()
    val edit = LocalEditMode.current
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val chats = if (q.isEmpty()) allChats else allChats.filter {
        it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q) || previews[it.id].orEmpty().lowercase().contains(q)
    }

    val app = LocalContext.current.applicationContext as StoryframeApp
    val isDark by app.themePreferences.isDarkMode.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bubble", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { scope.launch { app.themePreferences.setDarkMode(!isDark) } }) {
                        Icon(if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = "Toggle theme")
                    }
                }
            )
        },
        floatingActionButton = {
            if (edit) FloatingActionButton(onClick = { viewModel.createChat { id -> onOpenChat(id) } }) {
                Icon(Icons.Filled.Add, contentDescription = "New chat")
            }
        }
    ) { padding ->
        if (allChats.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(if (edit) "No chats yet. Tap + to start a chat with your favourite artist." else "No chats")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    PhoneSearchField(query, { query = it }, "Search chats", phoneColors())
                }
                if (chats.isEmpty()) item { Text("No results", modifier = Modifier.padding(8.dp)) }
                items(chats, key = { it.id }) { chat ->
                    ChatRow(
                        edit = edit,
                        chat = chat,
                        preview = previews[chat.id].orEmpty(),
                        onClick = { onOpenChat(chat.id) },
                        onDuplicate = { viewModel.duplicateChat(chat.id) },
                        onDelete = { viewModel.deleteChat(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRow(edit: Boolean, chat: BubbleChatEntity, preview: String, onClick: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val theme = BubbleThemes.byId(chat.theme)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(theme.background),
            contentAlignment = Alignment.Center
        ) {
            if (chat.artistAvatarUri != null) {
                AsyncImage(chat.artistAvatarUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.Person, null, tint = Color.White)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(chat.title.ifBlank { chat.artistName }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(chat.artistName, fontSize = 13.sp, maxLines = 1)
            if (preview.isNotBlank()) Text(preview, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (edit) Box {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuExpanded = false; onDuplicate() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuExpanded = false; onDelete() })
            }
        }
    }
}
