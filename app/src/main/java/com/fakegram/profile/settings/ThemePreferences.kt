package com.fakegram.profile.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val ACTIVE_CHARACTER_KEY = longPreferencesKey("active_character")
    }

    val isDarkMode = context.dataStore.data.map { prefs -> prefs[DARK_MODE_KEY] ?: true }

    /** The character / group currently selected in the header pill (-1 = none chosen yet). */
    val activeCharacterId = context.dataStore.data.map { prefs -> prefs[ACTIVE_CHARACTER_KEY] ?: -1L }

    suspend fun setActiveCharacterId(id: Long) {
        context.dataStore.edit { prefs -> prefs[ACTIVE_CHARACTER_KEY] = id }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }
}
