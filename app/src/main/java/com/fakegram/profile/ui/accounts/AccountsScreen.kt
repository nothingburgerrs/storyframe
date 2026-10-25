package com.fakegram.profile.ui.accounts

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fakegram.profile.R
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.AccountEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.media.MediaStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    application: Application,
    private val repository: ProfileRepository
) : AndroidViewModel(application) {

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    /** Creates or updates an account; a newly picked avatar is copied into app storage first. */
    fun save(existing: AccountEntity?, username: String, verified: Boolean, newAvatar: Uri?) {
        viewModelScope.launch {
            val path = newAvatar?.let { MediaStorage.copyToInternal(getApplication(), it) }
            if (existing == null) {
                repository.addAccount(AccountEntity(username = username, avatarUri = path, isVerified = verified))
            } else {
                repository.updateAccount(existing.copy(username = username, isVerified = verified, avatarUri = path ?: existing.avatarUri))
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteAccount(id) }
    }

    companion object {
        fun factory(application: Application, repository: ProfileRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountsViewModel(application, repository) as T
            }
    }
}

/** The "Accounts" tab: your library of placeholder accounts. */
@Composable
fun AccountsScreen(repository: ProfileRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as StoryframeApp
    val viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.factory(app, repository))
    val accounts by viewModel.accounts.collectAsState()
    val isDark by app.themePreferences.isDarkMode.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    var editing by remember { mutableStateOf<AccountEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { scope.launch { app.themePreferences.setDarkMode(!isDark) } }) {
                        Icon(if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = "Toggle theme")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, contentDescription = "New account") }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Save placeholder accounts here, then pick them for comments, \"followed by\" and more.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(accounts, key = { it.id }) { account ->
                    AccountRow(account, Modifier.clickable { editing = account })
                }
            }
        }
    }

    if (creating) {
        AccountEditDialog(
            existing = null,
            onDismiss = { creating = false },
            onSave = { name, verified, avatar -> viewModel.save(null, name, verified, avatar); creating = false },
            onDelete = null
        )
    }
    editing?.let { account ->
        AccountEditDialog(
            existing = account,
            onDismiss = { editing = null },
            onSave = { name, verified, avatar -> viewModel.save(account, name, verified, avatar); editing = null },
            onDelete = { viewModel.delete(account.id); editing = null }
        )
    }
}

@Composable
fun AccountAvatar(uri: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(Color(0xFF8E8E8E).copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Filled.Person, null, tint = Color(0xFF8E8E8E), modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun AccountRow(account: AccountEntity, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccountAvatar(account.avatarUri, 44.dp)
        Spacer(Modifier.width(12.dp))
        Text(account.username.ifBlank { "(no name)" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (account.isVerified) {
            Spacer(Modifier.width(4.dp))
            Icon(painterResource(R.drawable.ic_badge_verified), null, tint = Color.Unspecified, modifier = Modifier.size(15.dp))
        }
    }
}

/** Pick one of the saved accounts; shows a hint when the library is still empty. */
@Composable
fun AccountPickerDialog(accounts: List<AccountEntity>, onPick: (AccountEntity) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved accounts") },
        text = {
            if (accounts.isEmpty()) {
                Text("No saved accounts yet. Add some in the Accounts tab.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(accounts, key = { it.id }) { a -> AccountRow(a, Modifier.clickable { onPick(a) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AccountEditDialog(
    existing: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Uri?) -> Unit,
    onDelete: (() -> Unit)?
) {
    var username by remember(existing?.id) { mutableStateOf(existing?.username.orEmpty()) }
    var verified by remember(existing?.id) { mutableStateOf(existing?.isVerified ?: false) }
    var picked by remember(existing?.id) { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) picked = uri }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New account" else "Edit account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountAvatar(
                        picked?.toString() ?: existing?.avatarUri, 56.dp,
                        Modifier.clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )
                    Text("Tap to pick avatar", fontSize = 12.sp)
                }
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Verified", modifier = Modifier.weight(1f)); Switch(verified, { verified = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (username.isNotBlank()) onSave(username.trim(), verified, picked) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
