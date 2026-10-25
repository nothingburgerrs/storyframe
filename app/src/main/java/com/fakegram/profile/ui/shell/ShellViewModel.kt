package com.fakegram.profile.ui.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.CharacterEntity
import com.fakegram.profile.data.BubbleChatEntity
import com.fakegram.profile.data.CharacterType
import com.fakegram.profile.data.PanelEntity
import com.fakegram.profile.data.Platforms
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.data.ScheduleEventEntity
import com.fakegram.profile.data.chipsToJson
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.tabsToJson
import com.fakegram.profile.data.memberIdList
import com.fakegram.profile.data.platformList
import com.fakegram.profile.settings.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * App-wide "Character OS" state, shared by Welcome, Hub, Apps and the header pill:
 * the list of characters/groups, which one is active, and the helpers to edit them.
 */
class ShellViewModel(
    application: Application,
    private val repository: ProfileRepository,
    private val prefs: ThemePreferences
) : AndroidViewModel(application) {

    val characters: StateFlow<List<CharacterEntity>> = repository.characters
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** False until the first database read finishes, so screens do not flash their empty state. */
    val loaded: StateFlow<Boolean> = repository.characters.map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val profiles: StateFlow<List<ProfileEntity>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val events: StateFlow<List<ScheduleEventEntity>> = repository.scheduleEvents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val activeId: StateFlow<Long> = prefs.activeCharacterId
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1L)

    /** The selected character; falls back to the first one so something is always selected when any exist. */
    val active: StateFlow<CharacterEntity?> = combine(characters, activeId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _editMode = MutableStateFlow(true)

    /** True = every app on the phone can be changed; false = view only, like a real phone. */
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()

    fun setEditMode(editing: Boolean) {
        _editMode.value = editing
    }

    private val editLock = Mutex()

    init {
        viewModelScope.launch { migrateProfilesToCharacters() }
    }

    /**
     * One-time migration: people who already have Instagram profiles get a matching individual
     * character for each, linked to it. The profiles themselves are not changed.
     */
    private suspend fun migrateProfilesToCharacters() {
        if (repository.characters.first().isNotEmpty()) return
        val existing = repository.profiles.first().sortedBy { it.createdAt }
        for (p in existing) {
            repository.addCharacter(
                CharacterEntity(
                    type = CharacterType.INDIVIDUAL,
                    name = p.displayName.ifBlank { p.username },
                    handle = cleanHandle(p.username),
                    bio = p.bio,
                    avatarUri = p.avatarUri,
                    activePlatforms = listOf(Platforms.INSTAGRAM, Platforms.GOOGLE, Platforms.BUBBLE, Platforms.YOUTUBE).joinToString(","),
                    instagramProfileId = p.id
                )
            )
        }
    }

    fun addEvent(event: ScheduleEventEntity) {
        viewModelScope.launch { repository.addScheduleEvent(event) }
    }

    /** Editing an auto-placed entry makes it yours: the scheduler never moves or removes it again. */
    fun updateEvent(event: ScheduleEventEntity) {
        viewModelScope.launch { repository.updateScheduleEvent(event.copy(auto = false)) }
    }

    /** Sets how many posts per week [c] makes and spreads that many across every coming week. */
    fun setWeeklyGoal(c: CharacterEntity, goal: Int) {
        viewModelScope.launch {
            editLock.withLock {
                val cur = repository.getCharacter(c.id) ?: return@withLock
                repository.updateCharacter(cur.copy(postsPerWeek = goal.takeIf { it > 0 }))
            }
            rebuildSchedule(c.id, goal)
        }
    }

    /** Throws away the auto-placed days from today on and picks new random ones. */
    fun reshuffle(c: CharacterEntity) {
        viewModelScope.launch { rebuildSchedule(c.id, c.postsPerWeek ?: 0) }
    }

    /**
     * For each of the next 26 weeks, tops the week up to [goal] posts on random days that are not next to each other
     * (when there is room), skipping days that already have something. Entries you made or edited are kept.
     */
    private val scheduleLock = Mutex()

    private suspend fun rebuildSchedule(characterId: Long, goal: Int) = scheduleLock.withLock {
        val today = java.time.LocalDate.now()
        repository.deleteAutoEventsFrom(characterId, today.toString())
        if (goal <= 0) return@withLock
        val character = repository.getCharacter(characterId) ?: return@withLock
        val platform = character.platformList.firstOrNull { it in Platforms.postable } ?: Platforms.INSTAGRAM
        val existing = repository.scheduleFor(characterId)
        val firstWeek = today.minusDays((today.dayOfWeek.value % 7).toLong())
        for (w in 0 until 26) {
            val start = firstWeek.plusWeeks(w.toLong())
            val end = start.plusDays(6)
            val occupied = existing.mapNotNull { e -> runCatching { java.time.LocalDate.parse(e.date) }.getOrNull() }
                .filter { !it.isBefore(start) && !it.isAfter(end) }.toSet()
            val candidates = (0..6).map { start.plusDays(it.toLong()) }.filter { !it.isBefore(today) && it !in occupied }
            val need = goal - occupied.size
            if (need <= 0 || candidates.isEmpty()) continue
            // More posts than free days: the extras land on random days that already have one.
            val days = pickSpread(candidates, occupied, need.coerceAtMost(candidates.size)).toMutableList()
            while (days.size < need) days += candidates.random()
            for (d in days) {
                repository.addScheduleEvent(ScheduleEventEntity(characterId = characterId, date = d.toString(), platform = platform, auto = true))
            }
        }
    }

    /** [need] random days from [candidates], avoiding neighbours of each other and of [taken] days where possible. */
    private fun pickSpread(candidates: List<java.time.LocalDate>, taken: Set<java.time.LocalDate>, need: Int): List<java.time.LocalDate> {
        val chosen = mutableListOf<java.time.LocalDate>()
        fun touches(d: java.time.LocalDate) = (chosen + taken).any { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(it, d)) == 1L }
        for (d in candidates.shuffled()) { if (chosen.size == need) break; if (!touches(d)) chosen += d }
        for (d in candidates.shuffled()) { if (chosen.size == need) break; if (d !in chosen) chosen += d }
        return chosen
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch { repository.deleteScheduleEvent(id) }
    }

    fun setActive(id: Long) {
        viewModelScope.launch { prefs.setActiveCharacterId(id) }
    }

    /** Creates a character (the first one becomes active automatically) or updates an existing one. */
    fun saveCharacter(existing: CharacterEntity?, draft: CharacterEntity) {
        viewModelScope.launch {
            if (existing == null) {
                val id = repository.addCharacter(draft.copy(id = 0))
                if (active.value == null) prefs.setActiveCharacterId(id)
            } else {
                repository.updateCharacter(draft.copy(id = existing.id, createdAt = existing.createdAt))
            }
        }
    }

    /** Deleting also removes the character from every group; its Instagram profile is kept. */
    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            for (g in characters.value) {
                if (g.isGroup && id in g.memberIdList) {
                    repository.updateCharacter(g.copy(memberIds = g.memberIdList.filter { it != id }.joinToString(",")))
                }
            }
            repository.deleteCharacter(id)
            if (activeId.value == id) prefs.setActiveCharacterId(-1L)
        }
    }

    /** Read-modify-write on the freshest row, so quick consecutive edits never overwrite each other. */
    fun edit(id: Long, transform: (CharacterEntity) -> CharacterEntity) {
        viewModelScope.launch {
            editLock.withLock {
                val current = repository.getCharacter(id) ?: return@withLock
                repository.updateCharacter(transform(current))
            }
        }
    }

    /** A brand-new Instagram profile becomes the active character's profile if it has none yet. */
    fun linkInstagramIfMissing(profileId: Long) {
        val c = active.value ?: return
        if (c.instagramProfileId == null) edit(c.id) { it.copy(instagramProfileId = profileId) }
    }

    private val linkLock = Mutex()

    /**
     * The Google panel for [c]: the one already linked to them, else an unlinked panel with their name
     * (so older panels adopt the right owner), else a fresh one named after them.
     */
    suspend fun panelIdFor(c: CharacterEntity): Long = linkLock.withLock {
        val all = repository.panels.first()
        all.firstOrNull { it.characterId == c.id }?.let { return@withLock it.id }
        all.firstOrNull { it.characterId == null && it.title.trim().equals(c.name.trim(), ignoreCase = true) }?.let {
            repository.updatePanel(it.copy(characterId = c.id))
            return@withLock it.id
        }
        repository.addPanel(PanelEntity(query = c.name, title = c.name, characterId = c.id))
    }

    /** The Bubble chat where [c] talks to you - found by link, then by name, else created. */
    suspend fun chatIdFor(c: CharacterEntity): Long = linkLock.withLock {
        val all = repository.bubbleChats.first()
        all.firstOrNull { it.characterId == c.id }?.let { return@withLock it.id }
        all.firstOrNull {
            it.characterId == null &&
                (it.artistName.trim().equals(c.name.trim(), ignoreCase = true) || it.title.trim().equals(c.name.trim(), ignoreCase = true))
        }?.let {
            repository.updateBubbleChat(it.copy(characterId = c.id))
            return@withLock it.id
        }
        repository.addBubbleChat(BubbleChatEntity(title = c.name, artistName = c.name, artistAvatarUri = c.avatarUri, characterId = c.id))
    }

    /** The Pinterest account of [c]: the one already linked, else a fresh one with a starter tab and the character's picture. */
    suspend fun pinterestIdFor(c: CharacterEntity): Long = linkLock.withLock {
        repository.pinterestPages.first().firstOrNull { it.characterId == c.id }?.let { return@withLock it.id }
        repository.addPinterestPage(
            com.fakegram.profile.data.PinterestPageEntity(
                characterId = c.id, avatarUri = c.avatarUri,
                tabs = listOf(com.fakegram.profile.data.PinTab(System.currentTimeMillis(), "All")).tabsToJson(),
                chips = listOf(com.fakegram.profile.data.PinChip("Less AI", null, 0xFFE3E3DC)).chipsToJson()
            )
        )
    }

    companion object {
        fun factory(application: Application, repository: ProfileRepository, prefs: ThemePreferences): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ShellViewModel(application, repository, prefs) as T
            }
    }
}

/** Handles are stored without the leading @ and without spaces. */
fun cleanHandle(raw: String): String = raw.trim().trimStart('@').replace(Regex("\\s+"), "_")
