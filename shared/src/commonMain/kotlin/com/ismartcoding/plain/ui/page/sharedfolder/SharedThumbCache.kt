package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.cacheDirPath
import com.ismartcoding.plain.platform.createFileWriteHandle
import com.ismartcoding.plain.platform.fileExists
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Disk cache for remote share-entry thumbnails. The guest `/fs?w=&h=` variant
 * generates server-side thumbnails for images and videos; results land in
 * `cacheDir/shared_thumbs` keyed by share + path + size so list rows can load
 * them as plain local files (Coil cannot fetch the self-signed LAN endpoint).
 */
internal object SharedThumbCache {
    private const val THUMB_SIZE = 200

    private val mutex = Mutex()

    fun isThumbable(entry: SharedFileDto): Boolean {
        if (entry.isDir) return false
        return entry.mimeType.startsWith("image/") || entry.mimeType.startsWith("video/") ||
            entry.name.isImageFast() || entry.name.isVideoFast()
    }

    /** Cached local thumbnail path for [entry]; null when not thumbable or the download failed. */
    suspend fun thumbPath(link: SharedLink, urlToken: String, entry: SharedFileDto): String? {
        if (!isThumbable(entry)) return null
        val path = "${cacheDirPath()}/shared_thumbs/${link.sharedId}_${entry.virtualPath.hashCode()}_${entry.size}"
        if (fileExists(path)) return path
        return mutex.withLock {
            if (fileExists(path)) return@withLock path
            val handle = createFileWriteHandle(path)
            try {
                SharedLinkClient.downloadTo(
                    SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath) + "&w=$THUMB_SIZE&h=$THUMB_SIZE",
                    { buffer, length -> handle.write(buffer, 0, length) },
                )
                handle.close()
                path
            } catch (_: Exception) {
                handle.delete()
                null
            }
        }
    }
}
