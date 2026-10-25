package com.fakegram.profile.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val ACTIVE_CHARACTER_KEY = longPreferencesKey("active_character")
        private val WALLPAPER_KEY = stringPreferencesKey("wallpaper")
        private val FONT_KEY = stringPreferencesKey("font")
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

    /** Path of the picture used as the home screen wallpaper, or null for the built-in gradient. */
    val wallpaper = context.dataStore.data.map { prefs -> prefs[WALLPAPER_KEY]?.takeIf { it.isNotEmpty() } }

    /** Id of the phone's font (see AppFonts). */
    val font = context.dataStore.data.map { prefs -> prefs[FONT_KEY] ?: "roboto" }

    suspend fun setFont(id: String) {
        context.dataStore.edit { prefs -> prefs[FONT_KEY] = id }
    }

    suspend fun setWallpaper(path: String?) {
        context.dataStore.edit { prefs -> prefs[WALLPAPER_KEY] = path.orEmpty() }
    }
}
