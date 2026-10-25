package com.fakegram.profile.ui.shell

import com.fakegram.profile.data.CharacterEntity
import com.fakegram.profile.data.ScheduleEventEntity
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.memberIdList
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

fun greeting(now: LocalDateTime = LocalDateTime.now()): String = when (now.hour) {
    in 5..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..21 -> "Good Evening"
    else -> "Late Night"
}

/** "1:20PM" */
fun clockText(now: LocalDateTime = LocalDateTime.now()): String {
    val h12 = if (now.hour % 12 == 0) 12 else now.hour % 12
    return "%d:%02d%s".format(h12, now.minute, if (now.hour < 12) "AM" else "PM")
}

fun weekdayName(date: LocalDate = LocalDate.now()): String = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
fun monthDayName(date: LocalDate = LocalDate.now()): String =
    "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${date.dayOfMonth}"

/** Whole days until the next occurrence of a YYYY-MM-DD birthday (0 = today); null if unset or invalid. */
fun daysUntilBirthday(iso: String, today: LocalDate = LocalDate.now()): Int? {
    val born = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return null
    var next = runCatching { born.withYear(today.year) }.getOrNull() ?: born.withYear(today.year).withDayOfMonth(28)
    if (next.isBefore(today)) next = next.plusYears(1)
    // 29 Feb falls back to 28 Feb in non-leap years via withYear
    return java.time.temporal.ChronoUnit.DAYS.between(today, next).toInt()
}

data class UpcomingBirthday(val character: CharacterEntity, val days: Int)

/** Characters whose birthday falls within [within] days, soonest first. */
fun upcomingBirthdays(characters: List<CharacterEntity>, within: Int = 60, today: LocalDate = LocalDate.now()): List<UpcomingBirthday> =
    characters.mapNotNull { c -> daysUntilBirthday(c.birthday, today)?.takeIf { it <= within }?.let { UpcomingBirthday(c, it) } }
        .sortedBy { it.days }

data class TodayEvent(val who: String, val platform: String, val note: String)

/** Events planned for today for [character] (and, for a group, for its members). */
fun todayEvents(
    character: CharacterEntity?,
    all: List<CharacterEntity>,
    events: List<ScheduleEventEntity>,
    today: LocalDate = LocalDate.now()
): List<TodayEvent> {
    if (character == null) return emptyList()
    val owners = if (character.isGroup) listOf(character) + all.filter { it.id in character.memberIdList } else listOf(character)
    val key = today.toString()
    return events.filter { e -> e.date == key && owners.any { it.id == e.characterId } }
        .map { e -> TodayEvent(owners.first { it.id == e.characterId }.name, e.platform, e.note) }
}
