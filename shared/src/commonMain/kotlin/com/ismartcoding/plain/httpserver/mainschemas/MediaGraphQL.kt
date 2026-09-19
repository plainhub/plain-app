package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaDataType
import com.ismartcoding.plain.enums.toDataType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.platform.enabledAndIsGrantedAsync
import com.ismartcoding.plain.platform.deleteMedia
import com.ismartcoding.plain.platform.getMediaBuckets
import com.ismartcoding.plain.platform.getMediaIds
import com.ismartcoding.plain.platform.getMediaPathsByIds
import com.ismartcoding.plain.platform.getTrashedMediaIds
import com.ismartcoding.plain.platform.restoreMedia
import com.ismartcoding.plain.platform.trashMedia
import com.ismartcoding.plain.platform.enqueueRemoveImageIndex
import com.ismartcoding.plain.platform.moveMedia
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.preferences.VideoPlaylistPreference
import com.ismartcoding.plain.httpserver.models.MediaActionResult
import com.ismartcoding.plain.httpserver.models.MediaBucket
import com.ismartcoding.plain.httpserver.models.toModel

@GraphQLQuery
suspend fun mediaBuckets(type: MediaDataType): List<MediaBucket> {
    return if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
        getMediaBuckets(type.toDataType()).map { it.toModel() }
    } else {
        emptyList()
    }
}

@GraphQLMutation
suspend fun deleteMediaItems(type: MediaDataType, query: String): MediaActionResult {
    val dataType = type.toDataType()
    val hasTrashFeature = AppFeatureType.MEDIA_TRASH.has()
    val ids = if (hasTrashFeature) getTrashedMediaIds(dataType, query) else getMediaIds(dataType, query)
    if (type == MediaDataType.IMAGE) {
        enqueueRemoveImageIndex(ids)
    }
    deleteMedia(dataType, ids, true)
    return MediaActionResult(type, query, ids.size)
}

@GraphQLMutation
suspend fun trashMediaItems(type: MediaDataType, query: String): MediaActionResult {
    val dataType = type.toDataType()
    if (!isRPlus()) {
        return MediaActionResult(type, query, 0)
    }

    val ids = getMediaIds(dataType, query)
    when (type) {
        MediaDataType.AUDIO -> {
            val paths = getMediaPathsByIds(dataType, ids)
            trashMedia(dataType, ids)
            AudioQueueManager.removePaths(paths)
        }

        MediaDataType.VIDEO -> {
            val paths = getMediaPathsByIds(dataType, ids)
            trashMedia(dataType, ids)
            VideoPlaylistPreference.deleteAsync(paths)
        }

        MediaDataType.IMAGE -> {
            trashMedia(dataType, ids)
            enqueueRemoveImageIndex(ids)
        }

        MediaDataType.DOC -> {
            trashMedia(dataType, ids)
        }
    }
    TagHelper.deleteTagRelationByKeys(ids, dataType)
    return MediaActionResult(type, query, ids.size)
}

@GraphQLMutation
suspend fun restoreMediaItems(type: MediaDataType, query: String): MediaActionResult {
    val dataType = type.toDataType()
    if (!isRPlus()) {
        return MediaActionResult(type, query, 0)
    }

    val ids = getTrashedMediaIds(dataType, query)
    if (type == MediaDataType.IMAGE) {
        enqueueRemoveImageIndex(ids)
    }
    restoreMedia(dataType, ids)
    return MediaActionResult(type, query, ids.size)
}

@GraphQLMutation
suspend fun moveMediaItems(type: MediaDataType, query: String, destDir: String): MediaActionResult {
    val dataType = type.toDataType()
    Permission.WRITE_EXTERNAL_STORAGE.checkEnabledAsync()
    FilePathValidator.requireAllSafe(listOf(destDir))
    val ids = getMediaIds(dataType, query)
    if (ids.isEmpty()) {
        return MediaActionResult(type, query, 0)
    }
    if (type == MediaDataType.IMAGE) {
        enqueueRemoveImageIndex(ids)
    }
    moveMedia(dataType, ids, destDir)
    return MediaActionResult(type, query, ids.size)
}

fun SchemaBuilder.addMediaSchema() {
}
