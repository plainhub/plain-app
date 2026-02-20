package com.ismartcoding.plain.helpers

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utilities for content-addressable file hashing.
 *
 * Two-step dedup strategy
 * ─────────────────────────────────────────────────────────────
 * 1. Weak check (cheap):
 *      weakHash = SHA-256( first 4 KB ++ last 4 KB )
 *    Lookup: WHERE size = ? AND weak_hash = ?
 *    If no row found → file is new (skip step 2).
 *
 * 2. Strong check (only when weak hits):
 *      strongHash = SHA-256( entire file )
 *    This becomes the canonical fileId.
 *    If strongHash already exists in DB → reuse the record.
 *    Otherwise → insert new record with the computed hashes.
 */
object FileHashHelper {

    private const val EDGE_BYTES = 4 * 1024 // 4 KB

    /**
     * Compute the "weak" hash used for the cheap first-pass lookup.
     * SHA-256 of (first EDGE_BYTES bytes ++ last EDGE_BYTES bytes).
     * For files smaller than 2 × EDGE_BYTES the whole file is used.
     */
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

    /** [weakHash] variant operating on an in-memory byte array. */
    fun weakHash(data: ByteArray): String {
        val buf = if (data.size <= EDGE_BYTES * 2) {
            data
        } else {
            data.copyOfRange(0, EDGE_BYTES) + data.copyOfRange(data.size - EDGE_BYTES, data.size)
        }
        return sha256Bytes(buf)
    }

    /**
     * Full SHA-256 hash of the file – used as the canonical [DFile.id].
     */
    fun strongHash(file: File): String {
        return file.inputStream().use { sha256Stream(it) }
    }

    /**
     * Full SHA-256 hash of an arbitrary byte array (e.g. downloaded data).
     */
    fun strongHash(bytes: ByteArray): String = sha256Bytes(bytes)

    // ── internals ──────────────────────────────────────────────────────────

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
