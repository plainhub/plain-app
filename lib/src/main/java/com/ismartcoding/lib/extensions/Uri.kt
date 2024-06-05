package com.ismartcoding.lib.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

fun Uri.getFileName(context: Context): String {
    if (scheme == ContentResolver.SCHEME_FILE) {
        return this.lastPathSegment ?: ""
    }

    var fileName = ""
    if (scheme == ContentResolver.SCHEME_CONTENT) {
        fileName = context.contentResolver.queryOpenableFileName(this)
    }

    if (fileName.isEmpty()) {
        // Fallback: Extract file name from the URI
        fileName = this.lastPathSegment ?: ""
    }

    return fileName
}
