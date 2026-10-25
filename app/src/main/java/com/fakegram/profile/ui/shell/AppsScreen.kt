package com.fakegram.profile.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakegram.profile.data.CharacterEntity
import com.fakegram.profile.data.Platform
import com.fakegram.profile.data.Platforms
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.platformList
import com.fakegram.profile.media.MediaStorage
import kotlinx.coroutines.launch

/** Routes the launcher can open. Instagram goes straight into the character's own (existing) profile. */
object ShellRoutes {
    const val INSTAGRAM_LIST = "instagram"
    const val GOOGLE_LIST = "google"
    const val BUBBLE_LIST = "bubbles"
    const val ACCOUNTS = "accounts"
    const val CALENDAR = "calendar"
    /** The original Hub / Apps tabs, kept as the place to manage characters. */
    const val STUDIO = "studio"

    /** 0 = every video, otherwise only the videos of that character. */
    fun youtubeList(characterId: Long) = "youtubeList/$characterId"
}

/** Tab 2 - the active character's apps as plain rows, then the library of everything made so far. */
@Composable
fun AppsScreen(shell: ShellViewModel, palette: ShellPalette, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val active by shell.active.collectAsState()
    val profiles by shell.profiles.collectAsState()
    val c = active
    val apps = c?.platformList?.mapNotNull { id -> Platforms.all.firstOrNull { it.id == id } }.orEmpty()

    val scope = rememberCoroutineScope()

    /**
     * Where a tap goes. Instagram opens the character's own profile. For Google and Bubble an
     * individual goes straight to their own search / chat (created on first use); a group opens
     * the list of everyone in it. YouTube opens that character's videos.
     */
    fun open(p: Platform, character: CharacterEntity) {
        when (p.id) {
            Platforms.INSTAGRAM -> onOpen(
                character.instagramProfileId?.takeIf { id -> profiles.any { it.id == id } }?.let { "profile/$it" } ?: ShellRoutes.INSTAGRAM_LIST
            )
            Platforms.GOOGLE ->
                if (character.isGroup) onOpen("groupGoogle/${character.id}")
                else scope.launch { onOpen("panel/${shell.panelIdFor(character)}") }
            Platforms.BUBBLE ->
                if (character.isGroup) onOpen("groupBubble/${character.id}")
                else scope.launch { onOpen("bubble/${shell.chatIdFor(character)}") }
            Platforms.YOUTUBE -> onOpen(ShellRoutes.youtubeList(character.id))
            Platforms.PINTEREST -> scope.launch { onOpen("pinterest/${shell.pinterestIdFor(character)}") }
            else -> MediaStorage.toast(context, "${p.label} isn’t built yet")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 130.dp)) {
        Text("Apps", color = palette.text, fontFamily = ShellSerif, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(
            if (c != null) "What ${c.name} has on their phone" else "Pick a character to see their apps",
            color = palette.sub, fontSize = 15.sp
        )
        Spacer(Modifier.height(22.dp))

        if (apps.isEmpty()) {
            Text(
                if (c != null) "No apps enabled. Edit this character to turn some on." else "Create a character first from the avatar above.",
                color = palette.sub, modifier = Modifier.fillMaxWidth().glass(palette, RoundedCornerShape(22.dp)).padding(22.dp)
            )
        } else {
            RowsCard(palette) {
                apps.forEachIndexed { i, p ->
                    if (i > 0) RowDivider(palette)
                    AppRow(p, palette, onList = listRoute(p.id)?.let { r -> { onOpen(r) } }) { c?.let { open(p, it) } }
                }
                RowDivider(palette)
                LibraryRow("Accounts", Icons.Filled.People, palette) { onOpen(ShellRoutes.ACCOUNTS) }
            }
            Text("Tap the list icon on a row to see everything made in that app.", color = palette.sub, fontSize = 12.5.sp, modifier = Modifier.padding(start = 4.dp, top = 10.dp))
        }
    }
}

private fun listRoute(id: String): String? = when (id) {
    Platforms.INSTAGRAM -> ShellRoutes.INSTAGRAM_LIST
    Platforms.GOOGLE -> ShellRoutes.GOOGLE_LIST
    Platforms.BUBBLE -> ShellRoutes.BUBBLE_LIST
    Platforms.YOUTUBE -> ShellRoutes.youtubeList(0)
    else -> null
}

@Composable
private fun RowsCard(palette: ShellPalette, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(palette, RoundedCornerShape(24.dp)).padding(vertical = 4.dp)) { content() }
}

@Composable
private fun RowDivider(palette: ShellPalette) {
    HorizontalDivider(modifier = Modifier.padding(start = 70.dp, end = 16.dp), color = palette.glassBorder)
}

@Composable
private fun AppRow(platform: Platform, palette: ShellPalette, onList: (() -> Unit)? = null, onClick: () -> Unit) {
    val iconShape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (platform.built) 1f else 0.55f).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(iconShape).background(appBrush(platform.id), iconShape), contentAlignment = Alignment.Center) { AppGlyph(platform.id) }
        Spacer(Modifier.width(14.dp))
        Text(platform.label, color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (!platform.built) Text("soon", color = palette.sub, fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
        if (onList != null) {
            Icon(
                Icons.Filled.GridView, "All ${platform.label}", tint = palette.sub,
                modifier = Modifier.padding(end = 4.dp).size(34.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onList).padding(7.dp)
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.sub, modifier = Modifier.size(20.dp))
    }
}

internal fun appBrush(id: String): Brush = when (id) {
    Platforms.INSTAGRAM -> Brush.linearGradient(listOf(Color(0xFFF9CE34), Color(0xFFEE2A7B), Color(0xFF6228D7)))
    Platforms.GOOGLE -> Brush.linearGradient(listOf(Color.White, Color(0xFFF1F1F4)))
    Platforms.BUBBLE -> Brush.linearGradient(listOf(Color(0xFFB7A4FF), Color(0xFF7C8CFF)))
    Platforms.YOUTUBE -> Brush.linearGradient(listOf(Color(0xFFFF4B4B), Color(0xFFE0121F)))
    Platforms.PINTEREST -> Brush.linearGradient(listOf(Color(0xFFE60023), Color(0xFFE60023)))
    else -> Brush.linearGradient(listOf(Color(0xFFB7A4FF), Color(0xFF7C8CFF)))
}

@Composable
internal fun AppGlyph(id: String) {
    val glyph = Modifier.size(22.dp)
    when (id) {
        Platforms.INSTAGRAM -> Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = glyph)
        Platforms.GOOGLE -> Text(
            "G",
            style = TextStyle(
                brush = Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))),
                fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif
            )
        )
        Platforms.BUBBLE -> Icon(Icons.Filled.ChatBubble, null, tint = Color.White, modifier = glyph)
        Platforms.YOUTUBE -> Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Platforms.PINTEREST -> Text("P", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        else -> Icon(Icons.Filled.GridView, null, tint = Color.White, modifier = glyph)
    }
}

@Composable
private fun LibraryRow(title: String, icon: ImageVector, palette: ShellPalette, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(palette.chip), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = palette.text, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.sub, modifier = Modifier.size(20.dp))
    }
}
