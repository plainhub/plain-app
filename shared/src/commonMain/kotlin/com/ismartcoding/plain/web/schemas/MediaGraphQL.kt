package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.platform.Permission
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
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.VideoPlaylistPreference
import com.ismartcoding.plain.web.models.ActionResult
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addMediaSchema() {
    query("mediaBuckets") {
        resolver("type") { type: DataType ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndIsGrantedAsync()) {
                getMediaBuckets(type).map { it.toModel() }
            } else {
                emptyList()
            }
        }
    }
    mutation("deleteMediaItems") {
        resolver("type", "query") { type: DataType, query: String ->
            val hasTrashFeature = AppFeatureType.MEDIA_TRASH.has()
            val ids = if (hasTrashFeature) getTrashedMediaIds(type, query) else getMediaIds(type, query)
            if (type == DataType.IMAGE) {
                enqueueRemoveImageIndex(ids)
            }
            deleteMedia(type, ids, true)
            ActionResult(type, query)
        }
    }
    mutation("trashMediaItems") {
        resolver("type", "query") { type: DataType, query: String ->
            if (!isRPlus()) {
                return@resolver ActionResult(type, query)
            }

            val ids = getMediaIds(type, query)
            when (type) {
                DataType.AUDIO -> {
                    val paths = getMediaPathsByIds(type, ids)
                    trashMedia(type, ids)
                    AudioPlaylistPreference.deleteAsync(paths)
                }

                DataType.VIDEO -> {
                    val paths = getMediaPathsByIds(type, ids)
                    trashMedia(type, ids)
                    VideoPlaylistPreference.deleteAsync(paths)
                }

                DataType.IMAGE -> {
                    trashMedia(type, ids)
                    enqueueRemoveImageIndex(ids)
                }

                DataType.DOC -> {
                    trashMedia(type, ids)
                }

                else -> {}
            }
            TagHelper.deleteTagRelationByKeys(ids, type)
            ActionResult(type, query)
        }
    }
    mutation("restoreMediaItems") {
        resolver("type", "query") { type: DataType, query: String ->
            if (!isRPlus()) {
                return@resolver ActionResult(type, query)
            }

            val ids = getTrashedMediaIds(type, query)
            if (type == DataType.IMAGE) {
                enqueueRemoveImageIndex(ids)
            }
            restoreMedia(type, ids)
            ActionResult(type, query)
        }
    }
}
