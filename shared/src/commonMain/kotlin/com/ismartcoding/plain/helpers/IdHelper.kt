package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.chaCha20Encrypt
import kotlin.io.encoding.Base64

fun getFileId(path: String): String {
    if (path.isEmpty()) {
        return ""
    }
    if (path.startsWith("https://", true) || path.startsWith("http://", true)) {
        return path
    }
    return Base64.encode(
        chaCha20Encrypt(TempData.urlToken, path),
    )
}