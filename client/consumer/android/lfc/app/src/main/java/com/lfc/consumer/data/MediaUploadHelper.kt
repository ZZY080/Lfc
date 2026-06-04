package com.lfc.consumer.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object MediaUploadHelper {
    data class UploadFile(val file: File, val mimeType: String, val fileName: String)

    fun uriToUploadFile(context: Context, uri: Uri, prefix: String = "upload"): UploadFile {
        val rawMimeType = context.contentResolver.getType(uri)
        val isVideo = isVideoMime(rawMimeType, uri)
        val extension = if (isVideo) {
            videoMimeToExtension(rawMimeType, uri)
        } else {
            mimeTypeToExtension(rawMimeType, uri)
        }
        val mimeType = if (isVideo) {
            normalizeVideoMimeType(rawMimeType, extension)
        } else {
            normalizeMimeType(rawMimeType, extension)
        }
        val fileName = "${prefix}_${System.currentTimeMillis()}.$extension"
        val file = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException(if (isVideo) "无法读取视频" else "无法读取图片")
        return UploadFile(file, mimeType, fileName)
    }

    private fun isVideoMime(mimeType: String?, uri: Uri): Boolean {
        if (mimeType?.startsWith("video/") == true) return true
        val segment = uri.lastPathSegment?.lowercase().orEmpty()
        return segment.endsWith(".mp4") ||
            segment.endsWith(".mov") ||
            segment.endsWith(".webm") ||
            segment.endsWith(".3gp")
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

    private fun videoMimeToExtension(mimeType: String?, uri: Uri): String {
        return when {
            mimeType?.contains("quicktime", ignoreCase = true) == true -> "mov"
            mimeType?.contains("webm", ignoreCase = true) == true -> "webm"
            mimeType?.contains("3gpp", ignoreCase = true) == true -> "3gp"
            uri.lastPathSegment?.endsWith(".mov", ignoreCase = true) == true -> "mov"
            uri.lastPathSegment?.endsWith(".webm", ignoreCase = true) == true -> "webm"
            uri.lastPathSegment?.endsWith(".3gp", ignoreCase = true) == true -> "3gp"
            else -> "mp4"
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

    private fun normalizeVideoMimeType(mimeType: String?, extension: String): String {
        if (!mimeType.isNullOrBlank() && mimeType.startsWith("video/")) {
            return mimeType
        }
        return when (extension) {
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            else -> "video/mp4"
        }
    }
}
