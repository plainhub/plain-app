package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.appContext

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.checkEnabledAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.web.loaders.FileInfoLoader
import com.ismartcoding.plain.web.loaders.MountsLoader
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.FileInfo
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.MediaFileInfo
import com.ismartcoding.plain.web.models.Tag
import com.ismartcoding.plain.web.models.toModel
import kotlin.time.Instant
import java.io.File

fun SchemaBuilder.addFileQuerySchema() {
    query("mounts") {
        resolver { ->
            MountsLoader.load(appContext)
        }
    }
    query("recentFiles") {
        resolver { ->
            val context = appContext
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            if (isQPlus()) {
                FileMediaStoreHelper.getRecentFilesAsync(context).map { it.toModel() }
            } else {
                FileSystemHelper.getRecentFiles().map { it.toModel() }
            }
        }
    }
    query("files") {
        resolver("root", "offset", "limit", "query", "sortBy") { root: String, offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = appContext
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            FileSystemHelper.search(query, root, sortBy).drop(offset).take(limit).map { it.toModel() }
        }
    }
    query("fileInfo") {
        resolver("id", "path", "fileName") { id: ID, path: String, fileName: String ->
            val context = appContext
            Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
            val finalPath = path.getFinalPath()
            val file = File(finalPath)
            val updatedAt = Instant.fromEpochMilliseconds(file.lastModified())
            var tags = emptyList<Tag>()
            var data: MediaFileInfo? = null
            if (fileName.isImageFast()) {
                if (id.value.isNotEmpty()) {
                    tags = TagsLoader.load(id.value, DataType.IMAGE)
                }
                data = FileInfoLoader.loadImage(finalPath)
            } else if (fileName.isVideoFast()) {
                if (id.value.isNotEmpty()) {
                    tags = TagsLoader.load(id.value, DataType.VIDEO)
                }
                data = FileInfoLoader.loadVideo(context, finalPath)
            } else if (fileName.isAudioFast()) {
                if (id.value.isNotEmpty()) {
                    tags = TagsLoader.load(id.value, DataType.AUDIO)
                }
                data = FileInfoLoader.loadAudio(context, finalPath)
            }
            FileInfo(path, updatedAt, size = file.length(), tags, data)
        }
    }
    query("fileIds") {
        resolver("paths") { paths: List<String> ->
            paths.map { FileHelper.getFileId(it) }
        }
    }
}
