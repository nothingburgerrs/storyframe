package com.fakegram.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.fakegram.profile.ui.theme.AppFontFamilies
import com.fakegram.profile.ui.theme.AppFonts
import com.fakegram.profile.ui.theme.LocalAppFontFamily
import com.fakegram.profile.ui.nav.StoryframeNavHost
import com.fakegram.profile.ui.theme.StoryframeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as StoryframeApp
        setContent {
            val isDark by app.themePreferences.isDarkMode.collectAsState(initial = true)
            val fontId by app.themePreferences.font.collectAsState(initial = AppFonts.DEFAULT_ID)
            val context = LocalContext.current
            val family = remember(fontId) { AppFontFamilies.familyFor(context, AppFonts.byId(fontId)) }
            CompositionLocalProvider(LocalAppFontFamily provides family) {
                StoryframeTheme(darkTheme = isDark) {
                    StoryframeNavHost(repository = app.repository)
                }
            }
        }
    }
}
