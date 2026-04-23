package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.kgraphql.schema.execution.Executor
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.ai.ImageIndexManager
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.preferences.AudioPlaylistPreference
import com.ismartcoding.plain.preferences.VideoPlaylistPreference
import com.ismartcoding.plain.web.loaders.TagsLoader
import com.ismartcoding.plain.web.models.ActionResult
import com.ismartcoding.plain.web.models.Video
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addVideoMediaSchema() {
    query("videos") {
        configure {
            executor = Executor.DataLoaderPrepared
        }
        resolver { offset: Int, limit: Int, query: String, sortBy: FileSortBy ->
            val context = MainApp.instance
            Permission.WRITE_EXTERNAL_STORAGE.checkAsync(context)
            VideoMediaStoreHelper.searchAsync(context, query, limit, offset, sortBy).map { it.toModel() }
        }
        type<Video> {
            dataProperty("tags") {
                prepare { item -> item.id.value }
                loader { ids ->
                    TagsLoader.load(ids, DataType.VIDEO)
                }
            }
        }
    }
    query("videoCount") {
        resolver { query: String ->
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(MainApp.instance)) {
                VideoMediaStoreHelper.countAsync(MainApp.instance, query)
            } else {
                0
            }
        }
    }
    query("mediaBuckets") {
        resolver { type: DataType ->
            val context = MainApp.instance
            if (Permission.WRITE_EXTERNAL_STORAGE.enabledAndCanAsync(context)) {
                if (type == DataType.IMAGE) {
                    ImageMediaStoreHelper.getBucketsAsync(context).map { it.toModel() }
                } else if (type == DataType.AUDIO) {
                    if (isQPlus()) {
                        AudioMediaStoreHelper.getBucketsAsync(context).map { it.toModel() }
                    } else {
                        emptyList()
                    }
                } else if (type == DataType.VIDEO) {
                    VideoMediaStoreHelper.getBucketsAsync(context).map { it.toModel() }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
    mutation("deleteMediaItems") {
        resolver { type: DataType, query: String ->
            val ids: Set<String>
            val context = MainApp.instance
            val hasTrashFeature = AppFeatureType.MEDIA_TRASH.has()
            when (type) {
                DataType.AUDIO -> {
                    ids = if (hasTrashFeature) AudioMediaStoreHelper.getTrashedIdsAsync(context, query) else AudioMediaStoreHelper.getIdsAsync(context, query)
                    AudioMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, true)
                }

                DataType.VIDEO -> {
                    ids = if (hasTrashFeature) VideoMediaStoreHelper.getTrashedIdsAsync(context, query) else VideoMediaStoreHelper.getIdsAsync(context, query)
                    VideoMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, true)
                }

                DataType.IMAGE -> {
                    ids = if (hasTrashFeature) ImageMediaStoreHelper.getTrashedIdsAsync(context, query) else ImageMediaStoreHelper.getIdsAsync(context, query)
                    ImageMediaStoreHelper.deleteRecordsAndFilesByIdsAsync(context, ids, true)
                    ImageIndexManager.enqueueRemove(ids)
                }

                else -> {
                }
            }
            ActionResult(type, query)
        }
    }
    mutation("trashMediaItems") {
        resolver { type: DataType, query: String ->
            if (!isRPlus()) {
                return@resolver ActionResult(type, query)
            }

            var ids = setOf<String>()
            val context = MainApp.instance
            when (type) {
                DataType.AUDIO -> {
                    ids = AudioMediaStoreHelper.getIdsAsync(context, query)
                    val paths = AudioMediaStoreHelper.getPathsByIdsAsync(context, ids)
                    AudioMediaStoreHelper.trashByIdsAsync(context, ids)
                    AudioPlaylistPreference.deleteAsync(context, paths)
                }

                DataType.VIDEO -> {
                    ids = VideoMediaStoreHelper.getIdsAsync(context, query)
                    val paths = VideoMediaStoreHelper.getPathsByIdsAsync(context, ids)
                    VideoMediaStoreHelper.trashByIdsAsync(context, ids)
                    VideoPlaylistPreference.deleteAsync(context, paths)
                }

                DataType.IMAGE -> {
                    ids = ImageMediaStoreHelper.getIdsAsync(context, query)
                    ImageMediaStoreHelper.trashByIdsAsync(context, ids)
                    ImageIndexManager.enqueueRemove(ids)
                }

                else -> {
                }
            }
            TagHelper.deleteTagRelationByKeys(ids, type)
            ActionResult(type, query)
        }
    }
    mutation("restoreMediaItems") {
        resolver { type: DataType, query: String ->
            if (!isRPlus()) {
                return@resolver ActionResult(type, query)
            }

            val ids: Set<String>
            val context = MainApp.instance
            when (type) {
                DataType.AUDIO -> {
                    ids = AudioMediaStoreHelper.getTrashedIdsAsync(context, query)
                    AudioMediaStoreHelper.restoreByIdsAsync(context, ids)
                }

                DataType.VIDEO -> {
                    ids = VideoMediaStoreHelper.getTrashedIdsAsync(context, query)
                    VideoMediaStoreHelper.restoreByIdsAsync(context, ids)
                }

                DataType.IMAGE -> {
                    ids = ImageMediaStoreHelper.getTrashedIdsAsync(context, query)
                    ImageMediaStoreHelper.restoreByIdsAsync(context, ids)
                    ImageIndexManager.enqueueAdd(ids)
                }

                else -> {
                }
            }
            ActionResult(type, query)
        }
    }
}
