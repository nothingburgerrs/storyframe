package com.fakegram.profile.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import kotlinx.coroutines.launch

/**
 * The two core tabs (Hub / Apps) on the pastel shell. The header pill and the floating tab bar sit
 * outside the animated content, so switching tabs only cross-fades the page - nothing is rebuilt.
 */
@Composable
fun ShellTabs(shell: ShellViewModel, onNavigate: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val dark = LocalStoryframeColors.current.isDark
    val palette = shellPalette(dark)
    val scope = rememberCoroutineScope()
    val active by shell.active.collectAsState()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSwitcher by remember { mutableStateOf(false) }

    ShellBackground(palette) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // The pill sizes to its content but may use all the room the theme button leaves.
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    CharacterPill(active, palette, onClick = { showSwitcher = true })
                }
                IconButton(onClick = { scope.launch { app.themePreferences.setDarkMode(!dark) } }) {
                    Icon(if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = "Toggle theme", tint = palette.text)
                }
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(140)) },
                label = "shellTab",
                modifier = Modifier.weight(1f)
            ) { t ->
                if (t == 0) HubScreen(shell, palette, onOpen = onNavigate, onOpenApps = { tab = 1 }) else AppsScreen(shell, palette, onOpen = onNavigate)
            }
        }
        FloatingTabBar(tab, { tab = it }, palette, Modifier.align(Alignment.BottomCenter))
    }

    if (showSwitcher) CharacterSwitcherSheet(shell, onDismiss = { showSwitcher = false })
}

@Composable
private fun FloatingTabBar(selected: Int, onSelect: (Int) -> Unit, palette: ShellPalette, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(palette.glass).navigationBarsPadding().padding(top = 8.dp, bottom = 6.dp)
    ) {
        TabItem("Hub", Icons.Filled.Home, selected == 0, palette, Modifier.weight(1f)) { onSelect(0) }
        TabItem("Apps", Icons.Filled.GridView, selected == 1, palette, Modifier.weight(1f)) { onSelect(1) }
    }
}

@Composable
private fun TabItem(label: String, icon: ImageVector, selected: Boolean, palette: ShellPalette, modifier: Modifier, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) palette.text else palette.sub, tween(200), label = "tabTint")
    Column(
        modifier = modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, fontSize = 11.5.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

private val Color0 = androidx.compose.ui.graphics.Color.Transparent
