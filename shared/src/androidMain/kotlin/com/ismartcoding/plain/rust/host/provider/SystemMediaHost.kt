package com.ismartcoding.plain.rust.host.provider

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.media.provider.DocMediaStoreHelper
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.Doc
import com.ismartcoding.plain.models.Image
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.platform.media.MediaVolumeRevision
import com.ismartcoding.plain.platform.media.countMedia
import com.ismartcoding.plain.platform.media.getMediaIds
import com.ismartcoding.plain.platform.media.mediaStoreRevision
import com.ismartcoding.plain.platform.media.searchMedia
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.platform.system.mediaBucketItemFacts
import com.ismartcoding.plain.rust.host.*
import com.ismartcoding.plain.platform.media.audioAlbumArtUri
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

internal object SystemMediaHost {
    internal suspend fun handle(method: String, params: JsonObject): HostResult {
        return when (method) {
            "systemMediaBucketItemFacts" -> {
                val request = JsonHelper.jsonDecodeFromElement<MediaPlanRequest>(params)
                hostResult(mediaBucketItemFacts(
                    DataType.valueOf(request.dataType), request.plan))
            }
            "systemMediaRows" -> {
                val request = JsonHelper.jsonDecodeFromElement<MediaRowsRequest>(params)
                if (request.pathsOnly) {
                    require(request.dataType == DataType.DOC.name) { "Path projection requires documents" }
                    return hostResult(DocMediaStoreHelper.pathsAsync(appContext, request.plan.where(), request.limit, request.offset))
                }
                val includeRevision = request.includeRevision == true
                val before = if (includeRevision) mediaStoreRevision() else null
                val items = searchMedia(
                    DataType.valueOf(request.dataType),
                    request.plan, request.limit, request.offset,
                    FileSortBy.valueOf(request.sortBy),
                )
                val rows = items.map { mediaFacts(it) }
                if (includeRevision) hostResult(MediaRowsFacts(rows, before, mediaStoreRevision()))
                else hostResult(rows)
            }
            "systemMediaCount" -> {
                val request = JsonHelper.jsonDecodeFromElement<MediaPlanRequest>(params)
                hostResult(countMedia(
                    DataType.valueOf(request.dataType), request.plan))
            }
            "systemMediaIdsFacts" -> {
                val request = JsonHelper.jsonDecodeFromElement<MediaPlanRequest>(params)
                val type = DataType.valueOf(request.dataType)
                val plan = request.plan
                hostResult(getMediaIds(type, plan))
            }
            else -> error("Unsupported provider operation")
        }
    }

    private fun mediaFacts(item: IData): HostResult = when (item) {
        is Audio -> hostResult(AudioFacts(
            id = item.id,
            title = item.title,
            artist = item.artist,
            path = item.path,
            size = item.size,
            bucketId = item.bucketId,
            durationMs = item.durationMs,
            albumId = item.albumId,
            albumArtUri = audioAlbumArtUri(item.albumId),
            createdAt = item.createdAt.toString(),
            updatedAt = item.updatedAt.toString(),
            isFavorite = item.isFavorite,
        ))
        is Image -> mediaFacts(
            item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
            durationMs = 0L, takenAt = item.takenAt, isFavorite = item.isFavorite, width = item.width, height = item.height, rotation = item.rotation)
        is Video -> mediaFacts(
            item.id, item.title, item.path, item.size, item.bucketId, item.createdAt, item.updatedAt,
            durationMs = item.durationMs, takenAt = item.takenAt, isFavorite = item.isFavorite, width = item.width, height = item.height, rotation = item.rotation)
        is Doc -> mediaFacts(
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
        createdAt: Instant,
        updatedAt: Instant,
        durationMs: Long,
        takenAt: Instant?,
        isFavorite: Boolean,
        width: Int = 0, height: Int = 0, rotation: Int = 0,
    ): HostResult = hostResult(MediaFacts(
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
        width = width, height = height, rotation = rotation,
    ))

}

@Serializable
internal data class MediaRowsFacts(
    val rows: List<HostResult>,
    val revisionBefore: List<MediaVolumeRevision>?,
    val revisionAfter: List<MediaVolumeRevision>?,
)
