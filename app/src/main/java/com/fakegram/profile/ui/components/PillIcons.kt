package com.fakegram.profile.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.fakegram.profile.data.PillIcon

fun pillIconVector(icon: PillIcon): ImageVector = when (icon) {
    PillIcon.MUSIC -> Icons.Filled.MusicNote
    PillIcon.HEART -> Icons.Filled.Favorite
    PillIcon.STAR -> Icons.Filled.Star
    PillIcon.SPARKLE -> Icons.Filled.AutoAwesome
    PillIcon.CAMERA -> Icons.Filled.PhotoCamera
    PillIcon.BOOK -> Icons.Filled.Book
    PillIcon.GAME -> Icons.Filled.SportsEsports
    PillIcon.COFFEE -> Icons.Filled.Coffee
    PillIcon.SMILE -> Icons.Filled.SentimentSatisfied
    PillIcon.LOCATION -> Icons.Filled.LocationOn
    PillIcon.MOON -> Icons.Filled.NightsStay
    PillIcon.FLOWER -> Icons.Filled.LocalFlorist
}

fun pillIconLabel(icon: PillIcon): String = when (icon) {
    PillIcon.MUSIC -> "Music"
    PillIcon.HEART -> "Heart"
    PillIcon.STAR -> "Star"
    PillIcon.SPARKLE -> "Sparkle"
    PillIcon.CAMERA -> "Camera"
    PillIcon.BOOK -> "Book"
    PillIcon.GAME -> "Game"
    PillIcon.COFFEE -> "Coffee"
    PillIcon.SMILE -> "Smile"
    PillIcon.LOCATION -> "Location"
    PillIcon.MOON -> "Moon"
    PillIcon.FLOWER -> "Flower"
}
