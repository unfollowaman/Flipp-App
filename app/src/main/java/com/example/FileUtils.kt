package com.example

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

object FileUtils {
    /**
     * Safely resolve the display name for a Uri via SAF ContentResolver.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "Document"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        name = displayName
                    }
                }
            }
        } catch (_: Exception) {
            uri.path?.let { path ->
                val lastSegment = path.substringAfterLast("/")
                if (lastSegment.isNotBlank()) {
                    name = lastSegment
                }
            }
        }
        return name
    }

    /**
     * Safely resolve the human-readable file size string for a Uri.
     */
    fun getFileSizeString(context: Context, uri: Uri): String? {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    val sizeBytes = cursor.getLong(sizeIndex)
                    if (sizeBytes > 0) {
                        return when {
                            sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", sizeBytes / (1024f * 1024f))
                            sizeBytes >= 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024f)
                            else -> "$sizeBytes B"
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
