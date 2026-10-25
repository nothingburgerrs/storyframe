package com.fakegram.profile.ui.phone

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.rememberAsyncImagePainter
import com.fakegram.profile.StoryframeApp
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** One icon on the fake phone. */
private class HomeApp(
    val label: String,
    val route: String,
    val brush: Brush,
    val glyph: @Composable () -> Unit
)

/** An app that is currently zooming open from its icon. */
private class LaunchInfo(val app: HomeApp, val bounds: Rect)

private val InstagramBrush = Brush.linearGradient(listOf(Color(0xFFF9CE34), Color(0xFFEE2A7B), Color(0xFF6228D7)))
private val WhiteTile = Brush.linearGradient(listOf(Color.White, Color(0xFFF1F1F4)))

private val Apps = listOf(
    HomeApp("Messages", PhoneRoutes.MESSAGES, Brush.linearGradient(listOf(Color(0xFF67E27A), Color(0xFF19B93A)))) {
        Icon(Icons.Filled.Sms, null, tint = Color.White, modifier = Modifier.size(24.dp))
    },
    HomeApp("Google", PhoneRoutes.GOOGLE, WhiteTile) {
        Text(
            "G",
            style = TextStyle(
                brush = Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))),
                fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif
            )
        )
    },
    HomeApp("Instagram", PhoneRoutes.INSTAGRAM, InstagramBrush) {
        Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(22.dp))
    },
    HomeApp("YouTube", PhoneRoutes.YOUTUBE, Brush.linearGradient(listOf(Color(0xFFFF4B4B), Color(0xFFE0121F)))) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
    },
    HomeApp("Bubble", PhoneRoutes.BUBBLE, Brush.linearGradient(listOf(Color(0xFFB7A4FF), Color(0xFF7C8CFF)))) {
        Icon(Icons.Filled.ChatBubble, null, tint = Color.White, modifier = Modifier.size(22.dp))
    },
    HomeApp("Pinterest", PhoneRoutes.PINTEREST, Brush.linearGradient(listOf(Color(0xFFE60023), Color(0xFFE60023)))) {
        Text("P", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
    },
    HomeApp("Photos", PhoneRoutes.PHOTOS, WhiteTile) {
        Icon(Icons.Filled.PhotoLibrary, null, tint = Color(0xFFEE5A8F), modifier = Modifier.size(24.dp))
    },
    HomeApp("Notes", PhoneRoutes.NOTES, Brush.linearGradient(listOf(Color(0xFFFFE066), Color(0xFFF5B800)))) {
        Icon(Icons.Filled.Description, null, tint = Color.White, modifier = Modifier.size(24.dp))
    },
    HomeApp("Settings", PhoneRoutes.SETTINGS, Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFF636366)))) {
        Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
)

private val DockApps = Apps.take(4)
private val GridApps = Apps.drop(4)

/**
 * The first thing you see: a fake phone home screen. Tapping an icon zooms the app open from where it sits,
 * and the View / Edit switch decides whether the apps can be changed.
 */
@Composable
fun PhoneHomeScreen(editing: Boolean, onEditingChange: (Boolean) -> Unit, onNavigate: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as StoryframeApp
    val dark = LocalStoryframeColors.current.isDark
    val wallpaperPath by app.themePreferences.wallpaper.collectAsState(initial = null)
    var launching by remember { mutableStateOf<LaunchInfo?>(null) }

    val open: (HomeApp, Rect) -> Unit = { a, bounds -> if (launching == null) launching = LaunchInfo(a, bounds) }

    val gradient = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF241B45), Color(0xFF3B2347), Color(0xFF14101F)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFCFC6FF), Color(0xFFF2BBD9), Color(0xFFFFE3D1)))
    }

    Box(Modifier.fillMaxSize().background(gradient)) {
        wallpaperPath?.let { path ->
            Image(rememberAsyncImagePainter(path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) 0.25f else 0.08f)))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                ModeSwitch(editing = editing, onChange = onEditingChange)
            }

            Spacer(Modifier.height(22.dp))
            ClockWidget()
            Spacer(Modifier.height(30.dp))

            Column(Modifier.weight(1f)) {
                GridApps.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
                        row.forEach { a ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                AppIcon(a, showLabel = true, onTap = { open(a, it) })
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = if (dark) 0.14f else 0.32f))
                    .padding(vertical = 14.dp)
            ) {
                DockApps.forEach { a ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AppIcon(a, showLabel = false, onTap = { open(a, it) })
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(120.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.75f)))
            }
        }

        launching?.let { l -> LaunchOverlay(l) { onNavigate(l.app.route) } }
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
            style = TextStyle(shadow = Shadow(Color(0x33000000), blurRadius = 8f))
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
                style = TextStyle(shadow = Shadow(Color(0x66000000), blurRadius = 6f))
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
