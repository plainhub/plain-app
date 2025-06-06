package com.ismartcoding.plain.features.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeSecondsValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.features.file.FileSortBy

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
            projection.add(
                MediaStore.Audio.Media.BUCKET_ID,
            )
        }

        return projection.toTypedArray()
    }

    override fun buildBaseWhere(filterFields: List<FilterField>): ContentWhere {
        val where = ContentWhere()
        where.addGt(MediaStore.Audio.Media.DURATION, "0")
        filterFields.forEach {
            when (it.name) {
                "text" -> {
                    where.addLikes(
                        arrayListOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST),
                        arrayListOf(it.value, it.value),
                    )
                }

                "name" -> {
                    where.addEqual(MediaStore.Audio.Media.TITLE, it.value)
                }

                "bucket_id" -> {
                    where.addEqual(MediaStore.Audio.Media.BUCKET_ID, it.value)
                }

                "artist" -> {
                    where.addEqual(MediaStore.Audio.Media.ARTIST, it.value)
                }

                "trash" -> {
                    where.trash = it.value.toBooleanStrictOrNull()
                }
            }
        }
        return where
    }

    suspend fun searchAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DAudio> {
        return getPagingCursorAsync(context, query, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Audio.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE, cache)
            val artist = cursor.getStringValue(MediaStore.Audio.Media.ARTIST, cache).replace(MediaStore.UNKNOWN_STRING, "")
            val size = cursor.getLongValue(MediaStore.Audio.Media.SIZE, cache)
            val duration = cursor.getLongValue(MediaStore.Audio.Media.DURATION, cache) / 1000
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Audio.Media.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Audio.Media.DATE_MODIFIED, cache)
            val path = cursor.getStringValue(MediaStore.Audio.Media.DATA, cache)
            val bucketId = if (isQPlus()) {
                cursor.getStringValue(MediaStore.Audio.Media.BUCKET_ID, cache)
            } else ""
            val albumId = cursor.getStringValue(MediaStore.Audio.Media.ALBUM_ID, cache)
            DAudio(id, title, artist, path, duration, size, bucketId, albumId, createdAt, updatedAt)
        } ?: emptyList()
    }

    suspend fun getTagRelationStubsAsync(
        context: Context,
        query: String,
    ): List<TagRelationStub> {
        return getSearchCursorAsync(context, query)?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Audio.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE, cache)
            val size = cursor.getLongValue(MediaStore.Audio.Media.SIZE, cache)
            TagRelationStub(id, title, size)
        } ?: emptyList()
    }
}
