package com.ismartcoding.plain.platform

import android.webkit.MimeTypeMap

actual fun getExtensionFromMimeType(mimeType: String): String {
    return MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mimeType)
        .orEmpty()
        .trim()
}
