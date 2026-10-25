package com.fakegram.profile.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.data.YoutubeCommentEntity
import com.fakegram.profile.data.YoutubeVideoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Videos of one character (characterId 0 = every video). */
class YoutubeListViewModel(private val repository: ProfileRepository, private val characterId: Long) : ViewModel() {
    val videos: StateFlow<List<YoutubeVideoEntity>> = repository.youtubeVideos
        .map { all -> if (characterId == 0L) all else all.filter { it.characterId == characterId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(channelName: String, avatarUri: String?, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addYoutubeVideo(
                YoutubeVideoEntity(
                    characterId = characterId.takeIf { it != 0L }, channelName = channelName, channelAvatarUri = avatarUri
                )
            )
            onCreated(id)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteYoutubeVideo(id) }
    }

    fun save(video: YoutubeVideoEntity) {
        viewModelScope.launch { repository.updateYoutubeVideo(video) }
    }

    companion object {
        fun factory(repository: ProfileRepository, characterId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = YoutubeListViewModel(repository, characterId) as T
            }
    }
}

/** One watch page: the video and its comments. */
class YoutubeWatchViewModel(private val repository: ProfileRepository, private val videoId: Long) : ViewModel() {
    val video: StateFlow<YoutubeVideoEntity?> = repository.observeYoutubeVideo(videoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val comments: StateFlow<List<YoutubeCommentEntity>> = repository.observeYoutubeComments(videoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveVideo(video: YoutubeVideoEntity) {
        viewModelScope.launch { repository.updateYoutubeVideo(video) }
    }

    fun deleteVideo() {
        viewModelScope.launch { repository.deleteYoutubeVideo(videoId) }
    }

    fun saveComment(comment: YoutubeCommentEntity) {
        viewModelScope.launch {
            if (comment.id == 0L) {
                val next = (comments.value.maxOfOrNull { it.position } ?: -1) + 1
                repository.addYoutubeComment(comment.copy(videoId = videoId, position = next))
            } else repository.updateYoutubeComment(comment)
        }
    }

    fun deleteComment(id: Long) {
        viewModelScope.launch { repository.deleteYoutubeComment(id) }
    }

    companion object {
        fun factory(repository: ProfileRepository, videoId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = YoutubeWatchViewModel(repository, videoId) as T
            }
    }
}
