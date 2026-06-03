package com.lfc.consumer.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object MediaUploadHelper {
    data class UploadFile(val file: File, val mimeType: String, val fileName: String)

    fun uriToUploadFile(context: Context, uri: Uri, prefix: String = "upload"): UploadFile {
        val rawMimeType = context.contentResolver.getType(uri)
        val extension = mimeTypeToExtension(rawMimeType, uri)
        val mimeType = normalizeMimeType(rawMimeType, extension)
        val fileName = "${prefix}_${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("无法读取图片")
        return UploadFile(file, mimeType, fileName)
    }

    private fun mimeTypeToExtension(mimeType: String?, uri: Uri): String {
        return when {
            mimeType?.contains("png", ignoreCase = true) == true -> "png"
            mimeType?.contains("webp", ignoreCase = true) == true -> "webp"
            mimeType?.contains("gif", ignoreCase = true) == true -> "gif"
            uri.lastPathSegment?.endsWith(".png", ignoreCase = true) == true -> "png"
            uri.lastPathSegment?.endsWith(".webp", ignoreCase = true) == true -> "webp"
            uri.lastPathSegment?.endsWith(".gif", ignoreCase = true) == true -> "gif"
            else -> "jpg"
        }
    }

    private fun normalizeMimeType(mimeType: String?, extension: String): String {
        if (!mimeType.isNullOrBlank() && mimeType != "image/*" && mimeType.startsWith("image/")) {
            return mimeType
        }
        return when (extension) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }
}
