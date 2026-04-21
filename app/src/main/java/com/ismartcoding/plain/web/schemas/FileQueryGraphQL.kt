package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.Permission
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
            MountsLoader.load(MainApp.instance)
        }
    }
    query("recentFiles") {
        resolver { ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            if (isQPlus()) {
                FileMediaStoreHelper.getRecentFilesAsync(context).map { it.toModel() }
            } else {
                FileSystemHelper.getRecentFiles().map { it.toModel() }
            }
        }
    }
    query("files") {
        resolver { root: String, offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            FileSystemHelper.search(query, root, sortBy).drop(offset).take(limit).map { it.toModel() }
        }
    }
    query("fileInfo") {
        resolver { id: ID, path: String, fileName: String ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            val finalPath = path.getFinalPath(context)
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
        resolver { paths: List<String> ->
            paths.map { FileHelper.getFileId(it) }
        }
    }
}
