package com.fakegram.profile.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Editorial serif used for the big headlines of the outer shell. */
val ShellSerif: FontFamily = FontFamily.Serif

val ShellAccent = Color(0xFF7A6CF0)

/** Soft tile colours used on the Hub (lavender / blush / ink). */
val TileLavender = Color(0xFFDCD5FF)
val TileBlush = Color(0xFFF2BBD9)
val TileInk = Color(0xFF151515)

/** Minimal off-white / white-card look; one set per light/dark mode. */
@Immutable
class ShellPalette(
    val isDark: Boolean,
    val text: Color,
    val sub: Color,
    /** Card fill and its hairline border. */
    val glass: Color,
    val glassBorder: Color,
    val background: Color,
    /** Chip / input fill sitting on the background. */
    val chip: Color
)

private val LightPalette = ShellPalette(
    isDark = false,
    text = Color(0xFF141414),
    sub = Color(0xFF8A8A90),
    glass = Color(0xFFFFFFFF),
    glassBorder = Color(0xFFEDEDEF),
    background = Color(0xFFF7F7F8),
    chip = Color(0xFFEDEDEF)
)

private val DarkPalette = ShellPalette(
    isDark = true,
    text = Color(0xFFF4F4F5),
    sub = Color(0xFF9A9AA2),
    glass = Color(0xFF1B1B1D),
    glassBorder = Color(0xFF2A2A2D),
    background = Color(0xFF0F0F10),
    chip = Color(0xFF262629)
)

fun shellPalette(dark: Boolean): ShellPalette = if (dark) DarkPalette else LightPalette

/** Full-screen flat background. */
@Composable
fun ShellBackground(palette: ShellPalette, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.fillMaxSize().background(palette.background), content = content)
}

/** Flat card: solid fill + hairline border, children clipped to the shape. */
fun Modifier.glass(palette: ShellPalette, shape: Shape = RoundedCornerShape(24.dp)): Modifier =
    this.background(palette.glass, shape).border(1.dp, palette.glassBorder, shape).clip(shape)

@Composable
fun CharacterAvatar(uri: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(Color(0xFFD9D2E3)),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.58f))
        }
    }
}
