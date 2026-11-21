package com.ismartcoding.plain.rust.host.files

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.File as FileEntry
import com.ismartcoding.plain.platform.system.fileIndexCount
import com.ismartcoding.plain.platform.system.fileIndexId
import com.ismartcoding.plain.platform.system.fileIndexRows
import com.ismartcoding.plain.rust.host.*
import com.ismartcoding.plain.rust.host.provider.FileIndexRowsRequest
import com.ismartcoding.plain.rust.host.provider.providerPlan
import kotlinx.serialization.json.*

internal object SystemFilesHost {
    internal suspend fun handle(method: String, params: JsonObject): HostResult = when (method) {
        "systemFileIndexId" -> hostResult(fileIndexId(JsonHelper.jsonDecodeFromElement<PathRequest>(params).path))
        "systemFileIndexCount" -> hostResult(fileIndexCount(params.providerPlan()))
        "systemFileIndexRows" -> {
            val request = JsonHelper.jsonDecodeFromElement<FileIndexRowsRequest>(params)
            hostResult(fileIndexRows(
                request.plan, request.limit, request.offset,
                FileSortBy.valueOf(request.sortBy), request.includeTrashed,
            ).map(::fileFacts))
        }
        else -> error("Unsupported provider operation")
    }

    private fun fileFacts(file: FileEntry): FileFacts = FileFacts(
        name = file.name,
        path = file.path,
        mediaId = file.mediaId.orEmpty(),
        createdAt = file.createdAt?.toEpochMilliseconds(),
        updatedAt = file.updatedAt.toEpochMilliseconds(),
        size = file.size,
        isDir = file.isDir,
        childCount = file.childCount,
    )

}
