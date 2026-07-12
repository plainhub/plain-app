package com.ismartcoding.plain.thumbnail

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Disk cache for generated thumbnails.
 *
 * Cache key = SHA-256(absolutePath + ":" + lastModifiedMs + ":" + w + "x" + h + ":" + cc + ":" + CACHE_VERSION)
 * Cache location = [Context.cacheDir]/thumbs/
 *
 * Key components:
 *  - File path + last-modified timestamp: invalidates cache when source file changes.
 *  - Requested dimensions + centerCrop flag: separate entries per size/mode.
 *  - CACHE_VERSION: bump this constant whenever the thumbnail generation algorithm
 *    changes (e.g., EXIF rotation fix) to force regeneration of all cached thumbnails.
 *    Old cache files are silently ignored (keys no longer match); Android's OS-level
 *    cache trimming evicts them when storage is low.
 *
 * Eviction: Android's OS-level cache trimming handles total cache size automatically.
 * No custom LRU is needed — the OS trims cacheDir when storage is low.
 */
object ThumbnailCache {

    /**
     * Increment this whenever the decode/transform algorithm changes so that stale
     * cached thumbnails are automatically invalidated on the next request.
     * History:
     *  v1 — initial implementation
     *  v2 — added full EXIF orientation correction (all 8 orientations)
     */
    private const val CACHE_VERSION = "v2"

    private fun cacheDir(context: Context): File =
        File(context.cacheDir, "thumbs").also { it.mkdirs() }

    private fun cacheKey(path: String, lastModifiedMs: Long, w: Int, h: Int, centerCrop: Boolean): String {
        val raw = "$path:$lastModifiedMs:${w}x$h:$centerCrop:$CACHE_VERSION"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Return cached JPEG bytes if the cache entry is still valid (source file unchanged),
     * or null on cache miss.
     */
    fun get(context: Context, path: String, w: Int, h: Int, centerCrop: Boolean): ByteArray? {
        val key = cacheKey(path, File(path).lastModified(), w, h, centerCrop)
        val cached = File(cacheDir(context), "$key.jpg")
        return if (cached.exists()) cached.readBytes() else null
    }

    /** Store thumbnail JPEG bytes in the disk cache. */
    fun put(context: Context, path: String, w: Int, h: Int, centerCrop: Boolean, bytes: ByteArray) {
        val key = cacheKey(path, File(path).lastModified(), w, h, centerCrop)
        File(cacheDir(context), "$key.jpg").writeBytes(bytes)
    }
}
