package com.fakegram.profile.ui.components

import com.fakegram.profile.data.FollowedByEntity
import com.fakegram.profile.data.HighlightEntity
import com.fakegram.profile.data.PillEntity
import com.fakegram.profile.data.PostEntity
import com.fakegram.profile.data.ProfileEntity

/** Aggregated, immutable snapshot of everything ProfilePreviewContent needs to render. */
data class ProfileUiState(
    val profile: ProfileEntity,
    val pills: List<PillEntity> = emptyList(),
    val followedBy: List<FollowedByEntity> = emptyList(),
    val highlights: List<HighlightEntity> = emptyList(),
    val posts: List<PostEntity> = emptyList(),
    val carouselCounts: Map<Long, Int> = emptyMap()
) {
    val taggedPosts: List<PostEntity> get() = posts.filter { it.isTagged }
    val reelPosts: List<PostEntity> get() = posts.filter { it.isReel && !it.isTagged }
    /** Main Posts grid: everything except tagged-only posts, which belong solely in the Tagged tab. */
    val gridPosts: List<PostEntity> get() = posts.filter { !it.isTagged }.sortedWith(
        compareByDescending<PostEntity> { it.isPinned }.thenBy { it.position }
    )

    companion object {
        fun empty(): ProfileUiState = ProfileUiState(profile = ProfileEntity())
    }
}

fun followedBySummary(names: List<String>): String? {
    return when (names.size) {
        0 -> null
        1 -> "Followed by ${names[0]}"
        2 -> "Followed by ${names[0]} and ${names[1]}"
        else -> "Followed by ${names[0]}, ${names[1]} and ${names.size - 2} others"
    }
}
