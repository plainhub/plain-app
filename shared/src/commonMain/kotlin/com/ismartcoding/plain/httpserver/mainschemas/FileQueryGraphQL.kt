package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.platform.searchFilesInDir
import com.ismartcoding.plain.platform.getRecentFiles
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.helpers.getFileId
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.httpserver.loaders.MountsLoader
import com.ismartcoding.plain.httpserver.models.FavoriteFolder
import com.ismartcoding.plain.httpserver.models.File
import com.ismartcoding.plain.httpserver.models.FileInfo
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.MediaFileInfo
import com.ismartcoding.plain.httpserver.models.StorageMount
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.platform.loadAudioInfo
import com.ismartcoding.plain.platform.loadImageInfo
import com.ismartcoding.plain.platform.loadVideoInfo
import kotlin.reflect.typeOf

@GraphQLQuery
suspend fun mounts(): List<StorageMount> {
    return MountsLoader.load()
}

@GraphQLQuery
suspend fun recentFiles(): List<File> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return getRecentFiles().map { it.toModel() }
}

@GraphQLQuery
suspend fun files(root: String, offset: Int, limit: Int, query: String, sortBy: FileSortBy): List<File> {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    return searchFilesInDir(query, root, sortBy).drop(offset).take(limit).map { it.toModel() }
}

@GraphQLQuery
suspend fun fileInfo(path: String, fileName: String? = null): FileInfo {
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    val finalPath = path.getFinalPath()
    val stat = statFile(finalPath)
    val updatedAt = stat?.updatedAt ?: kotlin.time.Instant.fromEpochMilliseconds(0)
    val size = stat?.size ?: 0L
    val name = fileName ?: finalPath.getFilenameFromPath()
    val data: MediaFileInfo? = when {
        name.isImageFast() -> loadImageInfo(finalPath)
        name.isVideoFast() -> loadVideoInfo(finalPath)
        name.isAudioFast() -> loadAudioInfo(finalPath)
        else -> null
    }
    return FileInfo(path, updatedAt, size, data)
}

@GraphQLQuery
suspend fun fileIds(paths: List<String>): List<String> {
    return paths.map { getFileId(it) }
}

@GraphQLQuery
suspend fun favoriteFolders(): List<FavoriteFolder> {
    return FavoriteFoldersPreference.getValueAsync().map { it.toModel() }
}

fun SchemaBuilder.addFileQuerySchema() {
    type<File> {
        // mediaId is "" for non-media files — expose null instead of an empty sentinel.
        property("mediaId", typeOf<ID?>(), { it: File -> it.mediaId.ifEmpty { null }?.let { id -> ID(id) } })
    }
}
