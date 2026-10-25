package com.fakegram.profile.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.NoteEntity
import com.fakegram.profile.ui.shell.LocalEditMode
import kotlinx.coroutines.launch

private val NotesYellow = Color(0xFFE6A700)

/** A fake Notes app. Edit mode writes and deletes notes; View mode only reads and searches them. */
@Composable
fun NotesScreen(characterId: Long, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val dao = app.repository.notes
    val notes by remember(characterId) { dao.observe(characterId) }.collectAsState(initial = emptyList())
    val edit = LocalEditMode.current
    val c = phoneColors()
    val scope = rememberCoroutineScope()
    var openId by rememberSaveable { mutableStateOf(-1L) }
    var query by rememberSaveable { mutableStateOf("") }
    val open = notes.firstOrNull { it.id == openId }

    BackHandler(enabled = open != null) { openId = -1L }

    if (open != null) {
        NoteEditor(
            note = open, edit = edit, colors = c,
            onChange = { title, body -> scope.launch { dao.update(open.copy(title = title, body = body, updatedAt = System.currentTimeMillis())) } },
            onDelete = { scope.launch { dao.deleteById(open.id); openId = -1L } },
            onBack = { openId = -1L }
        )
        return
    }

    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) notes else notes.filter { it.title.lowercase().contains(q) || it.body.lowercase().contains(q) }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NotesYellow, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
            Spacer(Modifier.weight(1f))
            if (edit) {
                Icon(
                    Icons.Filled.Add, "New note", tint = NotesYellow,
                    modifier = Modifier.size(44.dp).clickable { scope.launch { openId = dao.insert(NoteEntity(characterId = characterId)) } }.padding(8.dp)
                )
            }
        }
        Text("Notes", color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))
        PhoneSearchField(query, { query = it }, "Search", c, Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(14.dp))
        if (shown.isEmpty()) {
            Text(
                if (q.isNotEmpty()) "No results" else if (edit) "No notes yet. Tap + to write one." else "No notes",
                color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp).navigationBarsPadding()) {
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card)) {
                        shown.forEachIndexed { i, n ->
                            if (i > 0) HorizontalDivider(Modifier.padding(start = 16.dp), color = c.line)
                            Column(Modifier.fillMaxWidth().clickable { openId = n.id }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(n.title.ifBlank { "New Note" }, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row {
                                    Text(formatDay(n.updatedAt), color = c.text, fontSize = 14.sp)
                                    Spacer(Modifier.size(10.dp))
                                    Text(
                                        n.body.lineSequence().firstOrNull { it.isNotBlank() } ?: "No additional text",
                                        color = c.sub, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(note: NoteEntity, edit: Boolean, colors: PhoneColors, onChange: (String, String) -> Unit, onDelete: () -> Unit, onBack: () -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NotesYellow, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
            Spacer(Modifier.weight(1f))
            if (edit) Icon(Icons.Filled.Delete, "Delete note", tint = NotesYellow, modifier = Modifier.size(44.dp).clickable(onClick = onDelete).padding(10.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(formatDay(note.updatedAt) + ", " + formatTime(note.updatedAt), color = colors.sub, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            NoteField(title, edit, "Title", TextStyle(color = colors.text, fontSize = 26.sp, fontWeight = FontWeight.Bold), colors) { title = it; onChange(title, body) }
            Spacer(Modifier.height(8.dp))
            NoteField(body, edit, "Note", TextStyle(color = colors.text, fontSize = 17.sp, lineHeight = 24.sp), colors) { body = it; onChange(title, body) }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun NoteField(value: String, edit: Boolean, hint: String, style: TextStyle, colors: PhoneColors, onValue: (String) -> Unit) {
    BasicTextField(
        value = value, onValueChange = onValue, readOnly = !edit, textStyle = style, cursorBrush = SolidColor(NotesYellow),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            androidx.compose.foundation.layout.Box {
                if (value.isEmpty() && edit) Text(hint, style = style.copy(color = colors.sub))
                inner()
            }
        }
    )
}
