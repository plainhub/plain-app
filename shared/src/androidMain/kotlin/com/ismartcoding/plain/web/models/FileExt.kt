package com.ismartcoding.plain.web.models

import kotlin.time.Instant

fun java.io.File.toModel(): File {
    return File(
        name = name,
        path = absolutePath,
        permission = "rw",
        createdAt = null,
        updatedAt = lastModified().let { Instant.fromEpochMilliseconds(it) },
        size = length(),
        isDir = isDirectory,
        children = if (isDirectory) listFiles()?.size ?: 0 else 0,
        mediaId = ""
    )
}
