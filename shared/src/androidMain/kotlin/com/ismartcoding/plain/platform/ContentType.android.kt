package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.extensions.getContentType
import com.ismartcoding.plain.appContext
import android.net.Uri
import java.io.File

actual fun getContentTypeForPath(path: String): String? {
    if (path.startsWith("content://")) return appContext.contentResolver.getType(Uri.parse(path))
    val file = File(path)
    if (!file.exists()) return null
    return file.name.getContentType()
}
