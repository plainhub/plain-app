package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.chaCha20Encrypt
import kotlin.io.encoding.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateId(): String {
    val bytes = Uuid.random().toString().encodeToByteArray()
    var value = 0L
    for (i in 0 until 8) {
        value = (value shl 8) or (bytes[i].toLong() and 0xFF)
    }
    return value.toString(36)
}

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