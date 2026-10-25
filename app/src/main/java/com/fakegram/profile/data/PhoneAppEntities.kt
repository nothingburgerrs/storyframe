package com.fakegram.profile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// The built-in phone apps (Notes, Photos, Messages). Each row belongs to the character whose phone it is;
// characterId 0 is the phone shown when no character exists yet.

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long = 0,
    val title: String = "",
    val body: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long = 0,
    val imageUri: String = "",
    /** Comma-separated lowercase words, same idea as Pinterest pins. */
    val tags: String = "",
    val takenAt: Long = System.currentTimeMillis()
)

fun PhotoEntity.matchesQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    return q.isNotEmpty() && tags.tagList().any { it.contains(q) }
}

@Entity(tableName = "sms_threads")
data class SmsThreadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long = 0,
    val contactName: String = "",
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sms_messages")
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: Long = 0,
    val text: String = "",
    /** True = sent by the phone's owner (right, blue); false = received (left, grey). */
    val fromMe: Boolean = false,
    val sentAt: Long = System.currentTimeMillis()
)

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes WHERE characterId = :characterId ORDER BY updatedAt DESC, id DESC")
    fun observe(characterId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PhotosDao {
    @Query("SELECT * FROM photos WHERE characterId = :characterId ORDER BY takenAt DESC, id DESC")
    fun observe(characterId: Long): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long

    @Update
    suspend fun update(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_threads WHERE characterId = :characterId ORDER BY createdAt DESC, id DESC")
    fun observeThreads(characterId: Long): Flow<List<SmsThreadEntity>>

    @Query("SELECT * FROM sms_threads WHERE id = :id")
    fun observeThread(id: Long): Flow<SmsThreadEntity?>

    /** Every message of one phone, so the list can show the last line and search can look inside chats. */
    @Query("SELECT m.* FROM sms_messages m INNER JOIN sms_threads t ON m.threadId = t.id WHERE t.characterId = :characterId ORDER BY m.sentAt ASC, m.id ASC")
    fun observeAllMessages(characterId: Long): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE threadId = :threadId ORDER BY sentAt ASC, id ASC")
    fun observeMessages(threadId: Long): Flow<List<SmsMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: SmsThreadEntity): Long

    @Update
    suspend fun updateThread(thread: SmsThreadEntity)

    @Query("DELETE FROM sms_threads WHERE id = :id")
    suspend fun deleteThread(id: Long)

    @Query("DELETE FROM sms_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesOfThread(threadId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessageEntity): Long

    @Query("DELETE FROM sms_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)
}
