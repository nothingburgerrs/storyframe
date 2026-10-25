package com.fakegram.profile.ui.pinterest

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.PinChip
import com.fakegram.profile.data.PinChipColors
import com.fakegram.profile.data.PinEntity
import com.fakegram.profile.data.PinTab
import com.fakegram.profile.data.PinterestPageEntity
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.data.chipsToJson
import com.fakegram.profile.data.tabsToJson
import com.fakegram.profile.data.toPinChips
import com.fakegram.profile.data.toPinTabs
import com.fakegram.profile.media.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One fake Pinterest account: its page settings (tabs, search, chips, profile picture) and all its pins. */
class PinterestViewModel(
    application: Application,
    private val repository: ProfileRepository,
    private val pageId: Long
) : AndroidViewModel(application) {

    val page: StateFlow<PinterestPageEntity?> = repository.observePinterestPage(pageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val pins: StateFlow<List<PinEntity>> = repository.observePins(pageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val lock = Mutex()

    /** Read-modify-write on the freshest row, so quick consecutive edits do not overwrite each other. */
    fun updatePage(transform: (PinterestPageEntity) -> PinterestPageEntity) {
        viewModelScope.launch {
            lock.withLock {
                val current = repository.getPinterestPage(pageId) ?: return@withLock
                repository.updatePinterestPage(transform(current))
            }
        }
    }

    // ---- tabs on the home feed ----
    fun addTab(onCreated: (Long) -> Unit) {
        val id = System.currentTimeMillis()
        updatePage { p -> p.copy(tabs = (p.tabs.toPinTabs() + PinTab(id, "New tab")).tabsToJson()) }
        onCreated(id)
    }

    fun renameTab(id: Long, label: String) {
        updatePage { p -> p.copy(tabs = p.tabs.toPinTabs().map { if (it.id == id) it.copy(label = label) else it }.tabsToJson()) }
    }

    fun deleteTab(id: Long) {
        updatePage { p -> p.copy(tabs = p.tabs.toPinTabs().filter { it.id != id }.tabsToJson()) }
        viewModelScope.launch { repository.deletePinsOfTab(pageId, id) }
    }

    // ---- chips on the search page ----
    fun addChip() {
        updatePage { p ->
            val chips = p.chips.toPinChips()
            p.copy(chips = (chips + PinChip("New", null, PinChipColors[chips.size % PinChipColors.size])).chipsToJson())
        }
    }

    fun updateChip(index: Int, chip: PinChip) {
        updatePage { p -> p.copy(chips = p.chips.toPinChips().mapIndexed { i, c -> if (i == index) chip else c }.chipsToJson()) }
    }

    fun deleteChip(index: Int) {
        updatePage { p -> p.copy(chips = p.chips.toPinChips().filterIndexed { i, _ -> i != index }.chipsToJson()) }
    }

    // ---- pins ----
    private suspend fun store(uri: Uri): Pair<String, Float>? = withContext(Dispatchers.IO) {
        val path = MediaStorage.copyToInternal(getApplication(), uri) ?: return@withContext null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        val aspect = if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth.toFloat() / opts.outHeight else 0.75f
        path to aspect.coerceIn(0.45f, 1.8f)
    }

    fun addPins(uris: List<Uri>, screen: Int, tabId: Long) {
        viewModelScope.launch {
            var position = (pins.value.maxOfOrNull { it.position } ?: -1) + 1
            for (uri in uris) {
                val (path, aspect) = store(uri) ?: continue
                repository.addPin(PinEntity(pageId = pageId, screen = screen, tabId = tabId, imageUri = path, aspect = aspect, position = position++))
            }
        }
    }

    /** A text-only sponsored pin to fill in. */
    fun addEmptyPin(screen: Int, tabId: Long) {
        viewModelScope.launch {
            repository.addPin(PinEntity(pageId = pageId, screen = screen, tabId = tabId, position = (pins.value.maxOfOrNull { it.position } ?: -1) + 1))
        }
    }

    fun setPinImage(pin: PinEntity, uri: Uri) {
        viewModelScope.launch {
            val (path, aspect) = store(uri) ?: return@launch
            repository.updatePin(pin.copy(imageUri = path, aspect = aspect))
        }
    }

    fun savePin(pin: PinEntity) {
        viewModelScope.launch { repository.updatePin(pin) }
    }

    fun deletePin(id: Long) {
        viewModelScope.launch { repository.deletePin(id) }
    }

    companion object {
        fun factory(application: Application, repository: ProfileRepository, pageId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = PinterestViewModel(application, repository, pageId) as T
            }
    }
}
