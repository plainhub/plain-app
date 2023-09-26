package com.ismartcoding.lib.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun Uri.getFileName(context: Context): String {
    if (scheme == ContentResolver.SCHEME_FILE) {
        return this.lastPathSegment ?: ""
    }

    var fileName = ""
    if (scheme == ContentResolver.SCHEME_CONTENT) {
        context.contentResolver.query(this, arrayOf(
            OpenableColumns.DISPLAY_NAME
        ), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val cache = mutableMapOf<String, Int>()
                fileName = cursor.getStringValue(OpenableColumns.DISPLAY_NAME, cache)
            }
        }
    }

    if (fileName.isEmpty()) {
        // Fallback: Extract file name from the URI
        fileName = this.lastPathSegment ?: ""
    }

    return fileName
}
