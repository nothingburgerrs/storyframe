package com.fakegram.profile.ui.phone

import androidx.compose.runtime.compositionLocalOf

/**
 * Edit mode (true) lets every app on the fake phone be changed; view mode (false) turns the same screens
 * into a plain phone you can only use. Switched on the home screen and with the floating button, read by the apps.
 */
val LocalEditMode = compositionLocalOf { true }

/** Every screen of the one shared phone. */
object PhoneRoutes {
    const val HOME = "home"
    const val INSTAGRAM = "instagram"
    const val GOOGLE = "google"
    const val BUBBLE = "bubbles"
    const val YOUTUBE = "youtube"
    const val PINTEREST = "pinterest"
    const val MESSAGES = "messages"
    const val PHOTOS = "photos"
    const val NOTES = "notes"
    const val SETTINGS = "settings"
}
