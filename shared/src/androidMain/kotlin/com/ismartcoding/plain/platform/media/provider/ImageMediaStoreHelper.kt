package com.ismartcoding.plain.platform.media.provider

import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.models.Image as MediaImage
import com.ismartcoding.plain.helpers.ContentWhere
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.plain.lib.extensions.getIntValue
import com.ismartcoding.plain.lib.extensions.getLongValue
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.getTimeSecondsValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.features.media.MediaType
import com.ismartcoding.plain.models.MediaSortBy
import com.ismartcoding.plain.features.files.toSortBy
import kotlin.time.Instant

object ImageMediaStoreHelper : BaseMediaContentHelper() {
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    override val mediaType: MediaType = MediaType.IMAGE

    override fun getProjection(): Array<String> {
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.BUCKET_ID,
        )
        if (Capability.MEDIA_FAVORITE in getDeviceCapabilities()) {
            projection.add(MediaStore.Images.Media.IS_FAVORITE)
        }
        return projection.toTypedArray()
    }

    suspend fun searchAsync(
        context: Context,
        where: ContentWhere,
        limit: Int,
        offset: Int,
        sortBy: MediaSortBy,
    ): List<MediaImage> = withIO {
        getPagingCursorAsync(context, where, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Images.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Images.Media.TITLE, cache)
            val size = cursor.getLongValue(MediaStore.Images.Media.SIZE, cache)
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Images.Media.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Images.Media.DATE_MODIFIED, cache)
            val takenAtMs = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN, cache)
            val takenAt = if (takenAtMs > 0) Instant.fromEpochMilliseconds(takenAtMs) else null
            val width = cursor.getIntValue(MediaStore.Images.Media.WIDTH, cache)
            val height = cursor.getIntValue(MediaStore.Images.Media.HEIGHT, cache)
            val rotation = cursor.getIntValue(MediaStore.Images.Media.ORIENTATION, cache)
            val path = cursor.getStringValue(MediaStore.Images.Media.DATA, cache)
            val bucketId = cursor.getStringValue(MediaStore.Images.Media.BUCKET_ID, cache)
            val isFavorite = if (Capability.MEDIA_FAVORITE in getDeviceCapabilities()) cursor.getIntValue(MediaStore.Images.Media.IS_FAVORITE, cache) == 1 else false
            MediaImage(id = id, title = title, path = path, size = size, width = width, height = height, rotation = rotation, bucketId = bucketId, createdAt = createdAt, updatedAt = updatedAt, takenAt = takenAt, isFavorite = isFavorite)
        } ?: emptyList()
    }

}
