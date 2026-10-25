package com.fakegram.profile.ui.util

import kotlin.math.abs

/**
 * Formats a free-text count field for display, Instagram-style.
 * If [raw] parses cleanly as a plain (optionally negative) integer, abbreviate it
 * (1200 -> "1.2K", 19400 -> "19.4K", 1847000 -> "1.8M"), rounding to 1 decimal and
 * dropping a trailing ".0" (8000 -> "8K"). Anything that doesn't parse as a plain
 * integer (already contains letters/symbols, e.g. "1.2M" or "???") is shown as-is.
 */
fun formatCount(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "0"
    val value = trimmed.toLongOrNull() ?: return trimmed
    val absValue = abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        absValue < 1000 -> trimmed
        absValue < 1_000_000 -> sign + oneDecimal(absValue / 1000.0) + "K"
        else -> sign + oneDecimal(absValue / 1_000_000.0) + "M"
    }
}

private fun oneDecimal(value: Double): String {
    val rounded = Math.round(value * 10) / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}
