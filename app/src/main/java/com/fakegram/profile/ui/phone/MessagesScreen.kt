package com.fakegram.profile.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.SmsMessageEntity
import com.fakegram.profile.data.SmsThreadEntity
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.InlineField
import com.fakegram.profile.ui.components.rememberCropPicker
import com.fakegram.profile.ui.shell.LocalEditMode
import kotlinx.coroutines.launch

private val MessagesBlue = Color(0xFF0A84FF)
private const val GapMillis = 60 * 60 * 1000L

/** A fake Messages app: conversation list with search, and iMessage-style chats. */
@Composable
fun MessagesScreen(characterId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val dao = app.repository.sms
    val threads by remember(characterId) { dao.observeThreads(characterId) }.collectAsState(initial = emptyList())
    val allMessages by remember(characterId) { dao.observeAllMessages(characterId) }.collectAsState(initial = emptyList())
    val edit = LocalEditMode.current
    val c = phoneColors()
    val scope = rememberCoroutineScope()
    var openId by rememberSaveable { mutableStateOf(-1L) }
    var query by rememberSaveable { mutableStateOf("") }
    var newThread by remember { mutableStateOf(false) }
    val open = threads.firstOrNull { it.id == openId }

    BackHandler(enabled = open != null) { openId = -1L }

    if (open != null) {
        ChatScreen(thread = open, edit = edit, colors = c, onBack = { openId = -1L })
        return
    }

    val q = query.trim().lowercase()
    val lastByThread = allMessages.groupBy { it.threadId }
    val shown = if (q.isEmpty()) threads else threads.filter { t ->
        t.contactName.lowercase().contains(q) || lastByThread[t.id].orEmpty().any { it.text.lowercase().contains(q) }
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MessagesBlue, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
            Spacer(Modifier.weight(1f))
            if (edit) Icon(Icons.Filled.Add, "New message", tint = MessagesBlue, modifier = Modifier.size(44.dp).clickable { newThread = true }.padding(8.dp))
        }
        Text("Messages", color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))
        PhoneSearchField(query, { query = it }, "Search", c, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        if (shown.isEmpty()) {
            Text(
                if (q.isNotEmpty()) "No results" else if (edit) "No conversations yet. Tap + to start one." else "No messages",
                color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
                items(shown, key = { it.id }) { t ->
                    Column {
                        ThreadRow(t, lastByThread[t.id].orEmpty().lastOrNull(), c, edit, onClick = { openId = t.id }, onDelete = {
                            scope.launch { dao.deleteMessagesOfThread(t.id); dao.deleteThread(t.id) }
                        })
                        HorizontalDivider(Modifier.padding(start = 76.dp), color = c.line)
                    }
                }
            }
        }
    }

    if (newThread) {
        PhoneTextDialog(
            title = "New message", hint = "Who is it with?", initial = "", confirm = "Start",
            onSave = { name ->
                newThread = false
                scope.launch { openId = dao.insertThread(SmsThreadEntity(characterId = characterId, contactName = name.trim().ifBlank { "Unknown" })) }
            },
            onDismiss = { newThread = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadRow(t: SmsThreadEntity, last: SmsMessageEntity?, c: PhoneColors, edit: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { if (edit) menu = true }).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(t, 48.dp, c)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.contactName, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (last != null) Text(formatTime(last.sentAt), color = c.sub, fontSize = 14.sp)
                }
                Text(last?.text ?: "No messages", color = c.sub, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Delete conversation") }, onClick = { menu = false; onDelete() })
        }
    }
}

@Composable
private fun ContactAvatar(t: SmsThreadEntity, size: Dp, c: PhoneColors) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF9DA3AE)), contentAlignment = Alignment.Center) {
        if (t.avatarUri != null) AsyncImage(t.avatarUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(t.contactName.take(1).uppercase(), color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChatScreen(thread: SmsThreadEntity, edit: Boolean, colors: PhoneColors, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val dao = app.repository.sms
    val messages by remember(thread.id) { dao.observeMessages(thread.id) }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    var fromMe by remember { mutableStateOf(true) }
    val pickAvatar = rememberCropPicker(CropShape.CIRCLE) { path -> scope.launch { dao.updateThread(thread.copy(avatarUri = path)) } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().imePadding()) {
        // header: back, avatar, name
        Box(Modifier.fillMaxWidth().background(colors.card).padding(vertical = 6.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MessagesBlue, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(44.dp).clickable(onClick = onBack).padding(10.dp))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.clip(CircleShape).clickable { if (edit) pickAvatar() }) { ContactAvatar(thread, 44.dp, colors) }
                InlineField(
                    thread.contactName, { v -> scope.launch { dao.updateThread(thread.copy(contactName = v.trim())) } },
                    TextStyle(color = colors.text, fontSize = 12.sp, textAlign = TextAlign.Center), placeholder = "Name", singleLine = true, wrap = true, cursorColor = colors.text
                )
            }
        }
        HorizontalDivider(color = colors.line)

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            items(messages.size, key = { messages[it].id }) { i ->
                val m = messages[i]
                val prev = messages.getOrNull(i - 1)
                Column(Modifier.fillMaxWidth()) {
                    if (prev == null || m.sentAt - prev.sentAt > GapMillis) {
                        Text(formatDay(m.sentAt) + " " + formatTime(m.sentAt), color = colors.sub, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                    Bubble(
                        m, colors, edit,
                        onSwap = { scope.launch { dao.insertMessage(m.copy(fromMe = !m.fromMe)) } },
                        onDelete = { scope.launch { dao.deleteMessage(m.id) } }
                    )
                }
            }
        }

        // input bar: a real writing bar in Edit mode, a static one in View mode
        Row(Modifier.fillMaxWidth().background(colors.bg).navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (edit) {
                Text(
                    if (fromMe) "Me" else "Them", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (fromMe) MessagesBlue else Color(0xFF8E8E93)).clickable { fromMe = !fromMe }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(Modifier.size(8.dp))
            }
            Row(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(50)).background(colors.field).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (edit) {
                    BasicTextField(
                        value = draft, onValueChange = { draft = it },
                        textStyle = TextStyle(color = colors.text, fontSize = 16.sp), cursorBrush = SolidColor(colors.text),
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        decorationBox = { inner ->
                            Box {
                                if (draft.isEmpty()) Text("iMessage", style = TextStyle(color = colors.sub, fontSize = 16.sp))
                                inner()
                            }
                        }
                    )
                } else {
                    Text("iMessage", color = colors.sub, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            if (edit) {
                Spacer(Modifier.size(8.dp))
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(if (draft.isBlank()) colors.field else MessagesBlue).clickable(enabled = draft.isNotBlank()) {
                        val text = draft.trim()
                        draft = ""
                        scope.launch { dao.insertMessage(SmsMessageEntity(threadId = thread.id, text = text, fromMe = fromMe, sentAt = System.currentTimeMillis())) }
                    },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.ArrowUpward, "Send", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(m: SmsMessageEntity, colors: PhoneColors, edit: Boolean, onSwap: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val bg = if (m.fromMe) MessagesBlue else if (colors.bg == Color(0xFF000000)) Color(0xFF2C2C2E) else Color(0xFFE9E9EB)
    val fg = if (m.fromMe) Color.White else colors.text
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromMe) Arrangement.End else Arrangement.Start) {
        Box {
            Text(
                m.text, color = fg, fontSize = 16.sp,
                modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(18.dp)).background(bg)
                    .combinedClickable(onClick = {}, onLongClick = { if (edit) menu = true })
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Swap side") }, onClick = { menu = false; onSwap() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
            }
        }
    }
}
