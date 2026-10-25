package com.fakegram.profile.ui.youtube

import com.fakegram.profile.ui.theme.inAppFont
import com.fakegram.profile.ui.phone.LocalEditMode
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.data.YoutubeVideoEntity
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.rememberCropPicker

/**
 * One recommended video, used by the home feed and by the list under a video's comments. Everything on it is
 * edited right here: the length badge, the title, the channel / views / time line, and the avatar. Tap the
 * picture to open the video; hold it for change-image / delete.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FeedCard(v: YoutubeVideoEntity, onSave: (YoutubeVideoEntity) -> Unit, onOpen: () -> Unit, onDelete: () -> Unit) {
    val edit = LocalEditMode.current
    var menu by remember { mutableStateOf(false) }
    val pickFrame = rememberCropPicker(CropShape.RECT_16_9) { onSave(v.copy(thumbnailUri = it)) }
    // durationText is "now / total"; the badge on a card is the total.
    val now = v.durationText.substringBefore("/").trim().ifEmpty { "0:00" }
    val total = if ("/" in v.durationText) v.durationText.substringAfter("/").trim() else ""
    val sub = TextStyle(color = YtSub, fontSize = 12.sp)

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF1A1A1A))
                .combinedClickable(onClick = onOpen, onLongClick = { if (edit) menu = true })
        ) {
            if (v.thumbnailUri != null) AsyncImage(v.thumbnailUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.PlayArrow, null, tint = YtSub, modifier = Modifier.size(34.dp))
                if (edit) Text("Hold to add an image", color = YtSub, fontSize = 11.sp)
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color(0xCC000000), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                TimeInput(total, { onSave(v.copy(durationText = "$now / ${it.trim()}")) }, TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Open") }, onClick = { menu = false; onOpen() })
                DropdownMenuItem(text = { Text("Change image") }, onClick = { menu = false; pickFrame() })
                DropdownMenuItem(text = { Text("Delete video") }, onClick = { menu = false; onDelete() })
            }
        }
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
            YtAvatar(v.channelAvatarUri, 36.dp) { onSave(v.copy(channelAvatarUri = it)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                InlineText(
                    v.title, { onSave(v.copy(title = it.trim())) },
                    TextStyle(color = YtText, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
                    Modifier.fillMaxWidth(), placeholder = "Video title"
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InlineText(v.channelName.trimStart('@'), { onSave(v.copy(channelName = it.trim().trimStart('@'))) }, sub, Modifier.weight(1f, fill = false), placeholder = "channel", singleLine = true)
                    Text(" • ", style = sub)
                    InlineText(v.viewsText, { onSave(v.copy(viewsText = it.trim())) }, sub, placeholder = "views", singleLine = true, wrap = true)
                    Text(" • ", style = sub)
                    InlineText(v.timeText, { onSave(v.copy(timeText = it.trim())) }, sub, placeholder = "time", singleLine = true, wrap = true)
                }
            }
            Box {
                Icon(Icons.Filled.MoreVert, "More", tint = YtText, modifier = Modifier.padding(8.dp).size(20.dp).clickable { if (edit) menu = true })
            }
        }
    }
}

/** "1:10:19" or "20:21" to seconds; 0 when it cannot be read. */
internal fun parseSeconds(text: String): Int {
    val parts = text.trim().split(":").mapNotNull { it.trim().toIntOrNull() }
    return when (parts.size) {
        1 -> parts[0]
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> 0
    }
}

internal fun formatSeconds(total: Int): String {
    val t = total.coerceAtLeast(0)
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Types digits like a stopwatch: 1 0 1 9 becomes 10:19, 1 1 0 1 9 becomes 1:10:19. Number keyboard, saved when you tap away. */
@Composable
internal fun TimeInput(value: String, onCommit: (String) -> Unit, style: TextStyle, modifier: Modifier = Modifier, placeholder: String = "0:00") {
    fun format(digits: String): String = when {
        digits.length <= 2 -> digits
        digits.length <= 4 -> digits.dropLast(2) + ":" + digits.takeLast(2)
        else -> digits.dropLast(4) + ":" + digits.dropLast(2).takeLast(2) + ":" + digits.takeLast(2)
    }
    var field by remember(value) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))) }
    var focused by remember { mutableStateOf(false) }
    androidx.compose.foundation.text.BasicTextField(
        value = field,
        onValueChange = { new ->
            val text = format(new.text.filter { it.isDigit() }.take(6))
            field = androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(text.length))
        },
        readOnly = !LocalEditMode.current, textStyle = style.inAppFont(), singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
        modifier = modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min).onFocusChanged {
            if (focused && !it.isFocused && field.text != value) onCommit(field.text)
            focused = it.isFocused
        },
        decorationBox = { inner ->
            Box { if (field.text.isEmpty()) Text(placeholder, style = style.copy(color = Color(0x99FFFFFF))); inner() }
        }
    )
}
