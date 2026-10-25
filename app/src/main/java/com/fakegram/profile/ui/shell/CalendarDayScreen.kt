package com.fakegram.profile.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.data.Platforms
import com.fakegram.profile.data.ScheduleEventEntity
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.memberIdList
import com.fakegram.profile.data.platformList
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.rememberCropPicker
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import java.time.LocalDate

/** One day: everything planned for it, edited in place. The + in the corner adds an entry. */
@Composable
fun CalendarDayScreen(shell: ShellViewModel, date: String, onBack: () -> Unit) {
    val palette = shellPalette(LocalStoryframeColors.current.isDark)
    val active by shell.active.collectAsState()
    val characters by shell.characters.collectAsState()
    val events by shell.events.collectAsState()
    val ownerIds = rememberCalendarOwners(shell)
    val c = active
    val day = remember(date) { runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()) }

    val list = remember(events, ownerIds, date) { events.filter { it.characterId in ownerIds && it.date == date }.sortedByDescending { it.id } }
    val names = remember(characters, ownerIds) { characters.filter { it.id in ownerIds }.associate { it.id to it.name } }
    val birthdays = remember(characters, ownerIds, date) { characters.filter { it.id in ownerIds && it.birthday.length >= 10 && it.birthday.substring(5) == date.substring(5) } }

    ShellBackground(palette) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.text) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onBack) { Icon(Icons.Filled.Check, "Done", tint = palette.text) }
                IconButton(
                    onClick = {
                        if (c != null) {
                            val first = c.platformList.firstOrNull { it in Platforms.postable } ?: Platforms.INSTAGRAM
                            shell.addEvent(ScheduleEventEntity(characterId = c.id, date = date, platform = first))
                        }
                    }
                ) { Icon(Icons.Filled.Add, "Add", tint = palette.text) }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
                Text(weekdayName(day), color = palette.text, fontFamily = ShellSerif, fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
                Text("${monthDayName(day)}, ${day.year}", color = palette.sub, fontFamily = ShellSerif, fontSize = 22.sp)
                Spacer(Modifier.height(20.dp))

                birthdays.forEach { b ->
                    Text(
                        "🎂  ${b.name}'s birthday", color = TileInk, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(TileBlush).padding(16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (list.isEmpty() && birthdays.isEmpty()) {
                    Text("Nothing planned. Tap + to add something to this day.", color = palette.sub, fontSize = 15.sp)
                }
                list.forEach { e ->
                    EventCard(e, null, palette, onChange = { shell.updateEvent(it) }, onDelete = { shell.deleteEvent(e.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventCard(e: ScheduleEventEntity, who: String?, palette: ShellPalette, onChange: (ScheduleEventEntity) -> Unit, onDelete: () -> Unit) {
    val pickPhoto = rememberCropPicker(CropShape.RECT_4_5) { onChange(e.copy(imageUri = it)) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.glass).padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(width = 64.dp, height = 80.dp).clip(StampShape(2.5f)).background(palette.chip).clickable(onClick = pickPhoto),
            contentAlignment = Alignment.Center
        ) {
            if (e.imageUri != null) AsyncImage(e.imageUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Filled.AddPhotoAlternate, "Add photo", tint = palette.sub)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Platforms.all.filter { it.id in Platforms.postable || it.id == e.platform }.forEach { p ->
                    val on = e.platform == p.id
                    Text(
                        p.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, color = if (on) Color.White else palette.text,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (on) TileInk else palette.chip)
                            .clickable { onChange(e.copy(platform = p.id)) }.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            NoteField(e.id, e.note, { onChange(e.copy(note = it)) }, palette)
            if (who != null) Text(who, color = palette.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            if (e.auto) Text("Placed by the weekly goal. Edit it to keep it.", color = palette.sub, fontSize = 11.5.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Icon(Icons.Filled.Delete, "Delete", tint = palette.sub, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onDelete).padding(6.dp))
    }
}

/** A note you type directly into the card. It is saved on every keystroke, so nothing is lost when you leave. */
@Composable
private fun NoteField(key: Long, value: String, onCommit: (String) -> Unit, palette: ShellPalette) {
    // Keyed by the entry, not its text, so the database echoing the text back never moves your cursor.
    var text by remember(key) { mutableStateOf(value) }
    val style = TextStyle(color = palette.text, fontSize = 16.sp, lineHeight = 22.sp)
    BasicTextField(
        value = text, onValueChange = { text = it; onCommit(it) }, textStyle = style, cursorBrush = SolidColor(palette.text),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner -> Box { if (text.isEmpty()) Text("Write a note...", style = style.copy(color = palette.sub)); inner() } }
    )
}
