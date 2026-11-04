package com.ismartcoding.plain.platform.media.provider


import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.models.Video as MediaVideo
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
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.files.toSortBy
import kotlin.time.Instant

object VideoMediaStoreHelper : BaseMediaContentHelper() {
    // https://stackoverflow.com/questions/63111091/java-lang-illegalargumentexception-volume-external-primary-not-found-in-android
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    override val mediaType: MediaType = MediaType.VIDEO

    override fun getProjection(): Array<String> {
        val projection = mutableListOf(
           MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DATE_TAKEN,
        )
        if (isQPlus()) {
            projection.add(MediaStore.Video.Media.ORIENTATION)
            if (Capability.MEDIA_FAVORITE in getDeviceCapabilities()) {
                projection.add(MediaStore.Video.Media.IS_FAVORITE)
            }
        }

        return projection.toTypedArray()
    }

    suspend fun searchAsync(
        context: Context,
        where: ContentWhere,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<MediaVideo> = withIO {
        val videos = mutableListOf<MediaVideo>()
        
        getPagingCursorAsync(context, where, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Video.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Video.Media.TITLE, cache)
            val size = cursor.getLongValue(MediaStore.Video.Media.SIZE, cache)
            val durationMs = cursor.getLongValue(MediaStore.Video.Media.DURATION, cache)
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Video.Media.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Video.Media.DATE_MODIFIED, cache)
            val width = cursor.getIntValue(MediaStore.Video.Media.WIDTH, cache)
            val height = cursor.getIntValue(MediaStore.Video.Media.HEIGHT, cache)
            val rotation = if (isQPlus()) cursor.getIntValue(MediaStore.Video.Media.ORIENTATION, cache) else 0
            val path = cursor.getStringValue(MediaStore.Video.Media.DATA, cache)
            val bucketId = cursor.getStringValue(MediaStore.Video.Media.BUCKET_ID, cache)
            val dateTakenMs = cursor.getLongValue(MediaStore.Video.Media.DATE_TAKEN, cache)
            val takenAt = if (dateTakenMs > 0) Instant.fromEpochMilliseconds(dateTakenMs) else null
            val isFavorite = if (Capability.MEDIA_FAVORITE in getDeviceCapabilities()) cursor.getIntValue(MediaStore.Video.Media.IS_FAVORITE, cache) == 1 else false

            val video = MediaVideo(id = id, title = title, path = path, durationMs = durationMs, size = size, width = width, height = height, rotation = rotation, bucketId = bucketId, createdAt = createdAt, updatedAt = updatedAt, takenAt = takenAt, isFavorite = isFavorite)
            videos.add(video)

        }
        
        
        videos
    }

}
