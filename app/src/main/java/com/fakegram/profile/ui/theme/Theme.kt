package com.fakegram.profile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Instagram-like palette
val IgBlue = Color(0xFF0095F6)
val IgBlack = Color(0xFF0D1015)
val IgDarkSurface = Color(0xFF121212)
val IgDarkBorder = Color(0xFF262626)
val IgWhite = Color(0xFFFFFFFF)
val IgGraySecondaryLight = Color(0xFF8E8E8E)
val IgGraySecondaryDark = Color(0xFFA8A8A8)
val IgLightBorder = Color(0xFFDBDBDB)
val IgRed = Color(0xFFED4956)

data class StoryframeColors(
    val background: Color,
    val surface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val accentBlue: Color,
    val pillBackground: Color,
    val isDark: Boolean
)

val LocalStoryframeColors = staticCompositionLocalOf {
    StoryframeColors(
        background = IgWhite,
        surface = IgWhite,
        primaryText = IgBlack,
        secondaryText = IgGraySecondaryLight,
        border = IgLightBorder,
        accentBlue = IgBlue,
        pillBackground = Color(0xFFEFEFEF),
        isDark = false
    )
}

private val LightScheme = lightColorScheme(
    primary = IgBlue,
    background = IgWhite,
    surface = IgWhite,
    onBackground = IgBlack,
    onSurface = IgBlack
)

private val DarkScheme = darkColorScheme(
    primary = IgBlue,
    background = IgBlack,
    surface = IgDarkSurface,
    onBackground = IgWhite,
    onSurface = IgWhite
)

@Composable
fun StoryframeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val storyframeColors = if (darkTheme) {
        StoryframeColors(
            background = IgBlack,
            surface = IgDarkSurface,
            primaryText = IgWhite,
            secondaryText = IgGraySecondaryDark,
            border = IgDarkBorder,
            accentBlue = IgBlue,
            pillBackground = Color(0xFF262626),
            isDark = true
        )
    } else {
        StoryframeColors(
            background = IgWhite,
            surface = IgWhite,
            primaryText = IgBlack,
            secondaryText = IgGraySecondaryLight,
            border = IgLightBorder,
            accentBlue = IgBlue,
            pillBackground = Color(0xFFEFEFEF),
            isDark = false
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalStoryframeColors provides storyframeColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            content = content
        )
    }
}
