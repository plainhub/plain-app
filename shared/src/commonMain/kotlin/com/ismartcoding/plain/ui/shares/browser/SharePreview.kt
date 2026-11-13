package com.ismartcoding.plain.ui.shares.browser

import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.platform.system.cacheDirPath

internal object SharePreview {
    suspend fun copy(entry: File, thumbnail: Boolean = false, directory: String = cacheDirPath(), files: com.ismartcoding.plain.features.files.FilesApi = ContentSource.Local.files): String {
        val url = if (thumbnail) entry.url.replace("&action=download", "&action=preview&size=sm&v=${entry.updatedAt.toEpochMilliseconds()}") else entry.url
        return files.copyToDirectory(url, directory, if (thumbnail) "thumbnail" else entry.name).also { check(it.isNotEmpty()) { "Share preview unavailable" } }
    }
}
