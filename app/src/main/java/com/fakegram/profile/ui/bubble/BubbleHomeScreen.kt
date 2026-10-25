package com.fakegram.profile.ui.bubble

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.data.BubbleChatEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.ui.phone.LocalEditMode
import com.fakegram.profile.ui.phone.PhoneColors
import com.fakegram.profile.ui.phone.PhoneSearchField
import com.fakegram.profile.ui.phone.phoneColors

private val BubbleAccent = Color(0xFF7C8CFF)

/** The Bubble app's chat list, laid out like Messages: big title, search, one row per artist. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BubbleHomeScreen(repository: ProfileRepository, onOpenChat: (Long) -> Unit, onBack: () -> Unit) {
    val viewModel: BubbleHomeViewModel = viewModel(factory = BubbleHomeViewModel.factory(repository))
    val allChats by viewModel.chats.collectAsState()
    val previews by viewModel.lastMessages.collectAsState()
    val edit = LocalEditMode.current
    val c = phoneColors()
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val chats = if (q.isEmpty()) allChats else allChats.filter {
        it.title.lowercase().contains(q) || it.artistName.lowercase().contains(q) || previews[it.id].orEmpty().lowercase().contains(q)
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = BubbleAccent, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
            Spacer(Modifier.weight(1f))
            if (edit) Icon(
                Icons.Filled.Add, "New chat", tint = BubbleAccent,
                modifier = Modifier.size(44.dp).clickable { viewModel.createChat { id -> onOpenChat(id) } }.padding(8.dp)
            )
        }
        Text("Bubble", color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))
        PhoneSearchField(query, { query = it }, "Search", c, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        if (chats.isEmpty()) {
            Text(
                if (q.isNotEmpty()) "No results" else if (edit) "No chats yet. Tap + to start one." else "No chats",
                color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
                items(chats, key = { it.id }) { chat ->
                    Column {
                        ChatRow(
                            edit = edit, chat = chat, preview = previews[chat.id].orEmpty(), c = c,
                            onClick = { onOpenChat(chat.id) },
                            onDuplicate = { viewModel.duplicateChat(chat.id) },
                            onDelete = { viewModel.deleteChat(chat.id) }
                        )
                        HorizontalDivider(Modifier.padding(start = 76.dp), color = c.line)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(edit: Boolean, chat: BubbleChatEntity, preview: String, c: PhoneColors, onClick: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val theme = BubbleThemes.byId(chat.theme)
    val name = chat.title.ifBlank { chat.artistName }
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { if (edit) menu = true }).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(theme.background), contentAlignment = Alignment.Center) {
                if (chat.artistAvatarUri != null) AsyncImage(chat.artistAvatarUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text(name.take(1).uppercase(), color = theme.topBarIcon, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name.ifBlank { "Chat" }, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (chat.artistName.isNotBlank() && chat.artistName != name) Text(chat.artistName, color = c.sub, fontSize = 13.sp, maxLines = 1)
                Text(preview.ifBlank { "No messages" }, color = c.sub, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
        }
    }
}
