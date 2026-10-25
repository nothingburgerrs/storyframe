package com.fakegram.profile.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fakegram.profile.data.FollowedByEntity
import com.fakegram.profile.data.HighlightEntity
import com.fakegram.profile.data.NoteType
import com.fakegram.profile.data.PillIcon
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.data.StoryRingType
import com.fakegram.profile.ui.components.pillIconVector

/** Single text field, Cancel/OK — used for stats, website, location, and followed-by usernames. */
@Composable
fun SimpleTextDialog(
    title: String,
    initialValue: String,
    label: String = title,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** The consolidated "Edit profile" dialog: story-ring dropdown, name fields, bio, avatar. */
@Composable
fun EditProfileDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (
        storyRingType: StoryRingType,
        displayName: String,
        username: String,
        bio: String,
        newAvatarPath: String?,
        clearAvatar: Boolean
    ) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var ringType by remember { mutableStateOf(profile.storyRingType) }
    var ringMenuExpanded by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var pickedAvatarPath by remember { mutableStateOf<String?>(null) }
    var avatarCleared by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropUri = uri
    }

    cropUri?.let { uri ->
        com.fakegram.profile.ui.components.ImageCropDialog(
            uri = uri,
            shape = com.fakegram.profile.ui.components.CropShape.CIRCLE,
            onCancel = { cropUri = null },
            onConfirm = { bitmap ->
                cropUri = null
                val path = com.fakegram.profile.media.MediaStorage.saveBitmapToInternal(context, bitmap)
                if (path != null) {
                    pickedAvatarPath = path
                    avatarCleared = false
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column {
                Box {
                    OutlinedButton(onClick = { ringMenuExpanded = true }) {
                        Text(ringType.name + " ▽")
                    }
                    DropdownMenu(expanded = ringMenuExpanded, onDismissRequest = { ringMenuExpanded = false }) {
                        StoryRingType.values().forEach { type ->
                            DropdownMenuItem(text = { Text(type.name) }, onClick = { ringType = type; ringMenuExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDDDDDD))
                            .clickable {
                                avatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        val showPath = pickedAvatarPath
                        when {
                            showPath != null -> AsyncImage(model = showPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp))
                            !avatarCleared && profile.avatarUri != null -> AsyncImage(model = profile.avatarUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp))
                            else -> Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Change Avatar", modifier = Modifier.weight(1f))
                    if (pickedAvatarPath != null || (!avatarCleared && profile.avatarUri != null)) {
                        IconButton(onClick = { pickedAvatarPath = null; avatarCleared = true }) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Clear avatar", tint = Color(0xFFED4956))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(ringType, displayName, username, bio, pickedAvatarPath, avatarCleared)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun NoteEditDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (NoteType, String?, String?, String?) -> Unit
) {
    var type by remember { mutableStateOf(profile.noteType) }
    var text by remember { mutableStateOf(profile.noteText.orEmpty()) }
    var song by remember { mutableStateOf(profile.noteSongTitle.orEmpty()) }
    var artist by remember { mutableStateOf(profile.noteArtist.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note") },
        text = {
            Column {
                listOf(NoteType.NONE to "None", NoteType.TEXT to "Text", NoteType.MUSIC to "Music").forEach { (t, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(label)
                    }
                }
                if (type == NoteType.TEXT) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Note text") }, singleLine = true)
                } else if (type == NoteType.MUSIC) {
                    OutlinedTextField(value = song, onValueChange = { song = it }, label = { Text("Song title") }, singleLine = true)
                    OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(type, text.ifBlank { null }, song.ifBlank { null }, artist.ifBlank { null })
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PillEditDialog(
    initialText: String,
    initialIcon: PillIcon?,
    initialShowIcon: Boolean,
    isEditingExisting: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, PillIcon?, Boolean) -> Unit,
    onDelete: () -> Unit = {}
) {
    var text by remember { mutableStateOf(initialText) }
    var icon by remember { mutableStateOf(initialIcon) }
    var showIcon by remember { mutableStateOf(initialShowIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pill") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showIcon, onCheckedChange = { showIcon = it })
                    Text("Show icon")
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                ) {
                    items(PillIcon.values().toList()) { pi ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .background(
                                    if (icon == pi) Color(0xFFCCE5FF) else Color(0xFFEFEFEF),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { icon = pi },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(pillIconVector(pi), contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (isEditingExisting) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) { Text("Delete pill", color = Color(0xFFED4956)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text, icon, showIcon) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun HighlightEditDialog(
    existing: HighlightEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onDelete: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var pickedPath by remember { mutableStateOf<String?>(null) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropUri = uri
    }

    cropUri?.let { uri ->
        com.fakegram.profile.ui.components.ImageCropDialog(
            uri = uri,
            shape = com.fakegram.profile.ui.components.CropShape.CIRCLE,
            onCancel = { cropUri = null },
            onConfirm = { bitmap ->
                cropUri = null
                pickedPath = com.fakegram.profile.media.MediaStorage.saveBitmapToInternal(context, bitmap)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add highlight" else "Edit highlight") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEFEFEF))) {
                        val showPath = pickedPath ?: existing?.coverUri
                        if (showPath != null) {
                            AsyncImage(model = showPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(onClick = {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Pick cover image")
                    }
                }
                if (existing != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) { Text("Delete highlight", color = Color(0xFFED4956)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, pickedPath) }) { Text(if (existing == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun FollowedByManagerDialog(
    entries: List<FollowedByEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, Uri?) -> Unit,
    onEditUsername: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onMove: (FollowedByEntity, Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FollowedByEntity?>(null) }
    val sorted = entries.sortedBy { it.position }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Followed by") },
        text = {
            Column {
                if (sorted.isEmpty()) {
                    Text("No accounts yet.")
                }
                sorted.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFEFEFEF))) {
                            if (entry.avatarUri != null) {
                                AsyncImage(model = entry.avatarUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(entry.username, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onMove(entry, -1) }, enabled = index > 0) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                        }
                        IconButton(onClick = { onMove(entry, 1) }, enabled = index < sorted.size - 1) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                        }
                        IconButton(onClick = { editingEntry = entry }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onDelete(entry.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showAddDialog = true }) { Text("+ Add account") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showAddDialog) {
        AddFollowedByEntryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { username, uri -> showAddDialog = false; onAdd(username, uri) }
        )
    }
    editingEntry?.let { entry ->
        SimpleTextDialog(
            title = "Edit username",
            initialValue = entry.username,
            onDismiss = { editingEntry = null },
            onConfirm = { value -> editingEntry = null; onEditUsername(entry.id, value) }
        )
    }
}

@Composable
private fun AddFollowedByEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedUri = uri
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add account") },
        text = {
            Column {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text(if (pickedUri == null) "Pick avatar (optional)" else "Avatar selected")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (username.isNotBlank()) onConfirm(username, pickedUri) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SettingsSheetContent(
    profile: ProfileEntity,
    onToggleVerified: (Boolean) -> Unit,
    onToggleActiveStory: (Boolean) -> Unit,
    onToggleShowReels: (Boolean) -> Unit,
    onToggleShowReposts: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(all = 16.dp)) {
        Text("Settings", modifier = Modifier.padding(all = 0.dp))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Verified badge")
            Switch(checked = profile.isVerified, onCheckedChange = onToggleVerified)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Active story")
            Switch(checked = profile.hasActiveStory, onCheckedChange = onToggleActiveStory)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Show reels tab")
            Switch(checked = profile.showReelsTab, onCheckedChange = onToggleShowReels)
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Show reposts tab")
            Switch(checked = profile.showRepostsTab, onCheckedChange = onToggleShowReposts)
        }
        Spacer(Modifier.height(8.dp))
    }
}
