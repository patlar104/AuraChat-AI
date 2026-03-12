package com.aurachat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

object ImageAttachmentStore {

    private const val ATTACHMENTS_DIRECTORY = "chat_attachments"

    suspend fun importSelectedImage(
        context: Context,
        sourceUri: Uri,
    ): String = withContext(Dispatchers.IO) {
        val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIRECTORY).apply { mkdirs() }
        val extension = resolveExtension(context, sourceUri)
        val targetFile = File(
            attachmentsDir,
            "attachment_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension",
        )

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Couldn't read that image.")

        Uri.fromFile(targetFile).toString()
    }

    suspend fun decodeBitmap(
        context: Context,
        storedImageUri: String,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(storedImageUri)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun decodeVisionBitmap(
        context: Context,
        storedImageUri: String,
        maxEdgePx: Int = Constants.Gemini.MAX_VISION_IMAGE_EDGE_PX,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val decoded = decodeBitmap(context, storedImageUri) ?: return@withContext null
        downscaleIfNeeded(decoded, maxEdgePx)
    }

    private fun resolveExtension(context: Context, sourceUri: Uri): String {
        val mimeType = context.contentResolver.getType(sourceUri)
        val mappedExtension = mimeType?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        if (!mappedExtension.isNullOrBlank()) return mappedExtension

        val lastSegment = sourceUri.lastPathSegment.orEmpty()
        val inferredExtension = lastSegment.substringAfterLast('.', missingDelimiterValue = "")
        return inferredExtension.takeIf { it.isNotBlank() } ?: "jpg"
    }

    private fun downscaleIfNeeded(
        bitmap: Bitmap,
        maxEdgePx: Int,
    ): Bitmap {
        val longestEdge = maxOf(bitmap.width, bitmap.height)
        if (longestEdge <= maxEdgePx) return bitmap

        val scale = maxEdgePx.toFloat() / longestEdge.toFloat()
        val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
}
