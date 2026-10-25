package com.fakegram.profile.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.fakegram.profile.data.CharacterEntity
import com.fakegram.profile.data.Platform
import com.fakegram.profile.data.Platforms
import com.fakegram.profile.data.ProfileEntity
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.platformList
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** One icon on the fake phone. [route] is resolved on tap because some apps create their data on first use. */
private class HomeApp(
    val key: String,
    val label: String,
    val brush: Brush,
    val glyph: @Composable () -> Unit,
    val route: suspend () -> String
)

/** An app that is currently zooming open from its icon. */
private class LaunchInfo(val app: HomeApp, val bounds: Rect, val route: String)

private suspend fun routeFor(shell: ShellViewModel, p: Platform, c: CharacterEntity, profiles: List<ProfileEntity>): String = when (p.id) {
    Platforms.INSTAGRAM ->
        c.instagramProfileId?.takeIf { id -> profiles.any { it.id == id } }?.let { "profile/$it" } ?: ShellRoutes.INSTAGRAM_LIST
    Platforms.GOOGLE -> if (c.isGroup) "groupGoogle/${c.id}" else "panel/${shell.panelIdFor(c)}"
    Platforms.BUBBLE -> if (c.isGroup) "groupBubble/${c.id}" else "bubble/${shell.chatIdFor(c)}"
    Platforms.YOUTUBE -> ShellRoutes.youtubeList(c.id)
    Platforms.PINTEREST -> "pinterest/${shell.pinterestIdFor(c)}"
    else -> ShellRoutes.STUDIO
}

/**
 * The first thing you see: a fake phone home screen. The active character's apps sit in a dock and a grid,
 * tapping one zooms it open from its icon, and the View / Edit switch decides whether the apps can be changed.
 */
@Composable
fun PhoneHomeScreen(shell: ShellViewModel, onNavigate: (String) -> Unit) {
    val dark = LocalStoryframeColors.current.isDark
    val palette = shellPalette(dark)
    val active by shell.active.collectAsState()
    val profiles by shell.profiles.collectAsState()
    val editing by shell.editMode.collectAsState()
    val scope = rememberCoroutineScope()
    var showSwitcher by remember { mutableStateOf(false) }
    var launching by remember { mutableStateOf<LaunchInfo?>(null) }

    val c = active
    val characterApps: List<HomeApp> = if (c == null) emptyList() else c.platformList
        .mapNotNull { id -> Platforms.all.firstOrNull { it.id == id && it.built } }
        .map { p -> HomeApp(p.id, p.label, appBrush(p.id), { AppGlyph(p.id) }) { routeFor(shell, p, c, profiles) } }
    val lightTile = Brush.linearGradient(listOf(Color.White, Color(0xFFF1F1F4)))
    val extras = listOf(
        HomeApp("calendar", "Calendar", lightTile, {
            Icon(Icons.Filled.CalendarMonth, null, tint = Color(0xFFE5384F), modifier = Modifier.size(24.dp))
        }) { ShellRoutes.CALENDAR },
        HomeApp("accounts", "Accounts", Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFF636366))), {
            Icon(Icons.Filled.People, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }) { ShellRoutes.ACCOUNTS },
        HomeApp("studio", "Studio", Brush.linearGradient(listOf(TileLavender, TileBlush)), {
            Icon(Icons.Filled.AutoAwesome, null, tint = TileInk, modifier = Modifier.size(24.dp))
        }) { ShellRoutes.STUDIO }
    )
    val dockApps = characterApps.take(4)
    val gridApps = characterApps.drop(4) + extras

    val open: (HomeApp, Rect) -> Unit = { app, bounds ->
        if (launching == null) scope.launch { launching = LaunchInfo(app, bounds, app.route()) }
    }

    val wallpaper = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF241B45), Color(0xFF3B2347), Color(0xFF14101F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFCFC6FF), Color(0xFFF2BBD9), Color(0xFFFFE3D1)))
    }

    Box(Modifier.fillMaxSize().background(wallpaper)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            // top: who's phone this is, and the View / Edit switch
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    CharacterPill(c, palette, onClick = { showSwitcher = true })
                }
                Spacer(Modifier.width(10.dp))
                ModeSwitch(editing = editing, onChange = { shell.setEditMode(it) })
            }

            Spacer(Modifier.height(22.dp))
            ClockWidget()
            Spacer(Modifier.height(30.dp))

            // the grid
            Column(Modifier.weight(1f)) {
                if (c == null) {
                    Text(
                        "Create a character in Studio to fill this phone.",
                        color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
                gridApps.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
                        row.forEach { app ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                AppIcon(app, showLabel = true, onTap = { open(app, it) })
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // the dock
            if (dockApps.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = if (dark) 0.14f else 0.32f))
                        .padding(vertical = 14.dp)
                ) {
                    dockApps.forEach { app ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            AppIcon(app, showLabel = false, onTap = { open(app, it) })
                        }
                    }
                    repeat(4 - dockApps.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.75f)))
            }
        }

        launching?.let { l -> LaunchOverlay(l) { onNavigate(l.route) } }
        if (showSwitcher) CharacterSwitcherSheet(shell, onDismiss = { showSwitcher = false })
    }
}

@Composable
private fun ClockWidget() {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(10_000)
        }
    }
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            time, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light,
            style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color(0x33000000), blurRadius = 8f))
        )
        Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** View = use the phone like a real one. Edit = every screen inside can be changed. */
@Composable
private fun ModeSwitch(editing: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.25f)).padding(3.dp)) {
        ModeSegment("View", selected = !editing) { onChange(false) }
        ModeSegment("Edit", selected = editing) { onChange(true) }
    }
}

@Composable
private fun ModeSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Color.White else Color.Transparent, tween(180), label = "modeBg")
    val fg by animateColorAsState(if (selected) Color(0xFF141414) else Color.White, tween(180), label = "modeFg")
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppIcon(app: HomeApp, showLabel: Boolean, onTap: (Rect) -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, tween(110), label = "iconPress")
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(72.dp).scale(pressScale)
            .clickable(interactionSource = source, indication = null) { onTap(bounds) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(60.dp).onGloballyPositioned { bounds = it.boundsInRoot() }
                .clip(shape).background(app.brush),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }) { app.glyph() }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                app.label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color(0x66000000), blurRadius = 6f))
            )
        }
    }
}

/** The tapped icon grows from where it sits to fill the screen, like opening an app on a real phone. */
@Composable
private fun LaunchOverlay(l: LaunchInfo, onEnd: () -> Unit) {
    val progress = remember(l) { Animatable(0f) }
    LaunchedEffect(l) {
        progress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        onEnd()
    }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullW = constraints.maxWidth.toFloat()
        val fullH = constraints.maxHeight.toFloat()
        val t = progress.value
        val left = lerp(l.bounds.left, 0f, t)
        val top = lerp(l.bounds.top, 0f, t)
        val w = lerp(l.bounds.width, fullW, t)
        val h = lerp(l.bounds.height, fullH, t)
        val radius = lerp(16f, 0f, t)
        val glyphAlpha = (1f - t / 0.5f).coerceIn(0f, 1f)
        Box(
            Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                .clip(RoundedCornerShape(radius.dp)).background(l.app.brush),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.graphicsLayer { alpha = glyphAlpha; scaleX = 1.45f; scaleY = 1.45f }) { l.app.glyph() }
        }
    }
}
