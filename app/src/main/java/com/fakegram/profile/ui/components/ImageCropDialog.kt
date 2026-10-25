package com.fakegram.profile.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.requiredSize
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
enum class CropShape { CIRCLE, RECT_4_5, RECT_16_9 }

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
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    // Zoom is relative to "the picture just covers the frame"; pan is in pixels from the centre.
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

    val aspect = when (shape) {
        CropShape.CIRCLE -> 1f
        CropShape.RECT_4_5 -> 4f / 5f
        CropShape.RECT_16_9 -> 16f / 9f
    }

    // Full-screen stage: the WHOLE picture is visible, dimmed outside the crop frame, so you can see what you are cutting.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val bmp = sourceBitmap
            when {
                loadFailed -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load that image.", color = Color.White)
                        TextButton(onClick = onCancel) { Text("Close") }
                    }
                }
                bmp == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                    val stageW = constraints.maxWidth.toFloat()
                    val stageH = constraints.maxHeight.toFloat()
                    val frameW = minOf(stageW * 0.9f, stageH * 0.5f * aspect)
                    val frameH = frameW / aspect
                    val baseScale = max(frameW / bmp.width, frameH / bmp.height)
                    val density = LocalDensity.current

                    fun clampOffset(o: Offset, s: Float): Offset {
                        val maxX = max(0f, (bmp.width * baseScale * s - frameW) / 2f)
                        val maxY = max(0f, (bmp.height * baseScale * s - frameH) / 2f)
                        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
                    }

                    Box(
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                offset = clampOffset(offset + pan, scale)
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        // The whole picture at "cover the frame" size, moved/zoomed with graphicsLayer. It is not clipped to the frame.
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                            modifier = Modifier
                                .requiredSize(with(density) { (bmp.width * baseScale).toDp() }, with(density) { (bmp.height * baseScale).toDp() })
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                        // Dim everything outside the frame, then outline the frame.
                        androidx.compose.foundation.Canvas(
                            Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        ) {
                            val tl = Offset((size.width - frameW) / 2f, (size.height - frameH) / 2f)
                            drawRect(Color(0xB3000000))
                            if (shape == CropShape.CIRCLE) {
                                drawCircle(Color.Black, radius = frameW / 2f, center = center, blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                                drawCircle(Color.White, radius = frameW / 2f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                            } else {
                                drawRect(Color.Black, topLeft = tl, size = androidx.compose.ui.geometry.Size(frameW, frameH), blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                                drawRect(Color.White, topLeft = tl, size = androidx.compose.ui.geometry.Size(frameW, frameH), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                            }
                        }
                    }

                    // All controls live at the TOP, well away from the phone's own buttons at the bottom.
                    Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(top = 28.dp, start = 12.dp, end = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
                            IconButton(onClick = { scale = (scale - 0.25f).coerceIn(1f, 6f); offset = clampOffset(offset, scale) }) {
                                Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
                            }
                            IconButton(onClick = { scale = (scale + 0.25f).coerceIn(1f, 6f); offset = clampOffset(offset, scale) }) {
                                Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                            }
                            Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                val totalScale = baseScale * scale
                                val displayedW = bmp.width * totalScale
                                val displayedH = bmp.height * totalScale
                                // The picture is centred in the frame, then shifted by `offset`.
                                val imageTopLeftX = (frameW - displayedW) / 2f + offset.x
                                val imageTopLeftY = (frameH - displayedH) / 2f + offset.y

                                var bx0 = (0f - imageTopLeftX) / totalScale
                                var by0 = (0f - imageTopLeftY) / totalScale
                                var bx1 = (frameW - imageTopLeftX) / totalScale
                                var by1 = (frameH - imageTopLeftY) / totalScale

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
                        ) { Text("Use photo", color = Color.White) }
                        }
                        Text("Pinch to zoom, drag to move. Dark = cut off.", color = Color(0xFFBBBBBB), fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp), modifier = Modifier.padding(horizontal = 12.dp).align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
}
