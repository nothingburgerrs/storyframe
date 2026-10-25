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

/** A fake "Bubble" fan-chat with one idol/artist. */
@Entity(tableName = "bubble_chats")
data class BubbleChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Only used in the app's own chat list. */
    val title: String = "New chat",
    val artistName: String = "Artist",
    /** Text of the little gradient tag next to the name ("ARTIST"). */
    val badgeText: String = "ARTIST",
    val artistAvatarUri: String? = null,
    /** Optional custom wallpaper behind the messages (artists can set their own). */
    val backgroundUri: String? = null,
    /** One of [BubbleMessageKind]-independent theme ids: LAVENDER, PINK, GREY, DARK. */
    val theme: String = "LAVENDER",
    val use24h: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** The character / group speaking in this chat (null = not attached to anyone). */
    val characterId: Long? = null
)

@Entity(
    tableName = "bubble_messages",
    foreignKeys = [ForeignKey(
        entity = BubbleChatEntity::class,
        parentColumns = ["id"],
        childColumns = ["chatId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BubbleMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    /** true = the artist (left, avatar + tag); false = the fan (right). */
    val fromArtist: Boolean = true,
    val kind: String = BubbleMessageKind.TEXT,
    val text: String = "",
    /** Optional second line under a dashed divider, e.g. the English translation. */
    val translation: String = "",
    val imageUri: String? = null,
    val timeText: String = "",
    val position: Int = 0
)

object BubbleMessageKind {
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
    const val DATE = "DATE"
}

@Dao
interface BubbleDao {
    @Query("SELECT * FROM bubble_chats ORDER BY updatedAt DESC")
    fun observeChats(): Flow<List<BubbleChatEntity>>

    @Query("SELECT * FROM bubble_chats WHERE id = :id")
    fun observeChat(id: Long): Flow<BubbleChatEntity?>

    @Query("SELECT * FROM bubble_chats WHERE id = :id")
    suspend fun getChat(id: Long): BubbleChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: BubbleChatEntity): Long

    @Update
    suspend fun updateChat(chat: BubbleChatEntity)

    @Query("DELETE FROM bubble_chats WHERE id = :id")
    suspend fun deleteChat(id: Long)

    @Query("SELECT * FROM bubble_messages WHERE chatId = :chatId ORDER BY position ASC, id ASC")
    fun observeMessages(chatId: Long): Flow<List<BubbleMessageEntity>>

    @Query("SELECT * FROM bubble_messages WHERE chatId = :chatId ORDER BY position ASC, id ASC")
    suspend fun getMessages(chatId: Long): List<BubbleMessageEntity>

    @Query("SELECT * FROM bubble_messages ORDER BY position ASC, id ASC")
    fun observeAllMessages(): Flow<List<BubbleMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: BubbleMessageEntity): Long

    @Update
    suspend fun updateMessage(message: BubbleMessageEntity)

    @Query("DELETE FROM bubble_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM bubble_messages WHERE chatId = :chatId")
    suspend fun clearMessages(chatId: Long)
}
