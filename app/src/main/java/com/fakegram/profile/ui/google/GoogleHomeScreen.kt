package com.fakegram.profile.ui.google

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.PanelEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.ui.phone.LocalEditMode

/** Does [q] find this panel? The text it shows in the search bar, or its title, either way round. */
private fun PanelEntity.matches(q: String): Boolean {
    val n = q.trim().lowercase()
    if (n.isEmpty()) return false
    val a = query.trim().lowercase()
    val t = title.trim().lowercase()
    return (a.isNotEmpty() && (a.contains(n) || n.contains(a))) || (t.isNotEmpty() && (t.contains(n) || n.contains(t)))
}

private fun PanelEntity.isExact(q: String): Boolean {
    val n = q.trim().lowercase()
    return query.trim().lowercase() == n || title.trim().lowercase() == n
}

/**
 * The Google app. Type anything you set up in Edit mode ("rom.com", an idol's name...) and its results page
 * opens. In Edit mode the panels you have made are listed here too, with + to make a new one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoogleHomeScreen(repository: ProfileRepository, onOpenPanel: (Long) -> Unit, onBack: () -> Unit) {
    val vm: PanelsHomeViewModel = viewModel(factory = PanelsHomeViewModel.factory(repository))
    val panels by vm.panels.collectAsState()
    val covers by vm.covers.collectAsState()
    val app = LocalContext.current.applicationContext as StoryframeApp
    val dark by app.themePreferences.isDarkMode.collectAsState(initial = true)
    val edit = LocalEditMode.current

    val bg = if (dark) Color(0xFF202124) else Color.White
    val ink = if (dark) Color(0xFFE8EAED) else Color(0xFF202124)
    val sub = if (dark) Color(0xFF9AA0A6) else Color(0xFF70757A)
    val bar = if (dark) Color(0xFF303134) else Color.White
    val barLine = if (dark) Color(0xFF303134) else Color(0xFFDFE1E5)

    var query by rememberSaveable { mutableStateOf("") }
    var missed by rememberSaveable { mutableStateOf<String?>(null) }

    val matches = panels.filter { it.matches(query) }.sortedBy { if (it.isExact(query)) 0 else 1 }

    fun submit() {
        val first = matches.firstOrNull()
        if (first != null) onOpenPanel(first.id) else if (query.isNotBlank()) missed = query.trim()
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ink, modifier = Modifier.size(44.dp).clickable(onClick = onBack).padding(10.dp))
            }
            Spacer(Modifier.height(if (query.isBlank()) 36.dp else 8.dp))
            if (query.isBlank()) {
                Text(
                    buildAnnotatedString {
                        val letters = listOf("G" to 0xFF4285F4, "o" to 0xFFEA4335, "o" to 0xFFFBBC05, "g" to 0xFF4285F4, "l" to 0xFF34A853, "e" to 0xFFEA4335)
                        letters.forEach { (ch, color) -> withStyle(SpanStyle(color = Color(color))) { append(ch) } }
                    },
                    fontSize = 52.sp, fontWeight = FontWeight.Medium, modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(28.dp))
            }

            // the search bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp).clip(RoundedCornerShape(26.dp))
                    .background(bar).border(1.dp, barLine, RoundedCornerShape(26.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = sub, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = query, onValueChange = { query = it; missed = null }, singleLine = true,
                    textStyle = TextStyle(color = ink, fontSize = 17.sp), cursorBrush = SolidColor(ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text("Search", style = TextStyle(color = sub, fontSize = 17.sp))
                            inner()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    Icon(Icons.Filled.Close, "Clear", tint = sub, modifier = Modifier.size(22.dp).clickable { query = ""; missed = null })
                } else {
                    Icon(Icons.Filled.Mic, null, tint = sub, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            val missedText = missed
            if (missedText != null) {
                Text(
                    "Your search - $missedText - did not match any documents.", color = ink, fontSize = 15.sp,
                    modifier = Modifier.padding(20.dp)
                )
            }

            if (query.isNotBlank()) {
                // suggestions
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    matches.take(8).forEach { p ->
                        SuggestionRow(Icons.Filled.History, p.query.ifBlank { p.title }, ink, sub) { onOpenPanel(p.id) }
                    }
                    SuggestionRow(Icons.Filled.Search, query.trim(), ink, sub) { submit() }
                }
            } else if (edit) {
                Text("Your panels", color = sub, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 6.dp))
                if (panels.isEmpty()) {
                    Text("Nothing yet. Tap + to make a search result page.", color = sub, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 90.dp)) {
                    panels.forEach { p ->
                        PanelListRow(p, covers[p.id], ink, sub, onOpen = { onOpenPanel(p.id) }, onDuplicate = { vm.duplicatePanel(p.id) }, onDelete = { vm.deletePanel(p.id) })
                    }
                }
            }
        }

        if (edit && query.isBlank()) {
            FloatingActionButton(
                onClick = { vm.createPanel { id -> onOpenPanel(id) } },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)
            ) { Icon(Icons.Filled.Add, "New panel") }
        }
    }
}

@Composable
private fun SuggestionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, ink: Color, sub: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = sub, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = ink, fontSize = 16.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PanelListRow(p: PanelEntity, cover: String?, ink: Color, sub: Color, onOpen: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = { menu = true }).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFBDC1C6)), contentAlignment = Alignment.Center) {
                if (cover != null) AsyncImage(cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.Search, null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.title.ifBlank { "Untitled" }, color = ink, fontSize = 16.sp, maxLines = 1)
                Text(p.query.ifBlank { "No search text yet" }, color = sub, fontSize = 13.sp, maxLines = 1)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
        }
    }
}
