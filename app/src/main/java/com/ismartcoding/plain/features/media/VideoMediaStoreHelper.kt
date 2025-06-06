package com.ismartcoding.plain.features.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeSecondsValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.features.file.FileSortBy

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
        )
        if (isQPlus()) {
            projection.add(MediaStore.Video.Media.ORIENTATION)
        }

        return projection.toTypedArray()
    }

    override fun buildBaseWhere(filterFields: List<FilterField>): ContentWhere {
        val where = ContentWhere()
        // where.add("${MediaStore.Video.Media.DURATION}>0")
        filterFields.forEach {
            if (it.name == "text") {
                where.add("${MediaStore.Video.Media.TITLE} LIKE ?", "%${it.value}%")
            } else if (it.name == "bucket_id") {
                where.addEqual(MediaStore.Video.Media.BUCKET_ID, it.value)
            } else if (it.name == "trash") {
                where.trash = it.value.toBooleanStrictOrNull()
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
    ): List<DVideo> {
        return getPagingCursorAsync(context, query, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Video.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Video.Media.TITLE, cache)
            val size = cursor.getLongValue(MediaStore.Video.Media.SIZE, cache)
            val duration = cursor.getLongValue(MediaStore.Video.Media.DURATION, cache) / 1000
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Video.Media.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Video.Media.DATE_MODIFIED, cache)
            val width = cursor.getIntValue(MediaStore.Video.Media.WIDTH, cache)
            val height = cursor.getIntValue(MediaStore.Video.Media.HEIGHT, cache)
            val rotation = if (isQPlus()) cursor.getIntValue(MediaStore.Video.Media.ORIENTATION, cache) else 0
            val path = cursor.getStringValue(MediaStore.Video.Media.DATA, cache)
            val bucketId = cursor.getStringValue(MediaStore.Video.Media.BUCKET_ID, cache)
            DVideo(id, title, path, duration, size, width, height, rotation, bucketId, createdAt, updatedAt)
        } ?: emptyList()
    }

    suspend fun getTagRelationStubsAsync(
        context: Context,
        query: String,
    ): List<TagRelationStub> {
        return getSearchCursorAsync(context, query)?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Video.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Video.Media.TITLE, cache)
            val size = cursor.getLongValue(MediaStore.Video.Media.SIZE, cache)
            TagRelationStub(id, title, size)
        } ?: emptyList()
    }
}
