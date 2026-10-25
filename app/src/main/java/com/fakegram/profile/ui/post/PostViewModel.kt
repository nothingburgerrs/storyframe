package com.fakegram.profile.ui.post

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.AccountEntity
import com.fakegram.profile.data.CarouselItemEntity
import com.fakegram.profile.data.CommentEntity
import com.fakegram.profile.data.PostEntity
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.media.MediaStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PostFeedState(
    val profile: ProfileEntity = ProfileEntity(),
    val posts: List<PostEntity> = emptyList(),
    /** postId -> ordered image uris of that carousel (empty for single-image posts). */
    val carousels: Map<Long, List<String>> = emptyMap()
)

/** Backs the opened-post ("Posts") screen: the feed, its carousels and each post's comments. */
class PostViewModel(
    application: Application,
    private val repository: ProfileRepository,
    profileId: Long
) : AndroidViewModel(application) {

    private val carouselFlow: Flow<Map<Long, List<String>>> = repository.observePosts(profileId).flatMapLatest { posts ->
        val carouselPosts = posts.filter { it.mediaType == com.fakegram.profile.data.MediaType.CAROUSEL }
        if (carouselPosts.isEmpty()) flowOf(emptyMap())
        else combine(carouselPosts.map { p -> repository.observeCarousel(p.id).map { items -> p.id to items.toUris() } }) {
            it.toMap()
        }
    }

    val state: StateFlow<PostFeedState> = combine(
        repository.observeProfile(profileId),
        repository.observePosts(profileId),
        carouselFlow
    ) { profile, posts, carousels ->
        PostFeedState(profile ?: ProfileEntity(id = profileId), posts, carousels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PostFeedState())

    private fun List<CarouselItemEntity>.toUris() = sortedBy { it.position }.map { it.mediaUri }

    val accounts: StateFlow<List<AccountEntity>> = repository.accounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    /** Stores a comment author as a reusable saved account (avatar copied if newly picked). */
    fun saveAccount(username: String, avatarPath: String?, newAvatar: Uri?, verified: Boolean) {
        viewModelScope.launch {
            val path = newAvatar?.let { MediaStorage.copyToInternal(getApplication(), it) } ?: avatarPath
            repository.addAccount(AccountEntity(username = username, avatarUri = path, isVerified = verified))
            MediaStorage.toast(getApplication(), "Saved to accounts")
        }
    }

    fun comments(postId: Long): Flow<List<CommentEntity>> = repository.observeComments(postId)

    fun updatePost(post: PostEntity) {
        viewModelScope.launch { repository.updatePost(post) }
    }

    fun addComment(postId: Long, position: Int) {
        viewModelScope.launch {
            repository.addComment(CommentEntity(postId = postId, position = position))
        }
    }

    /** Adds a comment that was typed straight into the comments sheet. */
    fun postComment(postId: Long, position: Int, username: String, avatarUri: String?, text: String) {
        viewModelScope.launch {
            repository.addComment(CommentEntity(postId = postId, username = username.ifBlank { "username" }, avatarUri = avatarUri, text = text, timeText = "now", likeCount = "0", hasTranslation = false, position = position))
        }
    }

    fun updateComment(comment: CommentEntity) {
        viewModelScope.launch { repository.updateComment(comment) }
    }

    /** Saves an edited comment together with an optionally newly picked avatar, in one write. */
    fun saveComment(comment: CommentEntity, newAvatar: Uri?) {
        viewModelScope.launch {
            val path = newAvatar?.let { MediaStorage.copyToInternal(getApplication(), it) }
            repository.updateComment(if (path != null) comment.copy(avatarUri = path) else comment)
        }
    }

    fun deleteComment(id: Long) {
        viewModelScope.launch { repository.deleteComment(id) }
    }

    companion object {
        fun factory(application: Application, repository: ProfileRepository, profileId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PostViewModel(application, repository, profileId) as T
            }
    }
}
