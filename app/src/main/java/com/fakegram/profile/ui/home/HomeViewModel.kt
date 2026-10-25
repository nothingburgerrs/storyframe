package com.fakegram.profile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.data.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ProfileRepository) : ViewModel() {

    val profiles: StateFlow<List<ProfileEntity>> = repository.profiles.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun createProfile(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createProfile()
            onCreated(id)
        }
    }

    fun duplicateProfile(id: Long) {
        viewModelScope.launch { repository.duplicateProfile(id) }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch { repository.deleteProfile(id) }
    }

    companion object {
        fun factory(repository: ProfileRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
