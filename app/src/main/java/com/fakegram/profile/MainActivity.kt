package com.fakegram.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fakegram.profile.ui.nav.StoryframeNavHost
import com.fakegram.profile.ui.theme.StoryframeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as StoryframeApp
        setContent {
            val isDark by app.themePreferences.isDarkMode.collectAsState(initial = true)
            StoryframeTheme(darkTheme = isDark) {
                StoryframeNavHost(repository = app.repository)
            }
        }
    }
}
