package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.searchFilesInDir
import com.ismartcoding.plain.platform.getRecentFiles
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.platform.getFileId
import com.ismartcoding.plain.web.loaders.MountsLoader
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.File
import com.ismartcoding.plain.web.models.FileInfo
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.MediaFileInfo
import com.ismartcoding.plain.web.models.Tag
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.platform.loadAudioInfo
import com.ismartcoding.plain.platform.loadImageInfo
import com.ismartcoding.plain.platform.loadVideoInfo

fun SchemaBuilder.addFileQuerySchema() {
    query("mounts") {
        resolver { ->
            MountsLoader.load()
        }
    }
    query("recentFiles") {
        resolver { ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            getRecentFiles().map { it.toModel() }
        }
    }
    query("files") {
        resolver("root", "offset", "limit", "query", "sortBy") { root: String, offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            searchFilesInDir(query, root, sortBy).drop(offset).take(limit).map { it.toModel() }
        }
    }
    query("fileInfo") {
        resolver("id", "path", "fileName") { id: ID, path: String, fileName: String ->
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            val finalPath = path.getFinalPath()
            val stat = statFile(finalPath)
            val updatedAt = stat?.updatedAt ?: kotlin.time.Instant.fromEpochMilliseconds(0)
            val size = stat?.size ?: 0L
            var tags = emptyList<Tag>()
            var data: MediaFileInfo? = null
            if (fileName.isImageFast()) {
                if (id.value.isNotEmpty()) {
                    tags = TagsLoader.load(id.value, DataType.IMAGE)
                }
                data = loadImageInfo(finalPath)
            } else if (fileName.isVideoFast()) {
                if (id.value.isNotEmpty()) {
                    tags = TagsLoader.load(id.value, DataType.VIDEO)
                }
                data = loadVideoInfo(finalPath)
            } else if (fileName.isAudioFast()) {
                if (id.value.isNotEmpty()) {
                    tags = TagsLoader.load(id.value, DataType.AUDIO)
                }
                data = loadAudioInfo(finalPath)
            }
            FileInfo(path, updatedAt, size = size, tags, data)
        }
    }
    query("fileIds") {
        resolver("paths") { paths: List<String> ->
            paths.map { getFileId(it) }
        }
    }
}
