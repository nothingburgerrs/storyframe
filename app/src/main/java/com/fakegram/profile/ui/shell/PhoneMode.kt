package com.fakegram.profile.ui.shell

import androidx.compose.runtime.compositionLocalOf

/**
 * Edit mode (true) lets every app on the fake phone be changed; view mode (false) turns the same screens
 * into a plain phone you can only use. Switched on the home screen and read by the apps.
 */
val LocalEditMode = compositionLocalOf { true }
