package com.fakegram.profile.ui.shell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakegram.profile.data.CharacterEntity
import com.fakegram.profile.data.CharacterType
import com.fakegram.profile.data.Platforms
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.memberIdList
import com.fakegram.profile.data.platformList
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.ImageCropDialog
import java.time.LocalDate

// ------------------------------------------------------------------------------------------
// Header pill
// ------------------------------------------------------------------------------------------

/** The pill at the top of the shell: who is active. Tapping it opens the switcher. */
@Composable
fun CharacterPill(active: CharacterEntity?, palette: ShellPalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .glass(palette, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (active == null) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.text, modifier = Modifier.padding(start = 6.dp).size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add a character", color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        } else {
            CharacterAvatar(active.avatarUri, 30.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    active.name.ifBlank { "Unnamed" }, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (active.isGroup) "Group · ${active.memberIdList.size} member${if (active.memberIdList.size == 1) "" else "s"}"
                    else if (active.handle.isNotBlank()) "@${active.handle}" else "Character",
                    color = palette.sub, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.sub, modifier = Modifier.size(20.dp))
        }
    }
}

// ------------------------------------------------------------------------------------------
// Switcher sheet
// ------------------------------------------------------------------------------------------

@Composable
fun CharacterSwitcherSheet(shell: ShellViewModel, onDismiss: () -> Unit) {
    val characters by shell.characters.collectAsState()
    val active by shell.active.collectAsState()
    val profiles by shell.profiles.collectAsState()
    var editing by remember { mutableStateOf<CharacterEntity?>(null) }
    var creating by remember { mutableStateOf<String?>(null) }

    val individuals = characters.filter { !it.isGroup }
    val groups = characters.filter { it.isGroup }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 28.dp)
        ) {
            Text("Switch profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            if (characters.isEmpty()) {
                Text(
                    "No characters yet. Create an individual or a group to get started.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(4.dp)
                )
            }
            if (individuals.isNotEmpty()) SectionLabel("Individuals")
            individuals.forEach { c ->
                CharacterRow(c, selected = active?.id == c.id,
                    onPick = { shell.setActive(c.id); onDismiss() }, onEdit = { editing = c })
            }
            if (groups.isNotEmpty()) SectionLabel("Groups")
            groups.forEach { c ->
                CharacterRow(c, selected = active?.id == c.id,
                    onPick = { shell.setActive(c.id); onDismiss() }, onEdit = { editing = c })
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { creating = CharacterType.INDIVIDUAL }, modifier = Modifier.weight(1f)) { Text("+ Individual") }
                OutlinedButton(onClick = { creating = CharacterType.GROUP }, modifier = Modifier.weight(1f)) { Text("+ Group") }
            }
        }
    }

    creating?.let { type ->
        CharacterEditorDialog(
            existing = null, type = type, individuals = individuals, profiles = profiles,
            onDismiss = { creating = null },
            onSave = { shell.saveCharacter(null, it); creating = null },
            onDelete = null
        )
    }
    editing?.let { c ->
        CharacterEditorDialog(
            existing = c, type = c.type, individuals = individuals.filter { it.id != c.id }, profiles = profiles,
            onDismiss = { editing = null },
            onSave = { shell.saveCharacter(c, it); editing = null },
            onDelete = { shell.deleteCharacter(c.id); editing = null }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterRow(c: CharacterEntity, selected: Boolean, onPick: () -> Unit, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) ShellAccent.copy(alpha = 0.16f) else Color.Transparent, shape)
            .clickable(onClick = onPick).padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CharacterAvatar(c.avatarUri, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(c.name.ifBlank { "Unnamed" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (c.isGroup) "Group" else "Individual") + if (c.handle.isNotBlank()) " · @${c.handle}" else "",
                fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 3.dp)) {
                c.platformList.forEach { p ->
                    Text(
                        Platforms.label(p), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    )
                }
            }
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Active", tint = ShellAccent, modifier = Modifier.size(20.dp))
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp)) }
    }
}

// ------------------------------------------------------------------------------------------
// Editor
// ------------------------------------------------------------------------------------------

/** Creates ([existing] == null) or edits an individual character or a group. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterEditorDialog(
    existing: CharacterEntity?,
    type: String,
    individuals: List<CharacterEntity>,
    profiles: List<ProfileEntity>,
    onDismiss: () -> Unit,
    onSave: (CharacterEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val group = (existing?.type ?: type) == CharacterType.GROUP

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var handle by remember { mutableStateOf(existing?.handle.orEmpty()) }
    var about by remember { mutableStateOf(if (group) existing?.lore.orEmpty() else existing?.bio.orEmpty()) }
    var birthday by remember { mutableStateOf(existing?.birthday.orEmpty()) }
    var avatarPath by remember { mutableStateOf(existing?.avatarUri) }
    var platforms by remember { mutableStateOf(existing?.platformList?.toSet() ?: setOf(Platforms.INSTAGRAM)) }
    var members by remember { mutableStateOf(existing?.memberIdList?.toSet() ?: emptySet()) }
    var instagramId by remember { mutableStateOf(existing?.instagramProfileId) }
    var igMenu by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) cropUri = uri }
    cropUri?.let { uri ->
        ImageCropDialog(
            uri = uri, shape = CropShape.CIRCLE,
            onCancel = { cropUri = null },
            onConfirm = { bitmap ->
                cropUri = null
                MediaStorage.saveBitmapToInternal(context, bitmap)?.let { avatarPath = it }
            }
        )
    }

    val birthdayValid = birthday.isBlank() || runCatching { LocalDate.parse(birthday.trim()) }.isSuccess

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) (if (group) "New group" else "New character") else (if (group) "Edit group" else "Edit character")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        CharacterAvatar(avatarPath, 64.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(if (group) "Group avatar" else "Avatar", modifier = Modifier.weight(1f))
                    if (avatarPath != null) TextButton(onClick = { avatarPath = null }) { Text("Remove") }
                }
                OutlinedTextField(name, { name = it }, label = { Text(if (group) "Group name" else "Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(handle, { handle = it }, label = { Text("Handle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(about, { about = it }, label = { Text(if (group) "Lore / concept" else "Bio") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    birthday, { birthday = it }, label = { Text("Birthday (YYYY-MM-DD)") }, singleLine = true, isError = !birthdayValid,
                    supportingText = if (birthdayValid) null else ({ Text("Use the format 2003-08-31") }), modifier = Modifier.fillMaxWidth()
                )

                Text("Apps this character uses (only these get launch shortcuts)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Platforms.all.forEach { p ->
                        FilterChip(
                            selected = p.id in platforms,
                            onClick = { platforms = if (p.id in platforms) platforms - p.id else platforms + p.id },
                            label = { Text(p.label + if (p.built) "" else " (soon)") }
                        )
                    }
                }

                if (group) {
                    Text("Members", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (individuals.isEmpty()) {
                        Text("Create some individual characters first, then add them here.", fontSize = 13.sp)
                    }
                    individuals.forEach { c ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { members = if (c.id in members) members - c.id else members + c.id },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = c.id in members, onCheckedChange = { members = if (it) members + c.id else members - c.id })
                            CharacterAvatar(c.avatarUri, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(c.name)
                        }
                    }
                }

                Text("Instagram profile (existing)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    OutlinedButton(onClick = { igMenu = true }) {
                        val current = profiles.firstOrNull { it.id == instagramId }
                        Text(current?.let { "${it.displayName.ifBlank { it.username }} (@${it.username})" } ?: "None")
                    }
                    DropdownMenu(expanded = igMenu, onDismissRequest = { igMenu = false }) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { instagramId = null; igMenu = false })
                        profiles.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.displayName.ifBlank { p.username }} (@${p.username})") },
                                onClick = { instagramId = p.id; igMenu = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && birthdayValid,
                onClick = {
                    val base = existing ?: CharacterEntity(type = type)
                    onSave(
                        base.copy(
                            name = name.trim(),
                            handle = cleanHandle(handle),
                            bio = if (group) base.bio else about.trim(),
                            lore = if (group) about.trim() else base.lore,
                            avatarUri = avatarPath,
                            birthday = birthday.trim(),
                            activePlatforms = Platforms.all.map { it.id }.filter { it in platforms }.joinToString(","),
                            memberIds = if (group) members.joinToString(",") else base.memberIds,
                            instagramProfileId = instagramId
                        )
                    )
                }
            ) { Text(if (existing == null) "Create" else "Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${if (group) "group" else "character"}?") },
            text = { Text("\"${existing?.name}\" will be removed${if (group) "" else " from every group"}. Its Instagram profile and other content are kept.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
