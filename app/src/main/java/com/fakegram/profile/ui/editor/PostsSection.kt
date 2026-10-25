package com.fakegram.profile.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fakegram.profile.data.MediaType
import com.fakegram.profile.data.PostEntity
import com.fakegram.profile.media.MediaStorage
import com.fakegram.profile.ui.components.CropShape
import com.fakegram.profile.ui.components.ImageCropDialog

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddPostSheet(viewModel: EditorViewModel, forTagged: Boolean = false, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    var singleCropUri by remember { mutableStateOf<Uri?>(null) }
    var singleCropIsVideo by remember { mutableStateOf(false) }
    var carouselQueue by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var carouselResults by remember { mutableStateOf<List<String>>(emptyList()) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { singleCropIsVideo = false; singleCropUri = uri } else onDismiss()
    }
    // "Video" posts no longer store a real video file — picking an image just sets it as the
    // fake-video thumbnail; the VIDEO mediaType (and its grid badge) is what makes it a "video" post.
    val videoThumbnailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { singleCropIsVideo = true; singleCropUri = uri } else onDismiss()
    }
    val carouselLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) { carouselResults = emptyList(); carouselQueue = uris } else onDismiss()
    }

    singleCropUri?.let { uri ->
        ImageCropDialog(
            uri = uri,
            shape = CropShape.RECT_4_5,
            onCancel = { singleCropUri = null; onDismiss() },
            onConfirm = { bitmap ->
                singleCropUri = null
                val path = MediaStorage.saveBitmapToInternal(context, bitmap)
                if (path != null) {
                    if (singleCropIsVideo) viewModel.addVideoPost(path, isTagged = forTagged)
                    else viewModel.addImagePost(path, isTagged = forTagged)
                }
                onDismiss()
            }
        )
    }

    if (carouselQueue.isNotEmpty()) {
        ImageCropDialog(
            uri = carouselQueue.first(),
            shape = CropShape.RECT_4_5,
            onCancel = { carouselQueue = emptyList(); carouselResults = emptyList(); onDismiss() },
            onConfirm = { bitmap ->
                val path = MediaStorage.saveBitmapToInternal(context, bitmap)
                val newResults = if (path != null) carouselResults + path else carouselResults
                val remaining = carouselQueue.drop(1)
                carouselResults = newResults
                carouselQueue = remaining
                if (remaining.isEmpty()) {
                    viewModel.addCarouselPost(newResults, isTagged = forTagged)
                    onDismiss()
                }
            }
        )
    }

    if (singleCropUri == null && carouselQueue.isEmpty()) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (forTagged) "Add tagged post" else "Add post", modifier = Modifier.padding(bottom = 12.dp))
                OutlinedButton(
                    onClick = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pick image") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { videoThumbnailLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pick video (thumbnail image)") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { carouselLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create carousel (pick multiple images)") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditPostSheet(viewModel: EditorViewModel, post: PostEntity, onOpenPost: () -> Unit = {}, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    var replaceCropUri by remember { mutableStateOf<Uri?>(null) }
    var replaceCropIsVideo by remember { mutableStateOf(false) }

    val replaceImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { replaceCropIsVideo = false; replaceCropUri = uri } else onDismiss()
    }
    val replaceVideoThumbnailLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { replaceCropIsVideo = true; replaceCropUri = uri } else onDismiss()
    }

    replaceCropUri?.let { uri ->
        ImageCropDialog(
            uri = uri,
            shape = CropShape.RECT_4_5,
            onCancel = { replaceCropUri = null; onDismiss() },
            onConfirm = { bitmap ->
                replaceCropUri = null
                val path = MediaStorage.saveBitmapToInternal(context, bitmap)
                if (path != null) viewModel.replacePostMedia(post, path, isVideo = replaceCropIsVideo)
                onDismiss()
            }
        )
    }

    if (replaceCropUri == null) {
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit post", modifier = Modifier.padding(bottom = 12.dp))
                Button(
                    onClick = { onDismiss(); onOpenPost() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open post") }
                Spacer(Modifier.height(8.dp))
                if (post.mediaType != MediaType.CAROUSEL) {
                    OutlinedButton(
                        onClick = { viewModel.changeMediaTypeToCarousel(post); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Convert to carousel") }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = { replaceImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Replace with image") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { replaceVideoThumbnailLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Replace with video (thumbnail image)") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.togglePinned(post); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (post.isPinned) "Unpin" else "Pin") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.toggleTagged(post); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (post.isTagged) "Remove from tagged" else "Mark as tagged") }
                Spacer(Modifier.height(8.dp))
                if (post.mediaType == MediaType.VIDEO) {
                    OutlinedButton(
                        onClick = { viewModel.toggleReel(post); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (post.isReel) "Remove from reels" else "Mark as reel") }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = { viewModel.deletePost(post.id); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
