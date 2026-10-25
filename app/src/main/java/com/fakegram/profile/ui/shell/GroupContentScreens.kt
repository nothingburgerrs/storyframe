package com.fakegram.profile.ui.shell

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakegram.profile.data.BubbleMessageKind
import com.fakegram.profile.data.CharacterEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.memberIdList
import kotlinx.coroutines.launch

/** The group itself first, then each member - everyone who can have a Google search / chat. */
private fun owners(group: CharacterEntity, all: List<CharacterEntity>): List<CharacterEntity> =
    listOf(group) + group.memberIdList.mapNotNull { id -> all.firstOrNull { it.id == id } }

/** A group's Google searches: its own panel plus one per member. A missing one is created when tapped. */
@Composable
fun GroupPanelsScreen(
    shell: ShellViewModel,
    repository: ProfileRepository,
    groupId: Long,
    onOpenPanel: (Long) -> Unit,
    onBack: () -> Unit
) {
    val characters by shell.characters.collectAsState()
    val panels by repository.panels.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val group = characters.firstOrNull { it.id == groupId }

    OwnerListScaffold(title = "${group?.name.orEmpty()} · Google", onBack = onBack) {
        if (group == null) return@OwnerListScaffold
        items(owners(group, characters), key = { it.id }) { owner ->
            val panel = panels.firstOrNull { it.characterId == owner.id }
            OwnerRow(
                owner = owner,
                subtitle = panel?.let { listOf(it.title, it.subtitle).filter { s -> s.isNotBlank() }.joinToString(" · ") },
                emptyHint = "No Google search yet. Tap to create one.",
                onClick = { scope.launch { onOpenPanel(shell.panelIdFor(owner)) } }
            )
        }
    }
}

/** A group's Bubble chats: its own plus one per member. A missing one is created when tapped. */
@Composable
fun GroupChatsScreen(
    shell: ShellViewModel,
    repository: ProfileRepository,
    groupId: Long,
    onOpenChat: (Long) -> Unit,
    onBack: () -> Unit
) {
    val characters by shell.characters.collectAsState()
    val chats by repository.bubbleChats.collectAsState(initial = emptyList())
    val messages by repository.allBubbleMessages.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val group = characters.firstOrNull { it.id == groupId }

    OwnerListScaffold(title = "${group?.name.orEmpty()} · Bubble", onBack = onBack) {
        if (group == null) return@OwnerListScaffold
        items(owners(group, characters), key = { it.id }) { owner ->
            val chat = chats.firstOrNull { it.characterId == owner.id }
            val last = chat?.let { c -> messages.filter { it.chatId == c.id }.maxByOrNull { it.position } }
            OwnerRow(
                owner = owner,
                subtitle = last?.let { if (it.kind == BubbleMessageKind.IMAGE) "Photo" else it.text }?.takeIf { it.isNotBlank() }
                    ?: chat?.let { "No messages yet" },
                emptyHint = "No chat yet. Tap to start one.",
                onClick = { scope.launch { onOpenChat(shell.chatIdFor(owner)) } }
            )
        }
    }
}

@Composable
private fun OwnerListScaffold(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        }
    }
}

@Composable
private fun OwnerRow(owner: CharacterEntity, subtitle: String?, emptyHint: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CharacterAvatar(owner.avatarUri, 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(owner.name.ifBlank { "Unnamed" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (owner.isGroup) {
                    Text(
                        "  Group", fontSize = 12.sp, color = ShellAccent, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(emptyHint, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
