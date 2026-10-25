package com.fakegram.profile.ui.shell

import com.fakegram.profile.data.CharacterEntity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.data.Platforms
import com.fakegram.profile.data.ScheduleEventEntity
import com.fakegram.profile.data.isGroup
import com.fakegram.profile.data.memberIdList
import com.fakegram.profile.ui.theme.LocalStoryframeColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** A postage-stamp outline: a rectangle with a zig-zag (perforated) edge. */
class StampShape(private val tooth: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = tooth * density.density
        val path = Path().apply {
            val nx = (size.width / (t * 2)).toInt().coerceAtLeast(2)
            val ny = (size.height / (t * 2)).toInt().coerceAtLeast(2)
            val sx = size.width / nx
            val sy = size.height / ny
            moveTo(0f, 0f)
            for (i in 0 until nx) { lineTo(sx * i + sx / 2, t); lineTo(sx * (i + 1), 0f) }
            for (i in 0 until ny) { lineTo(size.width - t, sy * i + sy / 2); lineTo(size.width, sy * (i + 1)) }
            for (i in 0 until nx) { lineTo(size.width - sx * i - sx / 2, size.height - t); lineTo(size.width - sx * (i + 1), size.height) }
            for (i in 0 until ny) { lineTo(t, size.height - sy * i - sy / 2); lineTo(0f, size.height - sy * (i + 1)) }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Short badge text for a platform, used on days that have no photo. */
fun platformBadge(id: String): String = when (id) {
    Platforms.INSTAGRAM -> "IG"
    Platforms.GOOGLE -> "G"
    Platforms.BUBBLE -> "B"
    Platforms.YOUTUBE -> "YT"
    else -> id.take(2).uppercase()
}

/** Whose events the calendar shows: only the selected character's. */
@Composable
fun rememberCalendarOwners(shell: ShellViewModel): Set<Long> {
    val active by shell.active.collectAsState()
    val characters by shell.characters.collectAsState()
    val c = active
    // Every character, and every group, has its own calendar: nothing from other characters is mixed in.
    return remember(c) { if (c == null) emptySet() else setOf(c.id) }
}

/** Month calendar: each day shows its most recent photo as a stamp. Tapping a day opens that day's page. */
@Composable
fun CalendarScreen(shell: ShellViewModel, onOpenDay: (String) -> Unit, onBack: () -> Unit) {
    val palette = shellPalette(LocalStoryframeColors.current.isDark)
    val characters by shell.characters.collectAsState()
    val active by shell.active.collectAsState()
    val events by shell.events.collectAsState()
    val ownerIds = rememberCalendarOwners(shell)

    var month by remember { mutableStateOf(YearMonth.now()) }

    // Newest first, so a day's cell always shows what was added last.
    val byDate = remember(events, ownerIds) { events.filter { it.characterId in ownerIds }.sortedByDescending { it.id }.groupBy { it.date } }
    val birthdays = remember(characters, ownerIds) { characters.filter { it.id in ownerIds && it.birthday.length >= 10 }.groupBy { it.birthday.substring(5) } }

    ShellBackground(palette) {
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.text) }
                Spacer(Modifier.weight(1f))
                val who = active
                if (who != null) {
                    Row(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(palette.glass).border(1.dp, palette.glassBorder, RoundedCornerShape(20.dp)).padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CharacterAvatar(who.avatarUri, 26.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(if (who.name.endsWith("s", ignoreCase = true)) "${who.name}' calendar" else "${who.name}'s calendar", color = palette.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text("%02d".format(month.monthValue), color = palette.text, fontFamily = ShellSerif, fontSize = 72.sp, lineHeight = 72.sp, fontWeight = FontWeight.Light)
                Column(Modifier.padding(start = 6.dp, bottom = 4.dp).weight(1f)) {
                    Text("${month.year}", color = palette.sub, fontSize = 13.sp)
                    Text(
                        month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH), color = palette.sub, maxLines = 1, softWrap = false,
                        fontFamily = ShellSerif, fontStyle = FontStyle.Italic, fontSize = 26.sp, lineHeight = 28.sp
                    )
                }
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month", tint = palette.sub) }
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month", tint = palette.sub) }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).forEach { d ->
                    Text(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), color = palette.sub, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))

            // Sunday-first grid starting on the Sunday on/before the 1st.
            val first = month.atDay(1)
            val start = first.minusDays((first.dayOfWeek.value % 7).toLong())
            val weeks = ((first.dayOfWeek.value % 7) + month.lengthOfMonth() + 6) / 7
            val today = LocalDate.now()
            for (w in 0 until weeks) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (d in 0 until 7) {
                        val date = start.plusDays((w * 7 + d).toLong())
                        val inMonth = YearMonth.from(date) == month
                        DayCell(
                            date, inMonth, byDate[date.toString()].orEmpty(), birthdays.containsKey(date.toString().substring(5)),
                            date == today, palette, Modifier.weight(1f)
                        ) { if (inMonth) onOpenDay(date.toString()) }
                    }
                }
            }

            WeeklyGoals(characters.filter { it.id in ownerIds }, events, palette, onGoal = { c, n -> shell.setWeeklyGoal(c, n) }, onReshuffle = { c -> shell.reshuffle(c) })
        }
    }
}

/** How many times each character (or group member) should post per week, with this week's progress from the calendar. */
@Composable
private fun WeeklyGoals(owners: List<CharacterEntity>, events: List<ScheduleEventEntity>, palette: ShellPalette, onGoal: (CharacterEntity, Int) -> Unit, onReshuffle: (CharacterEntity) -> Unit) {
    val today = LocalDate.now()
    val weekStart = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val weekEnd = weekStart.plusDays(6)
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text("Posts per week", color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text("Pick how many posts a week. They are placed on random days that are not back to back. Edit a day to keep it.", color = palette.sub, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        owners.forEach { c ->
            val goal = c.postsPerWeek ?: 0
            val done = events.count { e ->
                e.characterId == c.id && runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { d -> !d.isBefore(weekStart) && !d.isAfter(weekEnd) } == true
            }
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(20.dp)).background(palette.glass).border(1.dp, palette.glassBorder, RoundedCornerShape(20.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CharacterAvatar(c.avatarUri, 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name.ifBlank { "Unnamed" }, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        if (goal > 0) "$done of $goal this week" + if (done >= goal) "  \u2713" else "" else "$done planned this week \u00B7 no goal set",
                        color = palette.sub, fontSize = 12.5.sp
                    )
                    if (goal > 0) {
                        Text(
                            "Shuffle days", color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).background(palette.chip).clickable { onReshuffle(c) }.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(palette.chip)) {
                            Box(Modifier.fillMaxWidth((done.toFloat() / goal).coerceIn(0f, 1f)).height(5.dp).background(if (done >= goal) Color(0xFF3DBE6C) else ShellAccent))
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(34.dp).clip(CircleShape).background(palette.chip).clickable { onGoal(c, (goal - 1).coerceAtLeast(0)) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, "Fewer", tint = palette.text, modifier = Modifier.size(18.dp))
                }
                Text(if (goal > 0) "$goal" else "-", color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(30.dp))
                Box(Modifier.size(34.dp).clip(CircleShape).background(palette.chip).clickable { onGoal(c, (goal + 1).coerceAtMost(21)) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, "More", tint = palette.text, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate, inMonth: Boolean, events: List<ScheduleEventEntity>, birthday: Boolean,
    isToday: Boolean, palette: ShellPalette, modifier: Modifier, onClick: () -> Unit
) {
    val latest = events.firstOrNull()
    val photo = events.firstNotNullOfOrNull { it.imageUri }
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier.aspectRatio(0.66f).alpha(if (inMonth) 1f else 0.35f)
            .clip(cardShape).background(if (inMonth) palette.glass else Color.Transparent)
            .then(if (isToday) Modifier.border(1.5.dp, palette.text, cardShape) else Modifier.border(1.dp, if (inMonth) palette.glassBorder else Color.Transparent, cardShape))
            .clickable(enabled = inMonth, onClick = onClick).padding(5.dp)
    ) {
        Text("${date.dayOfMonth}", color = if (inMonth) palette.text else palette.sub, fontSize = 13.sp)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                photo != null -> AsyncImage(
                    model = photo, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(StampShape(2f)).background(Color(0xFFE6E6E8))
                )
                latest != null -> Box(
                    Modifier.fillMaxSize().clip(StampShape(2f)).background(TileLavender), contentAlignment = Alignment.Center
                ) {
                    Text(platformBadge(latest.platform), color = TileInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (events.size > 1) Text(
                        "+${events.size - 1}", color = TileInk.copy(alpha = 0.6f), fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 3.dp)
                    )
                }
                birthday -> Box(Modifier.fillMaxSize().clip(StampShape(2f)).background(TileBlush), contentAlignment = Alignment.Center) {
                    Text("🎂", fontSize = 14.sp)
                }
            }
        }
    }
}
