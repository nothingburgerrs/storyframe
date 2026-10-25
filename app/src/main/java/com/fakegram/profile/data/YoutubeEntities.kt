package com.fakegram.profile.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A fake YouTube watch page: player, title row, channel row, action row and a comments card. */
@Entity(tableName = "youtube_videos")
data class YoutubeVideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The character this video belongs to (null = not attached to anyone). */
    val characterId: Long? = null,
    val title: String = "New video",
    val channelName: String = "",
    val channelAvatarUri: String? = null,
    /** Still frame shown in the player. */
    val thumbnailUri: String? = null,
    val viewsText: String = "0 views",
    val timeText: String = "just now",
    val likeCount: String = "",
    val subtitleText: String = "",
    /** Shown on the player, e.g. "0:11 / 20:21". */
    val durationText: String = "0:11 / 20:21",
    /** Auto-dubbed / translated badge next to the channel name. */
    val autoDubbed: Boolean = false,
    val subscribed: Boolean = false,
    /** Title, channel line and the action buttons under the player. */
    val showPostInfo: Boolean = true,
    /** The caption line over the picture. */
    val showCaption: Boolean = true,
    /** The play / skip / top buttons, timer and fullscreen over the picture. Off = just the picture and its progress bar. */
    val showControls: Boolean = true,
    /** The list of other videos under the comments. */
    val showRelated: Boolean = true,
    /** How far along the player's progress bar is, 0..1 (drag it on the player). */
    val progress: Float = 0.04f,
    /** What the Comments card says (e.g. "12K"); blank = the real number of comments written. */
    val commentCount: String = "",
    /** The ad under the comments card is off unless asked for. */
    val showAd: Boolean = false,
    val adImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "youtube_comments",
    foreignKeys = [ForeignKey(
        entity = YoutubeVideoEntity::class,
        parentColumns = ["id"],
        childColumns = ["videoId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class YoutubeCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: Long,
    val username: String = "",
    val avatarUri: String? = null,
    val text: String = "",
    val timeText: String = "just now",
    val likeCount: String = "",
    val replyCount: Int = 0,
    val hasTranslation: Boolean = false,
    val position: Int = 0
)

@Dao
interface YoutubeDao {
    @Query("SELECT * FROM youtube_videos ORDER BY createdAt DESC, id DESC")
    fun observeVideos(): Flow<List<YoutubeVideoEntity>>

    @Query("SELECT * FROM youtube_videos WHERE id = :id")
    fun observeVideo(id: Long): Flow<YoutubeVideoEntity?>

    @Query("SELECT * FROM youtube_videos WHERE id = :id")
    suspend fun getVideo(id: Long): YoutubeVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: YoutubeVideoEntity): Long

    @Update
    suspend fun updateVideo(video: YoutubeVideoEntity)

    @Query("DELETE FROM youtube_videos WHERE id = :id")
    suspend fun deleteVideo(id: Long)

    @Query("SELECT * FROM youtube_comments WHERE videoId = :videoId ORDER BY position ASC, id ASC")
    fun observeComments(videoId: Long): Flow<List<YoutubeCommentEntity>>

    @Query("SELECT * FROM youtube_comments WHERE videoId = :videoId ORDER BY position ASC, id ASC")
    suspend fun getComments(videoId: Long): List<YoutubeCommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: YoutubeCommentEntity): Long

    @Update
    suspend fun updateComment(comment: YoutubeCommentEntity)

    @Query("DELETE FROM youtube_comments WHERE id = :id")
    suspend fun deleteComment(id: Long)
}
