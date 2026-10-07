package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemFilesHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "zipItemsFacts" -> {
            val type = params.getValue("type").jsonPrimitive.content
            val items = com.ismartcoding.plain.platform.searchZipItems(
                type,
                params.getValue("query").jsonPrimitive.content,
                params.getValue("id").jsonPrimitive.content,
            ).filter { com.ismartcoding.plain.platform.fileExists(it.sourcePath) }
            JsonHelper.jsonEncodeToElement(ZipItemsFacts(
                items = items.map { entry ->
                    ZipEntryFacts(
                        path = entry.sourcePath,
                        name = entry.entryName,
                    )
                },
            ))
        }
        "scanFilesFacts" -> {
            com.ismartcoding.plain.platform.scanFiles(
                params.getValue("paths").jsonArray.map { it.jsonPrimitive.content }.toTypedArray()
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "fileMetadataFacts" -> {
            val path = params.getValue("path").jsonPrimitive.content
            JsonHelper.jsonEncodeToElement(FileMetadataFacts(
                size = com.ismartcoding.plain.platform.statFile(path)?.size ?: 0,
                mimeType = com.ismartcoding.plain.platform.getContentTypeForPath(path).orEmpty(),
            ))
        }
        "systemMountFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.httpserver.loaders.MountsLoader.load().map { mount ->
            MountFacts(
                id = mount.id.value,
                name = mount.name,
                path = mount.path,
                mountPoint = mount.mountPoint,
                fsType = mount.fsType,
                totalBytes = mount.totalBytes,
                usedBytes = mount.usedBytes,
                freeBytes = mount.freeBytes,
                remote = mount.remote,
                alias = mount.alias,
                driveType = mount.driveType.name,
                diskId = mount.diskId,
            )
        })
        "systemRecentFileFacts" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.getRecentFiles().map(::fileFacts))
        "systemImageFileInfo" -> fileInfoFacts(method, params)
        "systemVideoFileInfo" -> fileInfoFacts(method, params)
        "systemAudioFileInfo" -> fileInfoFacts(method, params)
        "systemDeleteFiles" -> JsonHelper.jsonEncodeToElement(params.getValue("paths").jsonArray.count { path ->
            com.ismartcoding.plain.platform.deleteFileOrDir(path.jsonPrimitive.content)
        })
        "systemCreateDir" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.createDirectory(params.getValue("path").jsonPrimitive.content))
        "systemRenameFile" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.renameAndScanFile(
            params.getValue("path").jsonPrimitive.content,
            params.getValue("name").jsonPrimitive.content) != null)
        "systemWriteTextFile" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.writeFileText(
                params.getValue("path").jsonPrimitive.content,
                params.getValue("content").jsonPrimitive.content,
                params.getValue("overwrite").jsonPrimitive.boolean))
        "systemTransferFile" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.features.file.FileTaskHelper.execute(
            com.ismartcoding.plain.features.file.FileTaskType.valueOf(params.getValue("type").jsonPrimitive.content),
            listOf(com.ismartcoding.plain.features.file.FileTaskOp(
                params.getValue("src").jsonPrimitive.content,
                params.getValue("dst").jsonPrimitive.content,
                params.getValue("overwrite").jsonPrimitive.boolean),
            )).status == com.ismartcoding.plain.features.file.FileTaskStatus.DONE)
        else -> error("Unsupported provider operation")
    }

    /** The `File` contract row. `mediaId` stays an empty string for non-media
     * entries; Rust maps that to `null` rather than carrying the sentinel. */
    private fun fileFacts(file: com.ismartcoding.plain.features.file.DFile): FileFacts = FileFacts(
        name = file.name,
        path = file.path,
        mediaId = file.mediaId,
        createdAt = file.createdAt?.toEpochMilliseconds(),
        updatedAt = file.updatedAt.toEpochMilliseconds(),
        size = file.size,
        isDir = file.isDir,
        childCount = file.childCount,
    )

    private fun locationFacts(location: com.ismartcoding.plain.httpserver.models.Location?): LocationFacts? =
        location?.let {
            LocationFacts(
                latitude = it.latitude,
                longitude = it.longitude,
            )
        }

    private fun fileInfoFacts(method: String, params: JsonObject): JsonElement {
        val path = params.getValue("path").jsonPrimitive.content
        return when (method) {
            "systemImageFileInfo" -> com.ismartcoding.plain.platform.loadImageInfo(path).let {
                JsonHelper.jsonEncodeToElement(ImageInfoFacts(
                    width = it.width,
                    height = it.height,
                    location = locationFacts(it.location),
                ))
            }
            "systemVideoFileInfo" -> com.ismartcoding.plain.platform.loadVideoInfo(path).let {
                JsonHelper.jsonEncodeToElement(VideoInfoFacts(
                    width = it.width,
                    height = it.height,
                    durationMs = it.durationMs,
                    location = locationFacts(it.location),
                ))
            }
            else -> com.ismartcoding.plain.platform.loadAudioInfo(path).let {
                JsonHelper.jsonEncodeToElement(AudioInfoFacts(
                    durationMs = it.durationMs,
                    location = locationFacts(it.location),
                ))
            }
        }
    }
}
