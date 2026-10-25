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
import org.json.JSONArray
import org.json.JSONObject

/** A fake Pinterest account for a character: its profile picture, home tabs and the search page. */
@Entity(tableName = "pinterest_pages")
data class PinterestPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: Long? = null,
    val avatarUri: String? = null,
    /** JSON list of {id,label}: the category tabs across the top of the home feed. */
    val tabs: String = "",
    val searchQuery: String = "",
    /** JSON list of {label,image,color}: the pills under the search bar. */
    val chips: String = "",
    /** The red dot on the messages icon. */
    val showDot: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object PinScreen {
    const val HOME = 0
    const val SEARCH = 1
}

@Entity(
    tableName = "pinterest_pins",
    foreignKeys = [ForeignKey(
        entity = PinterestPageEntity::class,
        parentColumns = ["id"],
        childColumns = ["pageId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    /** [PinScreen.HOME] or [PinScreen.SEARCH]. */
    val screen: Int = PinScreen.HOME,
    /** Which home tab the pin is in (0 on the search page). */
    val tabId: Long = 0,
    val imageUri: String? = null,
    /** Width divided by height of the picture. */
    val aspect: Float = 0.75f,
    val title: String = "",
    val sponsored: Boolean = false,
    val sponsorName: String = "Sponsored",
    val position: Int = 0,
    /** Comma-separated lowercase words; searching one of them finds this pin. */
    val tags: String = ""
)

data class PinTab(val id: Long, val label: String)
data class PinChip(val label: String, val imageUri: String?, val color: Long)

fun String.tagList(): List<String> = split(',').map { it.trim() }.filter { it.isNotEmpty() }

/** "Airi, #Idol  selfie" -> "airi,idol,selfie". */
fun normalizeTags(raw: String): String =
    raw.split(',', ' ', '\n', '#').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

/** True when [query] is part of the title or of any tag, ignoring case. */
fun PinEntity.matchesQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return false
    return title.lowercase().contains(q) || tags.tagList().any { it.contains(q) }
}

val PinChipColors = listOf(0xFFF5DDFA, 0xFFDDE3FF, 0xFFD8F5DD, 0xFFFFE7CC, 0xFFFFD9DE, 0xFFE3E3DC)

fun String.toPinTabs(): List<PinTab> = runCatching {
    val a = JSONArray(this)
    List(a.length()) { i -> a.getJSONObject(i).let { PinTab(it.getLong("id"), it.getString("label")) } }
}.getOrDefault(emptyList())

fun List<PinTab>.tabsToJson(): String = JSONArray().also { arr -> forEach { arr.put(JSONObject().put("id", it.id).put("label", it.label)) } }.toString()

fun String.toPinChips(): List<PinChip> = runCatching {
    val a = JSONArray(this)
    List(a.length()) { i ->
        a.getJSONObject(i).let { PinChip(it.getString("label"), it.optString("image").takeIf { s -> s.isNotEmpty() }, it.optLong("color", PinChipColors[0])) }
    }
}.getOrDefault(emptyList())

fun List<PinChip>.chipsToJson(): String = JSONArray().also { arr ->
    forEach { arr.put(JSONObject().put("label", it.label).put("image", it.imageUri ?: "").put("color", it.color)) }
}.toString()

@Dao
interface PinterestDao {
    @Query("SELECT * FROM pinterest_pages")
    fun observePages(): Flow<List<PinterestPageEntity>>

    @Query("SELECT * FROM pinterest_pages WHERE id = :id")
    fun observePage(id: Long): Flow<PinterestPageEntity?>

    @Query("SELECT * FROM pinterest_pages WHERE id = :id")
    suspend fun getPage(id: Long): PinterestPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PinterestPageEntity): Long

    @Update
    suspend fun updatePage(page: PinterestPageEntity)

    @Query("SELECT * FROM pinterest_pins")
    fun observeAllPins(): Flow<List<PinEntity>>

    @Query("SELECT * FROM pinterest_pins WHERE pageId = :pageId ORDER BY position ASC, id ASC")
    fun observePins(pageId: Long): Flow<List<PinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: PinEntity): Long

    @Update
    suspend fun updatePin(pin: PinEntity)

    @Query("DELETE FROM pinterest_pins WHERE id = :id")
    suspend fun deletePin(id: Long)

    @Query("DELETE FROM pinterest_pins WHERE pageId = :pageId AND tabId = :tabId AND screen = 0")
    suspend fun deletePinsOfTab(pageId: Long, tabId: Long)
}
