package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * Content-addressable file store.
 *
 * Files are stored at:
 *   {appDir}/{id[0..1]}/{id[2..3]}/{id}
 *
 * The realPath is derived deterministically from [id], so path resolution
 * never needs a database query – but we persist it for diagnostics / tooling.
 *
 * [id]       SHA-256 hex digest of the full file content
 * [weakHash] SHA-256 hex digest of (first 4 KB ++ last 4 KB); used for the
 *            cheap first-pass duplicate check before computing the full hash.
 */
data class DAppFile(
    val id: String,          // full SHA-256 hex (64 chars)

    var size: Long = 0L,

    var mimeType: String = "",

    var realPath: String = "",

    var refCount: Int = 1,

    var weakHash: String = "",

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) {
    fun getFidUri(): String {
        val fileName = realPath.substringAfterLast('/', "")
        return "fid:$fileName"
    }
}
