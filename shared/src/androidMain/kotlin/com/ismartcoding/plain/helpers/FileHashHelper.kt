package com.ismartcoding.plain.helpers

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object FileHashHelper {

    private const val EDGE_BYTES = 4 * 1024 // 4 KB

    fun weakHash(file: File): String {
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
        return sha256Bytes(buf)
    }

    fun weakHash(data: ByteArray): String {
        val buf = if (data.size <= EDGE_BYTES * 2) {
            data
        } else {
            data.copyOfRange(0, EDGE_BYTES) + data.copyOfRange(data.size - EDGE_BYTES, data.size)
        }
        return sha256Bytes(buf)
    }

    fun strongHash(file: File): String {
        return file.inputStream().use { sha256Stream(it) }
    }

    fun strongHash(bytes: ByteArray): String = sha256Bytes(bytes)

    private fun sha256Bytes(data: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(data)
            .toHexString()
    }

    private fun sha256Stream(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHexString()
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }
}