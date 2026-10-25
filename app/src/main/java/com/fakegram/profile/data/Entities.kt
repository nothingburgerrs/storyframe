package com.fakegram.profile.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String = "new_user",
    val displayName: String = "New Profile",
    val avatarUri: String? = null,
    val storyRingType: StoryRingType = StoryRingType.NONE,
    val hasActiveStory: Boolean = false,
    val isVerified: Boolean = false,
    val postCount: String = "0",
    val followerCount: String = "0",
    val followingCount: String = "0",
    val bio: String = "",
    val location: String? = null,
    val website: String? = null,
    val noteType: NoteType = NoteType.NONE,
    val noteText: String? = null,
    val noteSongTitle: String? = null,
    val noteArtist: String? = null,
    val showReelsTab: Boolean = true,
    val showRepostsTab: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pills",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val text: String,
    val iconKey: String? = null,
    val showIcon: Boolean = true,
    val position: Int = 0
)

@Entity(
    tableName = "followed_by",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FollowedByEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val username: String,
    val avatarUri: String? = null,
    val position: Int = 0
)

@Entity(
    tableName = "highlights",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val title: String,
    val coverUri: String? = null,
    val position: Int = 0
)

@Entity(
    tableName = "posts",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val mediaType: MediaType = MediaType.IMAGE,
    val primaryMediaUri: String? = null,
    val videoUri: String? = null,
    val position: Int = 0,
    val isPinned: Boolean = false,
    val isTagged: Boolean = false,
    val isReel: Boolean = false,
    // ---- shown when the post is opened ----
    val caption: String = "",
    val likeCount: String = "0",
    val commentCount: String = "0",
    val repostCount: String = "0",
    val shareCount: String = "0",
    /** Free text such as "September 13". */
    val dateText: String = "",
    /** Optional music line under the username, e.g. "JENNIE · FALLEN ANGEL". */
    val musicText: String = ""
)

@Entity(
    tableName = "carousel_items",
    foreignKeys = [ForeignKey(
        entity = PostEntity::class,
        parentColumns = ["id"],
        childColumns = ["postId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CarouselItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val mediaUri: String,
    val position: Int = 0
)

@Entity(
    tableName = "comments",
    foreignKeys = [ForeignKey(
        entity = PostEntity::class,
        parentColumns = ["id"],
        childColumns = ["postId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val username: String = "username",
    val avatarUri: String? = null,
    val text: String = "",
    /** Free text such as "1w". */
    val timeText: String = "1w",
    val likeCount: String = "0",
    val isVerified: Boolean = false,
    val hasTranslation: Boolean = true,
    val position: Int = 0
)

/** A reusable placeholder account (name, avatar, verified badge) for comments, "followed by", etc. */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String = "",
    val avatarUri: String? = null,
    val isVerified: Boolean = false
)
