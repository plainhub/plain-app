package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.models.File as FileEntry
import com.ismartcoding.plain.models.Package as AppPackage

internal fun managedTitle(item: IData): String = when (item) {
    is FileEntry -> item.name
    is Note -> item.title.ifEmpty { item.getSummary().lineSequence().firstOrNull().orEmpty() }
    is MediaItem -> item.title.ifEmpty { item.path.substringAfterLast('/') }
    is Feed -> item.name.ifEmpty { item.url }
    is FeedEntry -> item.title
    is AppPackage -> item.name
    else -> error("Unsupported managed content")
}

internal fun managedDescription(item: IData): String = when (item) {
    is FileEntry -> if (item.isDir) item.path else item.size.formatBytes()
    is Note -> item.getSummary()
    is MediaItem -> item.size.formatBytes()
    is Feed -> item.url
    is FeedEntry -> item.getSummary()
    is AppPackage -> item.version
    else -> error("Unsupported managed content")
}

internal fun managedPath(item: IData): String = when (item) {
    is FileEntry -> item.path
    is MediaItem -> item.path
    is AppPackage -> item.path
    else -> error("Managed content has no file path")
}

internal fun managedFileName(item: IData): String = managedPath(item).substringAfterLast('/')
