package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.extensions.getContentType
import java.io.File

actual fun getContentTypeForPath(path: String): String? {
    val file = File(path)
    if (!file.exists()) return null
    return file.name.getContentType().toString()
}