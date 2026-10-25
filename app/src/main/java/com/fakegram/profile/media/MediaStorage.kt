package com.fakegram.profile.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaStorage {

    private fun mediaDir(context: Context): File {
        val dir = File(context.filesDir, "media")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Copies the content:// (or any) uri's bytes into internal storage, returning the absolute path. */
    fun copyToInternal(context: Context, uri: Uri, preferredExtension: String? = null): String? {
        return try {
            val resolver = context.contentResolver
            val ext = preferredExtension
                ?: resolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?: "jpg"
            val fileName = "${UUID.randomUUID()}.$ext"
            val outFile = File(mediaDir(context), fileName)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Saves an in-memory bitmap (e.g. a crop result) into internal storage, returning its path. */
    fun saveBitmapToInternal(context: Context, bitmap: Bitmap): String? {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val outFile = File(mediaDir(context), fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Extracts a thumbnail frame from a local video file path, saves as jpg, returns its path. */
    fun extractVideoThumbnail(context: Context, videoPath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            val bitmap: Bitmap? = retriever.getFrameAtTime(0)
            if (bitmap == null) return null
            val fileName = "${UUID.randomUUID()}_thumb.jpg"
            val outFile = File(mediaDir(context), fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            outFile.absolutePath
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Saves a bitmap into MediaStore Pictures/Storyframe. Returns true on success. */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
        return try {
            val resolver: ContentResolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Storyframe")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(itemUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
