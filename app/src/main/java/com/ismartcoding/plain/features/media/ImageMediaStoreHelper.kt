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
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.features.file.FileSortBy

object ImageMediaStoreHelper : BaseMediaContentHelper() {
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    override val mediaType: MediaType = MediaType.IMAGE

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.BUCKET_ID,
        )
    }

    override fun buildBaseWhere(filterFields: List<FilterField>): ContentWhere {
        val where = ContentWhere()
        filterFields.forEach {
            if (it.name == "text") {
                where.add("${MediaStore.Images.Media.TITLE} LIKE ?", "%${it.value}%")
            } else if (it.name == "bucket_id") {
                where.addEqual(MediaStore.Images.Media.BUCKET_ID, it.value)
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
    ): List<DImage> {
        return getPagingCursorAsync(context, query, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Images.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Images.Media.TITLE, cache)
            val size = cursor.getLongValue(MediaStore.Images.Media.SIZE, cache)
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Images.Media.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Images.Media.DATE_MODIFIED, cache)
            val width = cursor.getIntValue(MediaStore.Images.Media.WIDTH, cache)
            val height = cursor.getIntValue(MediaStore.Images.Media.HEIGHT, cache)
            val rotation = cursor.getIntValue(MediaStore.Images.Media.ORIENTATION, cache)
            val path = cursor.getStringValue(MediaStore.Images.Media.DATA, cache)
            val bucketId = cursor.getStringValue(MediaStore.Images.Media.BUCKET_ID, cache)
            DImage(id, title, path, size, width, height, rotation, bucketId, createdAt, updatedAt)
        } ?: emptyList()
    }

    suspend fun getTagRelationStubsAsync(
        context: Context,
        query: String,
    ): List<TagRelationStub> {
        return getSearchCursorAsync(context, query)?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Images.Media._ID, cache)
            val title = cursor.getStringValue(MediaStore.Images.Media.TITLE, cache)
            val size = cursor.getLongValue(MediaStore.Images.Media.SIZE, cache)
            TagRelationStub(id, title, size)
        } ?: emptyList()
    }
}
