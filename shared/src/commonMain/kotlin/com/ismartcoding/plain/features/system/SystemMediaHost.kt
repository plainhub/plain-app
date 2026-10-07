package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.enums.has
import kotlinx.serialization.json.*

internal object SystemMediaHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemMediaBucketItemFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.mediaBucketItemFacts(mediaDataType(params)).map { item ->
                MediaBucketItemFacts(
                    id = item.id,
                    name = item.name,
                    size = item.size,
                    path = item.path,
                    sortName = item.sortName,
                )
        })
        "systemMediaRows" -> {
            val items = com.ismartcoding.plain.platform.searchMedia(
                mediaDataType(params),
                params.getValue("query").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                fileSortBy(params),
            )
            JsonHelper.jsonEncodeToElement(items.map { mediaFacts(it) })
        }
        "systemImageRows" -> {
            val items = com.ismartcoding.plain.platform.searchImagesCombined(
                params.getValue("queryText").jsonPrimitive.content,
                params.getValue("extraQuery").jsonPrimitive.content,
                params.getValue("limit").jsonPrimitive.int,
                params.getValue("offset").jsonPrimitive.int,
                fileSortBy(params),
            )
            JsonHelper.jsonEncodeToElement(items.map { mediaFacts(it) })
        }
        "systemMediaCount" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.countMedia(
            mediaDataType(params), params.getValue("query").jsonPrimitive.content))
        "systemImageCount" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.countImagesCombined(
            params.getValue("queryText").jsonPrimitive.content,
            params.getValue("extraQuery").jsonPrimitive.content))
        "systemDocExtGroups" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getDocExtGroups("").map { (ext, count) ->
                DocExtensionFacts(
                    ext = ext,
                    count = count,
                ) })
        "systemMediaAction" -> runMediaAction(params)
        else -> error("Unsupported provider operation")
    }

    private fun mediaFacts(item: com.ismartcoding.plain.db.IData): JsonElement = when (item) {
        is com.ismartcoding.plain.audio.DAudio -> JsonHelper.jsonEncodeToElement(AudioFacts(
            id = item.id,
            title = item.title,
            artist = item.artist,
            path = item.path,
            size = item.size,
            bucketId = item.bucketId,
            durationMs = item.durationMs,
            albumFileId = com.ismartcoding.plain.platform.getAudioAlbumArtFileId(item),
            createdAt = item.createdAt.toString(),
            updatedAt = item.updatedAt.toString(),
            isFavorite = item.isFavorite,
        ))
        is com.ismartcoding.plain.data.DImage -> mediaFacts(
            item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
            durationMs = 0L, takenAt = item.takenAt, isFavorite = item.isFavorite)
        is com.ismartcoding.plain.data.DVideo -> mediaFacts(
            item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
            durationMs = item.durationMs, takenAt = item.takenAt, isFavorite = item.isFavorite)
        is com.ismartcoding.plain.data.DDoc -> mediaFacts(
            item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
            durationMs = item.durationMs, takenAt = null, isFavorite = false)
        else -> error("Unsupported media row ${item::class.simpleName}")
    }

    private fun mediaFacts(
        id: String,
        title: String,
        path: String,
        size: Long,
        bucketId: String,
        createdAt: kotlin.time.Instant,
        updatedAt: kotlin.time.Instant,
        durationMs: Long,
        takenAt: kotlin.time.Instant?,
        isFavorite: Boolean,
    ): JsonElement = JsonHelper.jsonEncodeToElement(MediaFacts(
        id = id,
        title = title,
        path = path,
        size = size,
        bucketId = bucketId,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        durationMs = durationMs,
        takenAt = takenAt?.let { it.toString() },
        isFavorite = isFavorite,
    ))

    /** Each action resolves its own id source: a restore looks in the trash,
     * a trash or a move looks in the live library, and only a delete has to
     * ask whether the trash feature is on at all. */
    private suspend fun runMediaAction(params: JsonObject): JsonElement {
        val action = params.getValue("action").jsonPrimitive.content
        val type = mediaDataType(params)
        val query = params.getValue("query").jsonPrimitive.content
        val destDir = params["destDir"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content ?: ""
        val fromTrash = action == "delete" && com.ismartcoding.plain.enums.AppFeatureType.MEDIA_TRASH.has()
        val ids = when {
            action == "restore" -> com.ismartcoding.plain.platform.getTrashedMediaIds(type, query)
            fromTrash -> com.ismartcoding.plain.platform.getTrashedMediaIds(type, query)
            else -> com.ismartcoding.plain.platform.getMediaIds(type, query)
        }
        return JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.features.mediaactions.MediaActionHelper.run(
            type,
            com.ismartcoding.plain.features.mediaactions.MediaAction.valueOf(action.uppercase()),
            ids,
            fromTrash,
            destDir,
        ))
    }

    private fun fileSortBy(params: JsonObject) =
        com.ismartcoding.plain.features.file.FileSortBy.valueOf(params.getValue("sortBy").jsonPrimitive.content)
}
