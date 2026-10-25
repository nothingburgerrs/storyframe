package com.fakegram.profile.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Target crop shape/aspect: a circle (avatar, highlight cover) or a 4:5 portrait rect (posts). */
enum class CropShape { CIRCLE, RECT_4_5 }

/**
 * Simple crop step shown right after picking any image: pinch-zoom + drag-pan the picked photo
 * inside a fixed-shape frame, then confirm to crop+save exactly the framed region.
 */
@Composable
fun ImageCropDialog(
    uri: Uri,
    shape: CropShape,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    androidx.compose.runtime.LaunchedEffect(uri) {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
        }
        if (bitmap == null) loadFailed = true else sourceBitmap = bitmap
    }

    val frameWidthDp = 260.dp
    val frameHeightDp = if (shape == CropShape.CIRCLE) 260.dp else 260.dp * 5f / 4f
    val clipShape: Shape = if (shape == CropShape.CIRCLE) CircleShape else RoundedCornerShape(4.dp)

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Adjust photo") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    loadFailed -> Text("Couldn't load that image.")
                    sourceBitmap == null -> Box(
                        modifier = Modifier.size(frameWidthDp, frameHeightDp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    else -> {
                        val bmp = sourceBitmap!!
                        val frameWidthPx = with(density) { frameWidthDp.toPx() }
                        val frameHeightPx = with(density) { frameHeightDp.toPx() }
                        val baseScale = max(frameWidthPx / bmp.width, frameHeightPx / bmp.height)

                        Box(
                            modifier = Modifier
                                .size(frameWidthDp, frameHeightDp)
                                .clip(clipShape)
                                .background(Color.Black)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        offset += pan
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // NOTE: the previous version enlarged the Image with Modifier.size(...),
                            // but size() is clamped by the parent Box's max constraints, so the
                            // image could never grow past the frame and zoom looked like a no-op.
                            // Now: cover-fit image filling the frame, zoomed/panned via graphicsLayer.
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scale = (scale - 0.25f).coerceIn(1f, 5f) }) {
                                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out")
                            }
                            Text("Pinch or use +/- to zoom, drag to reposition", fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp))
                            IconButton(onClick = { scale = (scale + 0.25f).coerceIn(1f, 5f) }) {
                                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = sourceBitmap != null,
                onClick = {
                    val bmp = sourceBitmap ?: return@TextButton
                    val frameWidthPx = with(density) { frameWidthDp.toPx() }
                    val frameHeightPx = with(density) { frameHeightDp.toPx() }
                    val baseScale = max(frameWidthPx / bmp.width, frameHeightPx / bmp.height)
                    val totalScale = baseScale * scale
                    val displayedW = bmp.width * totalScale
                    val displayedH = bmp.height * totalScale
                    // Image is centered in the frame Box, then shifted by `offset`.
                    val imageTopLeftX = (frameWidthPx - displayedW) / 2f + offset.x
                    val imageTopLeftY = (frameHeightPx - displayedH) / 2f + offset.y

                    fun frameXToBitmap(fx: Float) = (fx - imageTopLeftX) / totalScale
                    fun frameYToBitmap(fy: Float) = (fy - imageTopLeftY) / totalScale

                    var bx0 = frameXToBitmap(0f)
                    var by0 = frameYToBitmap(0f)
                    var bx1 = frameXToBitmap(frameWidthPx)
                    var by1 = frameYToBitmap(frameHeightPx)

                    bx0 = bx0.coerceIn(0f, bmp.width.toFloat())
                    by0 = by0.coerceIn(0f, bmp.height.toFloat())
                    bx1 = bx1.coerceIn(0f, bmp.width.toFloat())
                    by1 = by1.coerceIn(0f, bmp.height.toFloat())

                    val cropW = (bx1 - bx0).roundToInt().coerceAtLeast(1)
                    val cropH = (by1 - by0).roundToInt().coerceAtLeast(1)
                    val safeX = bx0.roundToInt().coerceIn(0, bmp.width - cropW)
                    val safeY = by0.roundToInt().coerceIn(0, bmp.height - cropH)

                    val cropped = try {
                        Bitmap.createBitmap(bmp, safeX, safeY, cropW, cropH)
                    } catch (e: Exception) {
                        bmp
                    }
                    onConfirm(cropped)
                }
            ) { Text("Use photo") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
