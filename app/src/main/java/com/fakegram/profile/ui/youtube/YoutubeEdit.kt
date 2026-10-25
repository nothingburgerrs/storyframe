package com.fakegram.profile.ui.youtube

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fakegram.profile.data.YoutubeCommentEntity
import com.fakegram.profile.data.YoutubeVideoEntity
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.rememberCropPicker

/**
 * Everything about one video in one place: the pictures, every line of text, and what is shown. Each change is saved
 * as you type, so there is nothing to confirm and nothing that moves under your finger.
 */
@Composable
fun VideoEditSheet(
    video: YoutubeVideoEntity,
    update: ((YoutubeVideoEntity) -> YoutubeVideoEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pickFrame = rememberCropPicker(CropShape.RECT_16_9) { p -> update { it.copy(thumbnailUri = p) } }
    val pickAvatar = rememberCropPicker(CropShape.CIRCLE) { p -> update { it.copy(channelAvatarUri = p) } }
    val pickAd = rememberCropPicker(CropShape.RECT_4_5) { p -> update { it.copy(adImageUri = p) } }
    val total = if ("/" in video.durationText) video.durationText.substringAfter("/").trim() else ""

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 18.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit video", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PictureTile("Video picture", video.thumbnailUri, Modifier.weight(1f).aspectRatio(16f / 9f), RoundedCornerShape(10.dp), pickFrame)
                PictureTile("Channel", video.channelAvatarUri, Modifier.size(72.dp), CircleShape, pickAvatar)
            }

            SheetField("Title", video.id, video.title, singleLine = false) { t -> update { it.copy(title = t) } }
            SheetField("Channel", video.id, video.channelName.trimStart('@')) { t -> update { it.copy(channelName = t.trim().trimStart('@')) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetField("Views", video.id, video.viewsText, Modifier.weight(1f)) { t -> update { it.copy(viewsText = t) } }
                SheetField("Posted", video.id, video.timeText, Modifier.weight(1f)) { t -> update { it.copy(timeText = t) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetField("Likes", video.id, video.likeCount, Modifier.weight(1f)) { t -> update { it.copy(likeCount = t) } }
                SheetField("Comments (blank = real)", video.id, video.commentCount, Modifier.weight(1f)) { t -> update { it.copy(commentCount = t) } }
            }
            SheetField("Video length (like 20:21)", video.id, total) { t ->
                update { cur ->
                    val len = t.trim()
                    cur.copy(durationText = "${formatSeconds((cur.progress * parseSeconds(len)).toInt())} / $len")
                }
            }
            SheetField("Caption over the picture", video.id, video.subtitleText) { t -> update { it.copy(subtitleText = t) } }

            HorizontalDivider()
            SheetToggle("Player buttons", video.showControls) { on -> update { it.copy(showControls = on) } }
            SheetToggle("Caption", video.showCaption) { on -> update { it.copy(showCaption = on) } }
            SheetToggle("Title, channel and buttons", video.showPostInfo) { on -> update { it.copy(showPostInfo = on) } }
            SheetToggle("Recommended videos", video.showRelated) { on -> update { it.copy(showRelated = on) } }
            SheetToggle("Auto-dubbed", video.autoDubbed) { on -> update { it.copy(autoDubbed = on) } }
            SheetToggle("Subscribed", video.subscribed) { on -> update { it.copy(subscribed = on) } }
            SheetToggle("Ad under the comments", video.showAd) { on -> update { it.copy(showAd = on) } }
            if (video.showAd) {
                PictureTile("Ad picture", video.adImageUri, Modifier.fillMaxWidth().aspectRatio(2f), RoundedCornerShape(10.dp), pickAd)
            }

            HorizontalDivider()
            TextButton(onClick = onDelete) { Text("Delete video", color = Color(0xFFFF453A), fontSize = 16.sp) }
        }
    }
}

/** One comment: who wrote it, what it says, and the little numbers. */
@Composable
fun CommentEditSheet(
    comment: YoutubeCommentEntity,
    update: ((YoutubeCommentEntity) -> YoutubeCommentEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pickAvatar = rememberCropPicker(CropShape.CIRCLE) { p -> update { it.copy(avatarUri = p) } }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 18.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit comment", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            PictureTile("Picture", comment.avatarUri, Modifier.size(72.dp), CircleShape, pickAvatar)
            SheetField("Username", comment.id, comment.username.trimStart('@')) { t -> update { it.copy(username = t.trim().trimStart('@')) } }
            SheetField("Comment", comment.id, comment.text, singleLine = false) { t -> update { it.copy(text = t) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetField("Posted", comment.id, comment.timeText, Modifier.weight(1f)) { t -> update { it.copy(timeText = t) } }
                SheetField("Likes", comment.id, comment.likeCount, Modifier.weight(1f)) { t -> update { it.copy(likeCount = t) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Replies: ${comment.replyCount}", modifier = Modifier.weight(1f), fontSize = 16.sp)
                TextButton(onClick = { update { it.copy(replyCount = (it.replyCount - 1).coerceAtLeast(0)) } }) { Text("−", fontSize = 20.sp) }
                TextButton(onClick = { update { it.copy(replyCount = it.replyCount + 1) } }) { Text("+", fontSize = 20.sp) }
            }
            SheetToggle("“Translate to English” link", comment.hasTranslation) { on -> update { it.copy(hasTranslation = on) } }
            HorizontalDivider()
            TextButton(onClick = onDelete) { Text("Delete comment", color = Color(0xFFFF453A), fontSize = 16.sp) }
        }
    }
}

@Composable
private fun SheetField(
    label: String, key: Long, initial: String, modifier: Modifier = Modifier, singleLine: Boolean = true, onChange: (String) -> Unit
) {
    // The box keeps its own text while you type, so what you see never jumps when the saved copy updates.
    var text by remember(key) { mutableStateOf(initial) }
    OutlinedTextField(
        value = text, onValueChange = { text = it; onChange(it) }, label = { Text(label) },
        singleLine = singleLine, modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SheetToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp)
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun PictureTile(label: String, uri: String?, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape, onPick: () -> Unit) {
    Box(
        modifier.clip(shape).background(Color(0xFF3A3A3A)).clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Person, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
            Text(label, color = Color(0xFFAAAAAA), fontSize = 11.sp)
        }
    }
}
