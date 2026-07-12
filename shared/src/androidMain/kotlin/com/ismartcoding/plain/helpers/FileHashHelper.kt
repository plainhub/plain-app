package com.ismartcoding.plain.helpers

import java.io.File
import java.security.MessageDigest

fun FileHashHelper.weakHash(file: File): String {
    val size = file.length()
    val buf = if (size <= EDGE_BYTES * 2) {
        file.readBytes()
    } else {
        val first = ByteArray(EDGE_BYTES)
        val last = ByteArray(EDGE_BYTES)
        file.inputStream().use { it.read(first) }
        file.inputStream().use { inp ->
            inp.skip(size - EDGE_BYTES)
            inp.read(last)
        }
        first + last
    }
    return weakHash(buf)
}

fun FileHashHelper.strongHash(file: File): String {
    return file.inputStream().use { stream ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (stream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}

private const val EDGE_BYTES = 4 * 1024 // 4 KB
