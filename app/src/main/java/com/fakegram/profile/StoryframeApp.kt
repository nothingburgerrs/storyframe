package com.fakegram.profile

import android.app.Application
import com.fakegram.profile.data.AppDatabase
import com.fakegram.profile.data.ProfileRepository
import com.fakegram.profile.settings.ThemePreferences

class StoryframeApp : Application() {
    lateinit var repository: ProfileRepository
        private set
    lateinit var themePreferences: ThemePreferences
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ProfileRepository(AppDatabase.getInstance(this))
        themePreferences = ThemePreferences(this)
    }
}
