package com.fakegram.profile.ui.google

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.PanelEntity
import com.fakegram.profile.data.PanelFactEntity
import com.fakegram.profile.data.PanelImageEntity
import com.fakegram.profile.data.PanelItemEntity
import com.fakegram.profile.data.PanelUiState
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.media.MediaStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Backs the "Google" tab: the list of saved knowledge panels. */
class PanelsHomeViewModel(private val repository: ProfileRepository) : ViewModel() {

    val panels: StateFlow<List<PanelEntity>> = repository.panels.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    /** panelId -> first image uri, used as the list thumbnail. */
    val covers: StateFlow<Map<Long, String>> = repository.allPanelImages
        .map { images -> images.groupBy { it.panelId }.mapValues { (_, list) -> list.minBy { it.position }.uri } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun createPanel(onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repository.createPanel()) }
    }

    fun duplicatePanel(id: Long) {
        viewModelScope.launch { repository.duplicatePanel(id) }
    }

    fun deletePanel(id: Long) {
        viewModelScope.launch { repository.deletePanel(id) }
    }

    companion object {
        fun factory(repository: ProfileRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = PanelsHomeViewModel(repository) as T
            }
    }
}

/** Backs a single panel's editor + preview. */
class PanelViewModel(
    application: Application,
    private val repository: ProfileRepository,
    private val panelId: Long
) : AndroidViewModel(application) {

    val uiState: StateFlow<PanelUiState> = combine(
        repository.observePanel(panelId),
        repository.observePanelImages(panelId),
        repository.observePanelFacts(panelId),
        repository.observePanelItems(panelId)
    ) { panel, images, facts, items ->
        PanelUiState(panel, images, facts, items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PanelUiState())

    // Text fields save on every keystroke; serialising read-modify-write keeps the last edit last.
    private val panelLock = Mutex()

    fun updatePanel(transform: (PanelEntity) -> PanelEntity) {
        viewModelScope.launch {
            panelLock.withLock {
                val current = repository.getPanel(panelId) ?: return@withLock
                repository.updatePanel(transform(current))
            }
        }
    }

    // ---- images ----
    fun addImages(uris: List<Uri>) {
        viewModelScope.launch {
            var position = uiState.value.images.size
            for (uri in uris) {
                val path = MediaStorage.copyToInternal(getApplication(), uri) ?: continue
                repository.addPanelImage(PanelImageEntity(panelId = panelId, uri = path, position = position++))
            }
        }
    }

    fun deleteImage(id: Long) {
        viewModelScope.launch { repository.deletePanelImage(id) }
    }

    fun moveImage(image: PanelImageEntity, delta: Int) {
        viewModelScope.launch {
            val list = uiState.value.images.sortedBy { it.position }
            val from = list.indexOfFirst { it.id == image.id }
            val to = from + delta
            if (from < 0 || to !in list.indices) return@launch
            val reordered = list.toMutableList().apply { add(to, removeAt(from)) }
            reordered.forEachIndexed { i, img -> if (img.position != i) repository.updatePanelImage(img.copy(position = i)) }
        }
    }

    // ---- facts ----
    fun addFact() {
        viewModelScope.launch {
            repository.addPanelFact(PanelFactEntity(panelId = panelId, label = "Age", position = uiState.value.facts.size))
        }
    }

    fun updateFact(fact: PanelFactEntity) {
        viewModelScope.launch { repository.updatePanelFact(fact) }
    }

    fun deleteFact(id: Long) {
        viewModelScope.launch { repository.deletePanelFact(id) }
    }

    // ---- songs / members ----
    fun addItem(kind: String) {
        viewModelScope.launch {
            repository.addPanelItem(PanelItemEntity(panelId = panelId, kind = kind, position = uiState.value.items.size))
        }
    }

    fun updateItem(item: PanelItemEntity) {
        viewModelScope.launch { repository.updatePanelItem(item) }
    }

    fun setItemImage(item: PanelItemEntity, uri: Uri) {
        viewModelScope.launch {
            val path = MediaStorage.copyToInternal(getApplication(), uri) ?: return@launch
            repository.updatePanelItem(item.copy(imageUri = path))
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch { repository.deletePanelItem(id) }
    }

    companion object {
        fun factory(application: Application, repository: ProfileRepository, panelId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PanelViewModel(application, repository, panelId) as T
            }
    }
}
