package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.lib.crypto.sha256
import com.ismartcoding.plain.lib.extensions.toHexString

/** Bytes used for the edge-based [FileHashHelper.weakHash] fragment hash. */
internal const val FileHashEdgeBytes = 4 * 1024

/** SHA-256 hex of [data]. */
internal fun sha256Hex(data: ByteArray): String {
    return sha256(data).toHexString()
}

object FileHashHelper {

    fun weakHash(data: ByteArray): String {
        val buf = if (data.size <= FileHashEdgeBytes * 2) {
            data
        } else {
            data.copyOfRange(0, FileHashEdgeBytes) + data.copyOfRange(data.size - FileHashEdgeBytes, data.size)
        }
        return sha256Hex(buf)
    }

    fun strongHash(bytes: ByteArray): String = sha256Hex(bytes)
}
