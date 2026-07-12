package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.crypto.sha256

object FileHashHelper {

    private const val EDGE_BYTES = 4 * 1024 // 4 KB

    fun weakHash(data: ByteArray): String {
        val buf = if (data.size <= EDGE_BYTES * 2) {
            data
        } else {
            data.copyOfRange(0, EDGE_BYTES) + data.copyOfRange(data.size - EDGE_BYTES, data.size)
        }
        return sha256Hex(buf)
    }

    fun strongHash(bytes: ByteArray): String = sha256Hex(bytes)

    private fun sha256Hex(data: ByteArray): String {
        return sha256(data).toHexString()
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
