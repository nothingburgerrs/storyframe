package com.fakegram.profile.ui.pinterest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.data.PinChip
import com.fakegram.profile.data.PinTab
import com.fakegram.profile.data.PinterestPageEntity
import com.fakegram.profile.data.chipsToJson
import com.fakegram.profile.data.tabsToJson
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import kotlinx.coroutines.flow.first

/**
 * The phone has one Pinterest. It opens the account that already holds the most pins (so what was set up
 * earlier is what you see), or makes a fresh one when there is none.
 */
@Composable
fun PinterestHost(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    var pageId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        val repo = app.repository
        val pages = repo.pinterestPages.first()
        pageId = if (pages.isEmpty()) {
            repo.addPinterestPage(
                PinterestPageEntity(
                    tabs = listOf(PinTab(System.currentTimeMillis(), "All")).tabsToJson(),
                    chips = listOf(PinChip("Less AI", null, 0xFFE3E3DC)).chipsToJson()
                )
            )
        } else {
            val pins = repo.observeAllPins().first()
            (pages.maxByOrNull { page -> pins.count { it.pageId == page.id } } ?: pages.first()).id
        }
    }
    val id = pageId
    if (id == null) {
        val dark = LocalStoryframeColors.current.isDark
        Box(Modifier.fillMaxSize().background(if (dark) Color.Black else Color.White))
    } else {
        PinterestScreen(pageId = id, onBack = onBack)
    }
}
