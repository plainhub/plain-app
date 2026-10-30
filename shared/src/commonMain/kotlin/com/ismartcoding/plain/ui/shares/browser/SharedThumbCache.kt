package com.ismartcoding.plain.ui.shares.browser

import com.ismartcoding.plain.features.shares.SharedFileDto
import com.ismartcoding.plain.features.shares.SharedLink
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import kotlinx.coroutines.CancellationException

internal object SharedThumbCache {
    fun isThumbable(entry: SharedFileDto): Boolean = !entry.isDir && (
        entry.mimeType.startsWith("image/") || entry.mimeType.startsWith("video/") ||
            entry.name.isImageFast() || entry.name.isVideoFast())

    suspend fun thumbModel(source: ContentSource, link: SharedLink, urlToken: String, entry: SharedFileDto): Any? {
        if (!isThumbable(entry)) return null
        return try {
            val path = source.sharedLinks.cacheFile(link, urlToken, entry, thumbnail = true)
            if (source is ContentSource.Remote) source.client.resource(path) else path
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }
}
