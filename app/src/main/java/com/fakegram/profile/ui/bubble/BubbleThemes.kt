package com.fakegram.profile.ui.bubble

import androidx.compose.ui.graphics.Color

/** Colour set for one Bubble chat look. Values are sampled from real Bubble screenshots. */
class BubbleTheme(
    val id: String,
    val label: String,
    val background: Color,
    val topBar: Color,
    val topBarIcon: Color,
    val inputBar: Color,
    val inputField: Color,
    val bubble: Color,
    val text: Color,
    /** Second (translated) line under the dashed divider. */
    val translation: Color,
    val time: Color,
    val name: Color,
    val ring: Color,
    val badgeStart: Color,
    val badgeEnd: Color,
    /** Corner radius of the tag in dp: small = squarish label, large = pill. */
    val badgeRadius: Int,
    val meBubble: Color,
    val meText: Color,
    val divider: Color,
    val dateChip: Color,
    val dateText: Color
)

object BubbleThemes {
    val Lavender = BubbleTheme(
        id = "LAVENDER", label = "Lavender",
        background = Color(0xFFE0D8F1), topBar = Color.White, topBarIcon = Color(0xFF1A1A1A),
        inputBar = Color.White, inputField = Color.White,
        bubble = Color.White, text = Color(0xFF1C1C1E), translation = Color(0xFF6A4FD3),
        time = Color(0xFF9A93A8), name = Color(0xFF1C1C1E), ring = Color(0xFF6FA4F0),
        badgeStart = Color(0xFF7C8CFF), badgeEnd = Color(0xFF3FC3F2), badgeRadius = 2,
        meBubble = Color(0xFF8E6CF5), meText = Color.White, divider = Color(0xFFDCD6E8),
        dateChip = Color(0x33000000), dateText = Color.White
    )

    val Pink = BubbleTheme(
        id = "PINK", label = "Pink",
        background = Color(0xFFDDB1CB), topBar = Color(0xFFF5EAF1), topBarIcon = Color(0xFF1A1A1A),
        inputBar = Color(0xFFF5EAF1), inputField = Color.White,
        bubble = Color(0xFFEEECEB), text = Color(0xFF111111), translation = Color(0xFF8654D8),
        time = Color(0xFF9A8C97), name = Color(0xFF111111), ring = Color(0xFFB29CF3),
        badgeStart = Color(0xFF5A3BF0), badgeEnd = Color(0xFFCBC5F6), badgeRadius = 10,
        meBubble = Color(0xFF8654D8), meText = Color.White, divider = Color(0xFFD8D4D2),
        dateChip = Color(0x33000000), dateText = Color.White
    )

    val Grey = BubbleTheme(
        id = "GREY", label = "Classic",
        background = Color(0xFFEEEEEE), topBar = Color.White, topBarIcon = Color(0xFF1A1A1A),
        inputBar = Color.White, inputField = Color(0xFFF2F2F2),
        bubble = Color.White, text = Color(0xFF111111), translation = Color(0xFF2C439C),
        time = Color(0xFF9B9B9B), name = Color(0xFF111111), ring = Color(0xFF4A67D6),
        badgeStart = Color(0xFF2F4CC8), badgeEnd = Color(0xFF2F4CC8), badgeRadius = 10,
        meBubble = Color(0xFF4A67D6), meText = Color.White, divider = Color(0xFFDDDDDD),
        dateChip = Color(0x33000000), dateText = Color.White
    )

    val Dark = BubbleTheme(
        id = "DARK", label = "Dark",
        background = Color(0xFF17171B), topBar = Color(0xFF212126), topBarIcon = Color(0xFFF2F2F2),
        inputBar = Color(0xFF212126), inputField = Color(0xFF2D2D34),
        bubble = Color(0xFF2E2E35), text = Color(0xFFF2F2F5), translation = Color(0xFFB7A2FF),
        time = Color(0xFF8A8A94), name = Color(0xFFF2F2F5), ring = Color(0xFF7C8CFF),
        badgeStart = Color(0xFF6C5CE7), badgeEnd = Color(0xFF9C8CFF), badgeRadius = 10,
        meBubble = Color(0xFF6C5CE7), meText = Color.White, divider = Color(0xFF45454F),
        dateChip = Color(0x33FFFFFF), dateText = Color(0xFFD0D0D8)
    )

    val all = listOf(Lavender, Pink, Grey, Dark)
    fun byId(id: String): BubbleTheme = all.firstOrNull { it.id == id } ?: Lavender
}
