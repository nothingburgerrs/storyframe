package com.fakegram.profile.ui.bubble

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.BubbleChatEntity
import com.fakegram.profile.data.BubbleMessageEntity
import com.fakegram.profile.data.BubbleMessageKind
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.media.MediaStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Backs the "Bubble" tab: the list of saved chats. */
class BubbleHomeViewModel(private val repository: ProfileRepository) : ViewModel() {

    val chats: StateFlow<List<BubbleChatEntity>> = repository.bubbleChats.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    /** chatId -> text of its last message, for the list preview. */
    val lastMessages: StateFlow<Map<Long, String>> = combine(repository.allBubbleMessages, repository.bubbleChats) { messages, _ ->
        messages.groupBy { it.chatId }.mapValues { (_, list) ->
            val last = list.maxByOrNull { it.position }
            when {
                last == null -> ""
                last.kind == BubbleMessageKind.IMAGE -> "Photo"
                else -> last.text
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun createChat(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repository.createBubbleChat()) }
    }

    fun duplicateChat(id: Long) {
        viewModelScope.launch { repository.duplicateBubbleChat(id) }
    }

    fun deleteChat(id: Long) {
        viewModelScope.launch { repository.deleteBubbleChat(id) }
    }

    companion object {
        fun factory(repository: ProfileRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = BubbleHomeViewModel(repository) as T
            }
    }
}

data class BubbleChatState(
    val chat: BubbleChatEntity? = null,
    val messages: List<BubbleMessageEntity> = emptyList()
)

/** Backs one chat: the conversation plus its look (theme, artist name, avatar...). */
class BubbleChatViewModel(
    application: Application,
    private val repository: ProfileRepository,
    private val chatId: Long
) : AndroidViewModel(application) {

    val state: StateFlow<BubbleChatState> = combine(
        repository.observeBubbleChat(chatId),
        repository.observeBubbleMessages(chatId)
    ) { chat, messages -> BubbleChatState(chat, messages) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BubbleChatState())

    private val chatLock = Mutex()

    fun updateChat(transform: (BubbleChatEntity) -> BubbleChatEntity) {
        viewModelScope.launch {
            chatLock.withLock {
                val current = repository.getBubbleChat(chatId) ?: return@withLock
                repository.updateBubbleChat(transform(current))
            }
        }
    }

    /** Saves the settings dialog in one write; a newly picked avatar is copied into app storage. */
    fun saveSettings(transform: (BubbleChatEntity) -> BubbleChatEntity, newAvatar: Uri?, newBackground: Uri?) {
        viewModelScope.launch {
            val avatarPath = newAvatar?.let { MediaStorage.copyToInternal(getApplication(), it) }
            val backgroundPath = newBackground?.let { MediaStorage.copyToInternal(getApplication(), it) }
            chatLock.withLock {
                val current = repository.getBubbleChat(chatId) ?: return@withLock
                var updated = transform(current)
                if (avatarPath != null) updated = updated.copy(artistAvatarUri = avatarPath)
                if (backgroundPath != null) updated = updated.copy(backgroundUri = backgroundPath)
                repository.updateBubbleChat(updated)
            }
        }
    }

    private fun nowText(use24h: Boolean): String =
        SimpleDateFormat(if (use24h) "HH:mm" else "hh:mm a", Locale.ENGLISH).format(Date())

    private fun nextPosition(): Int = (state.value.messages.maxOfOrNull { it.position } ?: -1) + 1

    fun sendText(fromArtist: Boolean, text: String, translation: String) {
        viewModelScope.launch {
            val use24h = state.value.chat?.use24h ?: true
            repository.addBubbleMessage(
                BubbleMessageEntity(
                    chatId = chatId, fromArtist = fromArtist, kind = BubbleMessageKind.TEXT,
                    text = text, translation = translation, timeText = nowText(use24h), position = nextPosition()
                )
            )
            touch()
        }
    }

    fun sendImage(fromArtist: Boolean, uri: Uri) {
        viewModelScope.launch {
            val path = MediaStorage.copyToInternal(getApplication(), uri) ?: return@launch
            val use24h = state.value.chat?.use24h ?: true
            repository.addBubbleMessage(
                BubbleMessageEntity(
                    chatId = chatId, fromArtist = fromArtist, kind = BubbleMessageKind.IMAGE,
                    imageUri = path, timeText = nowText(use24h), position = nextPosition()
                )
            )
            touch()
        }
    }

    fun addDivider(text: String) {
        viewModelScope.launch {
            repository.addBubbleMessage(
                BubbleMessageEntity(chatId = chatId, kind = BubbleMessageKind.DATE, text = text, position = nextPosition())
            )
            touch()
        }
    }

    fun updateMessage(message: BubbleMessageEntity) {
        viewModelScope.launch { repository.updateBubbleMessage(message); touch() }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch { repository.deleteBubbleMessage(id) }
    }

    fun clearChat() {
        viewModelScope.launch { repository.clearBubbleMessages(chatId) }
    }

    fun moveMessage(message: BubbleMessageEntity, delta: Int) {
        viewModelScope.launch {
            val list = state.value.messages.sortedBy { it.position }
            val from = list.indexOfFirst { it.id == message.id }
            val to = from + delta
            if (from < 0 || to !in list.indices) return@launch
            val reordered = list.toMutableList().apply { add(to, removeAt(from)) }
            reordered.forEachIndexed { i, m -> if (m.position != i) repository.updateBubbleMessage(m.copy(position = i)) }
        }
    }

    private suspend fun touch() {
        chatLock.withLock {
            repository.getBubbleChat(chatId)?.let { repository.updateBubbleChat(it) }
        }
    }

    companion object {
        fun factory(application: Application, repository: ProfileRepository, chatId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BubbleChatViewModel(application, repository, chatId) as T
            }
    }
}
