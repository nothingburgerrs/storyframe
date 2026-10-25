package com.fakegram.profile.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.CarouselItemEntity
import com.fakegram.profile.data.FollowedByEntity
import com.fakegram.profile.data.HighlightEntity
import com.fakegram.profile.data.MediaType
import com.fakegram.profile.data.NoteType
import com.fakegram.profile.data.PillEntity
import com.fakegram.profile.data.PostEntity
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.data.StoryRingType
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.settings.ThemePreferences
import com.fakegram.profile.ui.components.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(
    application: Application,
    private val repository: ProfileRepository,
    private val themePreferences: ThemePreferences,
    val profileId: Long
) : AndroidViewModel(application) {

    private val profileFlow = repository.observeProfile(profileId)
    private val pillsFlow = repository.observePills(profileId)
    private val followedByFlow = repository.observeFollowedBy(profileId)
    private val highlightsFlow = repository.observeHighlights(profileId)
    private val postsFlow = repository.observePosts(profileId)

    val isDarkMode: StateFlow<Boolean> = themePreferences.isDarkMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    private val _carouselCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())

    init {
        viewModelScope.launch {
            postsFlow
                .catch { /* never let a DB read failure crash the app */ }
                .collect { posts ->
                    val counts = mutableMapOf<Long, Int>()
                    for (post in posts) {
                        if (post.mediaType == MediaType.CAROUSEL) {
                            counts[post.id] = runCatching { repository.getCarouselForPost(post.id).size }.getOrDefault(0)
                        }
                    }
                    _carouselCounts.value = counts
                }
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        profileFlow, pillsFlow, followedByFlow, highlightsFlow, postsFlow
    ) { profile, pills, followedBy, highlights, posts ->
        ProfileUiState(
            profile = profile ?: ProfileEntity(id = profileId),
            pills = pills,
            followedBy = followedBy,
            highlights = highlights,
            posts = posts,
            carouselCounts = _carouselCounts.value
        )
    }.catch { emit(ProfileUiState.empty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState.empty())

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDarkMode(enabled) }
    }

    private fun currentProfile(): ProfileEntity = uiState.value.profile

    fun updateProfile(transform: (ProfileEntity) -> ProfileEntity) {
        viewModelScope.launch {
            val updated = transform(currentProfile())
            repository.updateProfile(updated)
        }
    }

    // ---- Media pick helpers ----
    fun setAvatar(path: String) {
        updateProfile { it.copy(avatarUri = path) }
    }

    /**
     * Applies every field from the "Edit profile" dialog in a SINGLE read-modify-write.
     * Calling [updateProfile] twice in a row (once for the avatar, once for the rest) is a race:
     * each call reads [currentProfile] from the current (possibly stale) [uiState] snapshot, and
     * Room's write is async, so the second call can read a snapshot that doesn't yet reflect the
     * first call's write and clobber it — which is exactly how the avatar picker "worked" (picker
     * launched, image selected) but the new avatar never actually stuck.
     */
    fun saveProfileEdits(
        storyRingType: StoryRingType,
        displayName: String,
        username: String,
        bio: String,
        newAvatarPath: String?,
        clearAvatar: Boolean
    ) {
        updateProfile {
            it.copy(
                storyRingType = storyRingType,
                displayName = displayName,
                username = username,
                bio = bio,
                avatarUri = when {
                    newAvatarPath != null -> newAvatarPath
                    clearAvatar -> null
                    else -> it.avatarUri
                }
            )
        }
    }

    fun addFollowedByAvatar(entryId: Long, uri: Uri) {
        val path = MediaStorage.copyToInternal(getApplication(), uri) ?: return
        viewModelScope.launch {
            val existing = uiState.value.followedBy.find { it.id == entryId } ?: return@launch
            repository.updateFollowedBy(existing.copy(avatarUri = path))
        }
    }

    fun addHighlightCover(highlightId: Long, uri: Uri) {
        val path = MediaStorage.copyToInternal(getApplication(), uri) ?: return
        viewModelScope.launch {
            val existing = uiState.value.highlights.find { it.id == highlightId } ?: return@launch
            repository.updateHighlight(existing.copy(coverUri = path))
        }
    }

    // ---- Pills ----
    fun addPill(text: String, iconKey: String?, showIcon: Boolean) {
        viewModelScope.launch {
            val position = uiState.value.pills.size
            repository.addPill(PillEntity(profileId = profileId, text = text, iconKey = iconKey, showIcon = showIcon, position = position))
        }
    }

    fun updatePill(pill: PillEntity) {
        viewModelScope.launch { repository.updatePill(pill) }
    }

    fun deletePill(pill: PillEntity) {
        viewModelScope.launch { repository.deletePill(pill) }
    }

    fun movePill(pill: PillEntity, delta: Int) {
        viewModelScope.launch {
            val list = uiState.value.pills.sortedBy { it.position }.toMutableList()
            val index = list.indexOfFirst { it.id == pill.id }
            val target = index + delta
            if (index < 0 || target < 0 || target >= list.size) return@launch
            val a = list[index]
            val b = list[target]
            repository.updatePill(a.copy(position = b.position))
            repository.updatePill(b.copy(position = a.position))
        }
    }

    // ---- Followed by ----
    fun addFollowedBy(username: String) {
        addFollowedByPending(username, null)
    }

    fun addFollowedByPending(username: String, uri: Uri?) {
        viewModelScope.launch {
            val path = uri?.let { MediaStorage.copyToInternal(getApplication(), it) }
            val position = uiState.value.followedBy.size
            repository.addFollowedBy(FollowedByEntity(profileId = profileId, username = username, avatarUri = path, position = position))
        }
    }

    fun deleteFollowedBy(id: Long) {
        viewModelScope.launch { repository.deleteFollowedBy(id) }
    }

    fun moveFollowedBy(entry: FollowedByEntity, delta: Int) {
        viewModelScope.launch {
            val list = uiState.value.followedBy.sortedBy { it.position }.toMutableList()
            val index = list.indexOfFirst { it.id == entry.id }
            val target = index + delta
            if (index < 0 || target < 0 || target >= list.size) return@launch
            val a = list[index]
            val b = list[target]
            repository.updateFollowedBy(a.copy(position = b.position))
            repository.updateFollowedBy(b.copy(position = a.position))
        }
    }

    // ---- Highlights ----
    fun addHighlight(title: String, coverPath: String?) {
        viewModelScope.launch {
            val position = uiState.value.highlights.size
            repository.addHighlight(HighlightEntity(profileId = profileId, title = title, coverUri = coverPath, position = position))
        }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch { repository.deleteHighlight(id) }
    }

    fun editHighlight(id: Long, title: String, newCoverPath: String?) {
        viewModelScope.launch {
            val existing = uiState.value.highlights.find { it.id == id } ?: return@launch
            val path = newCoverPath ?: existing.coverUri
            repository.updateHighlight(existing.copy(title = title, coverUri = path))
        }
    }

    fun updateFollowedByUsername(id: Long, username: String) {
        viewModelScope.launch {
            val existing = uiState.value.followedBy.find { it.id == id } ?: return@launch
            repository.updateFollowedBy(existing.copy(username = username))
        }
    }

    fun moveHighlight(highlight: HighlightEntity, delta: Int) {
        viewModelScope.launch {
            val list = uiState.value.highlights.sortedBy { it.position }.toMutableList()
            val index = list.indexOfFirst { it.id == highlight.id }
            val target = index + delta
            if (index < 0 || target < 0 || target >= list.size) return@launch
            val a = list[index]
            val b = list[target]
            repository.updateHighlight(a.copy(position = b.position))
            repository.updateHighlight(b.copy(position = a.position))
        }
    }

    // ---- Posts ----
    // All of these now take an already-saved internal file path — the crop step (ImageCropDialog)
    // runs first at the UI layer and saves the cropped bitmap via MediaStorage.saveBitmapToInternal.
    fun addImagePost(path: String, isTagged: Boolean = false) {
        viewModelScope.launch {
            val position = uiState.value.posts.size
            repository.addPost(
                PostEntity(profileId = profileId, mediaType = MediaType.IMAGE, primaryMediaUri = path, position = position, isTagged = isTagged)
            )
        }
    }

    // "Video" posts no longer pick or store a real video file — just an image used as the
    // fake-video thumbnail. The VIDEO mediaType flag alone drives the grid's video badge.
    fun addVideoPost(thumbPath: String, isTagged: Boolean = false) {
        viewModelScope.launch {
            val position = uiState.value.posts.size
            repository.addPost(
                PostEntity(
                    profileId = profileId,
                    mediaType = MediaType.VIDEO,
                    primaryMediaUri = thumbPath,
                    videoUri = null,
                    position = position,
                    isReel = true,
                    isTagged = isTagged
                )
            )
        }
    }

    fun addCarouselPost(paths: List<String>, isTagged: Boolean = false) {
        if (paths.isEmpty()) return
        viewModelScope.launch {
            val position = uiState.value.posts.size
            val postId = repository.addPost(
                PostEntity(profileId = profileId, mediaType = MediaType.CAROUSEL, primaryMediaUri = paths.first(), position = position, isTagged = isTagged)
            )
            paths.forEachIndexed { index, path ->
                repository.addCarouselItem(CarouselItemEntity(postId = postId, mediaUri = path, position = index))
            }
        }
    }

    fun loadOtherProfiles(callback: (List<ProfileEntity>) -> Unit) {
        viewModelScope.launch {
            val list = repository.profiles.first()
            callback(list.filter { it.id != profileId })
        }
    }

    fun loadPostsForProfile(otherProfileId: Long, callback: (List<PostEntity>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getPostsForProfile(otherProfileId))
        }
    }

    fun loadCarouselForPost(postId: Long, callback: (List<CarouselItemEntity>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getCarouselForPost(postId))
        }
    }

    fun reusePost(sourcePost: PostEntity, sourceCarousel: List<CarouselItemEntity>) {
        viewModelScope.launch {
            val position = uiState.value.posts.size
            val newPostId = repository.addPost(
                sourcePost.copy(id = 0, profileId = profileId, position = position)
            )
            sourceCarousel.forEach { item ->
                repository.addCarouselItem(item.copy(id = 0, postId = newPostId))
            }
        }
    }

    fun togglePinned(post: PostEntity) {
        viewModelScope.launch { repository.updatePost(post.copy(isPinned = !post.isPinned)) }
    }

    fun toggleTagged(post: PostEntity) {
        viewModelScope.launch { repository.updatePost(post.copy(isTagged = !post.isTagged)) }
    }

    fun toggleReel(post: PostEntity) {
        viewModelScope.launch { repository.updatePost(post.copy(isReel = !post.isReel)) }
    }

    fun deletePost(id: Long) {
        viewModelScope.launch { repository.deletePost(id) }
    }

    fun changeMediaTypeToCarousel(post: PostEntity) {
        if (post.mediaType == MediaType.CAROUSEL) return
        viewModelScope.launch {
            val firstPath = post.primaryMediaUri
            repository.updatePost(post.copy(mediaType = MediaType.CAROUSEL, videoUri = null))
            if (firstPath != null) {
                repository.addCarouselItem(CarouselItemEntity(postId = post.id, mediaUri = firstPath, position = 0))
            }
        }
    }

    fun replacePostMedia(post: PostEntity, path: String, isVideo: Boolean) {
        viewModelScope.launch {
            if (isVideo) {
                repository.updatePost(post.copy(mediaType = MediaType.VIDEO, primaryMediaUri = path, videoUri = null, isReel = true))
            } else {
                repository.updatePost(post.copy(mediaType = MediaType.IMAGE, primaryMediaUri = path, videoUri = null))
            }
        }
    }

    fun reorderPosts(newOrder: List<PostEntity>) {
        viewModelScope.launch {
            newOrder.forEachIndexed { index, post ->
                if (post.position != index) {
                    repository.updatePost(post.copy(position = index))
                }
            }
        }
    }

    companion object {
        fun factory(application: Application, repository: ProfileRepository, themePreferences: ThemePreferences, profileId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditorViewModel(application, repository, themePreferences, profileId) as T
                }
            }
    }
}
