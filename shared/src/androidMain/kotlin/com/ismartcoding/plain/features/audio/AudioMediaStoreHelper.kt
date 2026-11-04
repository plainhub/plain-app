package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.models.Audio as AudioTrack
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
import com.ismartcoding.plain.platform.media.provider.BaseMediaContentHelper

object AudioMediaStoreHelper : BaseMediaContentHelper() {
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    override val mediaType: MediaType = MediaType.AUDIO

    override fun getProjection(): Array<String> {
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        if (isQPlus()) {
            projection.add(MediaStore.Audio.Media.BUCKET_ID)
            if (Capability.MEDIA_FAVORITE in getDeviceCapabilities()) {
                projection.add(MediaStore.Audio.Media.IS_FAVORITE)
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
    ): List<AudioTrack> = withIO {
        val audios = mutableListOf<AudioTrack>()
        
        getPagingCursorAsync(context, where, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Audio.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE, cache)
            val artist = cursor.getStringValue(MediaStore.Audio.Media.ARTIST, cache)
            val size = cursor.getLongValue(MediaStore.Audio.Media.SIZE, cache)
            val durationMs = cursor.getLongValue(MediaStore.Audio.Media.DURATION, cache)
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Audio.Media.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Audio.Media.DATE_MODIFIED, cache)
            val path = cursor.getStringValue(MediaStore.Audio.Media.DATA, cache)
            val bucketId = if (isQPlus()) {
                cursor.getStringValue(MediaStore.Audio.Media.BUCKET_ID, cache)
            } else ""
            val albumId = cursor.getStringValue(MediaStore.Audio.Media.ALBUM_ID, cache)
            val isFavorite = if (isQPlus()) cursor.getIntValue(MediaStore.Audio.Media.IS_FAVORITE, cache) == 1 else false

            val audio = AudioTrack(id = id, title = title, artist = artist, path = path, durationMs = durationMs, size = size, bucketId = bucketId, albumId = albumId, createdAt = createdAt, updatedAt = updatedAt, isFavorite = isFavorite)
            audios.add(audio)

        }
        
        
        audios
    }

}
