package com.fakegram.profile.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.fakegram.profile.media.MediaStorage

/**
 * Opens the photo picker, then the crop step for [shape], then hands back the saved file path.
 * Call the returned function from a click.
 */
@Composable
fun rememberCropPicker(shape: CropShape, onResult: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> pending = uri }
    pending?.let { uri ->
        ImageCropDialog(
            uri = uri, shape = shape,
            onCancel = { pending = null },
            onConfirm = { bitmap ->
                pending = null
                MediaStorage.saveBitmapToInternal(context, bitmap)?.let(onResult)
            }
        )
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
