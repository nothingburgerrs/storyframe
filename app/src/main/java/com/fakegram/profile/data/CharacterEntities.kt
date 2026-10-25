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

object CharacterType {
    const val INDIVIDUAL = "individual"
    const val GROUP = "group"
}

data class Platform(val id: String, val label: String, val built: Boolean)

/** Every app a character can have. `built = false` ones are stored but have no screen yet. */
object Platforms {
    const val INSTAGRAM = "instagram"
    const val GOOGLE = "google"
    const val BUBBLE = "bubble"
    const val YOUTUBE = "youtube"
    const val PINTEREST = "pinterest"

    val all = listOf(
        Platform(INSTAGRAM, "Instagram", true),
        Platform(GOOGLE, "Google", true),
        Platform(BUBBLE, "Bubble", true),
        Platform(YOUTUBE, "YouTube", true),
        Platform(PINTEREST, "Pinterest", true)
    )

    /** The apps a character posts on, i.e. what the calendar offers. */
    val postable = listOf(INSTAGRAM, BUBBLE, YOUTUBE)

    fun label(id: String) = all.firstOrNull { it.id == id }?.label ?: id
}

/**
 * The outer-shell identity the whole app is organised around.
 *  - INDIVIDUAL: one persona (name, handle, bio, avatar)
 *  - GROUP: a group account (name, handle, lore, members = individual characters)
 * [activePlatforms] decides which launch shortcuts the character gets and [instagramProfileId]
 * links to the existing Instagram profile, so the platform screens themselves stay untouched.
 * Lists are stored as delimited strings (ids with ',', image paths with '|') to avoid converters.
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String = CharacterType.INDIVIDUAL,
    val name: String = "",
    val handle: String = "",
    val bio: String = "",
    val lore: String = "",
    val avatarUri: String? = null,
    val memberIds: String = "",
    val activePlatforms: String = Platforms.INSTAGRAM,
    val instagramProfileId: Long? = null,
    val mood: String = "",
    val quote: String = "",
    val quoteSource: String = "",
    /** YYYY-MM-DD (the year is only used for display; the next occurrence is what counts). */
    val birthday: String = "",
    val vibe: String = "",
    val postsPerWeek: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

val CharacterEntity.isGroup: Boolean get() = type == CharacterType.GROUP
val CharacterEntity.memberIdList: List<Long> get() = memberIds.split(',').mapNotNull { it.trim().toLongOrNull() }
val CharacterEntity.platformList: List<String> get() = activePlatforms.split(',').map { it.trim() }.filter { it.isNotEmpty() }
val CharacterEntity.vibeList: List<String> get() = vibe.split('|').filter { it.isNotBlank() }

/** One planned post in the in-universe calendar (filled by the scheduler; read by the Welcome card). */
@Entity(
    tableName = "schedule_events",
    foreignKeys = [ForeignKey(
        entity = CharacterEntity::class,
        parentColumns = ["id"],
        childColumns = ["characterId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ScheduleEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long,
    /** YYYY-MM-DD */
    val date: String,
    val platform: String = Platforms.INSTAGRAM,
    val note: String = "",
    /** Optional photo shown as a stamp on the calendar. */
    val imageUri: String? = null,
    /** Placed by the weekly-goal scheduler (until you edit it, then it is yours and is never touched again). */
    val auto: Boolean = false
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: Long): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CharacterEntity): Long

    @Update
    suspend fun update(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_events ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<ScheduleEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ScheduleEventEntity): Long

    @Update
    suspend fun update(event: ScheduleEventEntity)

    @Query("SELECT * FROM schedule_events WHERE characterId = :characterId")
    suspend fun forCharacter(characterId: Long): List<ScheduleEventEntity>

    @Query("DELETE FROM schedule_events WHERE characterId = :characterId AND auto = 1 AND date >= :fromDate")
    suspend fun deleteAutoFrom(characterId: Long, fromDate: String)

    @Query("DELETE FROM schedule_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
