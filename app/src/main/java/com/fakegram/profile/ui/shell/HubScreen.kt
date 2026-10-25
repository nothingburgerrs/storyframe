package com.fakegram.profile.ui.shell

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.fakegram.profile.data.CharacterEntity
import com.fakegram.profile.data.CharacterType
import com.fakegram.profile.data.Platforms
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.memberIdList
import com.fakegram.profile.data.vibeList
import com.fakegram.profile.media.MediaStorage
import java.time.LocalDate

/** Where each vibe-board photo sits (percent of the board width) and how it is tilted. Up to six. */
private class Slot(val l: Float, val t: Float, val w: Float, val r: Float)

private val SLOTS = listOf(
    Slot(4f, 3f, 44f, -6f), Slot(47f, 0f, 46f, 5f), Slot(2f, 44f, 40f, 4f),
    Slot(35f, 38f, 34f, -3f), Slot(63f, 45f, 34f, 7f), Slot(31f, 68f, 32f, -5f)
)

/** Tab 1 - a calm greeting, a few chips and soft tiles for the active character or group. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HubScreen(shell: ShellViewModel, palette: ShellPalette, onOpen: (String) -> Unit = {}, onOpenApps: () -> Unit = {}) {
    val active by shell.active.collectAsState()
    val characters by shell.characters.collectAsState()
    val profiles by shell.profiles.collectAsState()
    val loaded by shell.loaded.collectAsState()
    val events by shell.events.collectAsState()

    var editing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf<String?>(null) }
    var vibeDialog by remember { mutableStateOf(false) }

    val c = active
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 130.dp)
    ) {
        if (c == null) {
            if (loaded) {
                Text("Hey!", color = palette.text, fontFamily = ShellSerif, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Text("Create your first character to get started.", color = palette.sub, fontSize = 15.sp)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { creating = CharacterType.INDIVIDUAL }, colors = ButtonDefaults.buttonColors(containerColor = TileInk, contentColor = Color.White)) { Text("+ Character") }
                    OutlinedButton(onClick = { creating = CharacterType.GROUP }) { Text("+ Group", color = palette.text) }
                }
            }
        } else {
            Text("Hey, ${c.name}!", color = palette.text, fontFamily = ShellSerif, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
            Text(
                if (c.isGroup) "What is the group up to?" else (c.bio.ifBlank { "What is ${c.name} up to?" }),
                color = palette.sub, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 2
            )

            val groups = if (c.isGroup) emptyList() else characters.filter { it.isGroup && c.id in it.memberIdList }
            val birthday = daysUntilBirthday(c.birthday)
            SectionLabel("Details", palette, action = "Edit") { editing = true }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (c.handle.isNotBlank()) Chip("@${c.handle}", palette) { editing = true }
                if (c.isGroup) Chip("Group", palette) { editing = true }
                if (birthday != null) Chip(if (birthday == 0) "Birthday today" else "Birthday in $birthday d", palette) { editing = true }
                groups.forEach { g -> Chip(g.name, palette) { shell.setActive(g.id) } }
            }

            Spacer(Modifier.height(22.dp))
            val todayKey = LocalDate.now().toString()
            val upcoming = events.filter { e -> e.date >= todayKey && (e.characterId == c.id || (c.isGroup && e.characterId in c.memberIdList)) }
                .minByOrNull { it.date }
            val nextBirthday = upcomingBirthdays(characters.filter { it.id == c.id || (c.isGroup && it.id in c.memberIdList) }).firstOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(280.dp)) {
                Tile(
                    modifier = Modifier.weight(1.1f).fillMaxHeight(), color = TileLavender, onClick = { onOpen(ShellRoutes.CALENDAR) },
                    icon = Icons.Filled.CalendarMonth, title = "Calendar",
                    sub = upcoming?.let { "${monthDayName(LocalDate.parse(it.date))} · ${Platforms.label(it.platform)}" }
                        ?: nextBirthday?.let { "${it.character.name}'s birthday" } ?: "Nothing planned"
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(weekdayName(), color = TileInk, fontFamily = ShellSerif, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    Text(monthDayName(), color = TileInk.copy(alpha = 0.55f), fontFamily = ShellSerif, fontSize = 18.sp, maxLines = 1)
                    Spacer(Modifier.height(18.dp))
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(
                        modifier = Modifier.weight(1f).fillMaxWidth(), color = TileBlush, onClick = { vibeDialog = true },
                        icon = Icons.Filled.Image, title = "Vibe board", sub = "${c.vibeList.size} photos"
                    )
                    Tile(
                        modifier = Modifier.weight(1f).fillMaxWidth(), color = TileInk, dark = true, onClick = { editing = true },
                        icon = Icons.Filled.Edit, title = "Edit ${if (c.isGroup) "group" else "character"}"
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(TileInk).clickable(onClick = onOpenApps).padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Open ${c.name}'s apps", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1)
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(TileLavender), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TileInk)
                }
            }

            if (c.isGroup) {
                val members = c.memberIdList.mapNotNull { id -> characters.firstOrNull { it.id == id } }
                SectionLabel("Members", palette, trailing = "${members.size}")
                if (members.isEmpty()) {
                    SoftCard("No members yet. Tap to add some.", palette, Modifier.clickable { editing = true })
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        items(members, key = { it.id }) { m ->
                            Column(
                                modifier = Modifier.width(112.dp).glass(palette, RoundedCornerShape(24.dp)).clickable { shell.setActive(m.id) }
                                    .padding(top = 16.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CharacterAvatar(m.avatarUri, 64.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(m.name, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(if (m.handle.isNotBlank()) "@${m.handle}" else "Character", color = palette.sub, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            val vibe = c.vibeList.take(SLOTS.size)
            if (vibe.isNotEmpty()) {
                SectionLabel("Vibe board", palette, action = "Edit") { vibeDialog = true }
                VibeBoard(vibe, Modifier.clickable { vibeDialog = true })
            }
        }
    }

    creating?.let { type ->
        CharacterEditorDialog(
            existing = null, type = type, individuals = characters.filter { !it.isGroup }, profiles = profiles,
            onDismiss = { creating = null }, onSave = { shell.saveCharacter(null, it); creating = null }, onDelete = null
        )
    }
    if (editing && c != null) {
        CharacterEditorDialog(
            existing = c, type = c.type, individuals = characters.filter { !it.isGroup && it.id != c.id }, profiles = profiles,
            onDismiss = { editing = false }, onSave = { shell.saveCharacter(c, it); editing = false },
            onDelete = { shell.deleteCharacter(c.id); editing = false }
        )
    }
    if (vibeDialog && c != null) {
        VibeDialog(c, onDismiss = { vibeDialog = false }) { list -> shell.edit(c.id) { it.copy(vibe = list.joinToString("|")) } }
    }
}

@Composable
private fun Chip(text: String, palette: ShellPalette, dim: Boolean = false, onClick: () -> Unit) {
    Text(
        text, color = if (dim) palette.sub else palette.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(palette.chip).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

/** Rounded colour tile with a white icon bubble top-left and a label at the bottom. */
@Composable
private fun Tile(
    modifier: Modifier, color: Color, onClick: () -> Unit, icon: ImageVector, title: String, sub: String = "", dark: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val ink = if (dark) Color.White else TileInk
    Column(modifier = modifier.clip(RoundedCornerShape(26.dp)).background(color).clickable(onClick = onClick).padding(16.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = TileInk, modifier = Modifier.size(20.dp))
        }
        content()
        if (title.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            Text(title, color = ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (sub.isNotEmpty()) Text(sub, color = ink.copy(alpha = 0.6f), fontSize = 12.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SectionLabel(text: String, palette: ShellPalette, trailing: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 26.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = palette.sub, fontSize = 12.sp)
        if (action != null) Text(action, color = palette.sub, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onAction).padding(2.dp))
    }
}

@Composable
private fun SoftCard(text: String, palette: ShellPalette, modifier: Modifier = Modifier) {
    Text(
        text, color = palette.sub, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().glass(palette, RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 22.dp)
    )
}

/** Tilted polaroid-style photos arranged into a cluster. */
@Composable
private fun VibeBoard(photos: List<String>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp).aspectRatio(1f / 1.14f)) {
        val w = maxWidth
        photos.forEachIndexed { i, uri ->
            val s = SLOTS[i]
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier.offset(x = w * (s.l / 100f), y = w * (s.t / 100f)).width(w * (s.w / 100f)).aspectRatio(0.8f)
                    .zIndex(if (i % 2 == 1) 2f else 1f).rotate(s.r)
                    .shadow(6.dp, shape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                    .background(Color.White, shape).padding(3.dp).clip(RoundedCornerShape(11.dp))
            ) { AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VibeDialog(character: CharacterEntity, onDismiss: () -> Unit, onChange: (List<String>) -> Unit) {
    val context = LocalContext.current
    val photos = character.vibeList
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(SLOTS.size)) { uris ->
        val room = SLOTS.size - photos.size
        val added = uris.take(room).mapNotNull { MediaStorage.copyToInternal(context, it) }
        if (added.isNotEmpty()) onChange(photos + added)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vibe board") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Up to ${SLOTS.size} photos. They are arranged into a tilted cluster.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.forEachIndexed { i, uri ->
                        Box(Modifier.size(width = 76.dp, height = 95.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Color(0x99000000))
                                    .clickable { onChange(photos.filterIndexed { idx, _ -> idx != i }) },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(15.dp)) }
                        }
                    }
                    if (photos.size < SLOTS.size) {
                        Box(
                            Modifier.size(width = 76.dp, height = 95.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Add, contentDescription = "Add photos") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
