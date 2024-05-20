package com.ismartcoding.plain.features

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeMillisecondsValue
import com.ismartcoding.lib.extensions.getTimeSecondsValue
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.data.TagRelationStub

object ImageMediaStoreHelper : BaseContentHelper() {
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    override val idKey: String = MediaStore.Images.Media._ID

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

    override fun getWhere(query: String): ContentWhere {
        return getWhere(query, MediaStore.Images.Media._ID)
    }

    override fun getBaseWhere(groups: List<FilterField>): ContentWhere {
        val where = ContentWhere()
        groups.forEach {
            if (it.name == "text") {
                where.add("${MediaStore.Images.Media.TITLE} LIKE ?", "%${it.value}%")
            } else if (it.name == "bucket_id") {
                where.add("${MediaStore.Images.Media.BUCKET_ID} = ?", it.value)
            }
        }
        return where
    }

    override fun getWheres(query: String): List<ContentWhere> {
        return getWheres(query, MediaStore.Images.Media._ID)
    }

    fun search(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DImage> {
        val cursor = getSearchCursor(context, query, limit, offset, sortBy.toSortBy())
        val result = mutableListOf<DImage>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
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
                result.add(DImage(id, title, path, size, width, height, rotation, bucketId, createdAt, updatedAt))
            } while (cursor.moveToNext())
        }
        return result
    }

    fun getTagRelationStubs(
        context: Context,
        query: String,
    ): List<TagRelationStub> {
        val cursor = getSearchCursor(context, query)
        val result = mutableListOf<TagRelationStub>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
                val id = cursor.getStringValue(MediaStore.Images.Media._ID, cache)
                val title = cursor.getStringValue(MediaStore.Images.Media.TITLE, cache)
                val size = cursor.getLongValue(MediaStore.Images.Media.SIZE, cache)
                result.add(TagRelationStub(id, title, size))
            } while (cursor.moveToNext())
        }
        return result
    }

    fun getBuckets(context: Context): List<DMediaBucket> {
        val bucketMap = mutableMapOf<String, DMediaBucket>()

        // Columns to retrieve from the MediaStore query
        val projection =
            arrayOf(
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA,
            )

        // Querying the MediaStore for images
        val cursor =
            context.contentResolver.query(
                uriExternal,
                projection,
                "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} != ''",
                null,
                null,
            )

        cursor?.use { c ->
            val cache = mutableMapOf<String, Int>()
            while (c.moveToNext()) {
                val bucketId = c.getStringValue(MediaStore.Images.Media.BUCKET_ID, cache)
                val bucketName = c.getStringValue(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, cache)
                val size = c.getLongValue(MediaStore.Images.Media.SIZE, cache)
                val path = c.getStringValue(MediaStore.Images.Media.DATA, cache)
                val bucket = bucketMap[bucketId]
                if (bucket != null) {
                    if (bucket.topItems.size < 4) {
                        bucket.topItems.add(path)
                    }
                    bucket.size += size
                    bucket.itemCount++
                } else {
                    bucketMap[bucketId] = DMediaBucket(bucketId, bucketName, 1, size, mutableListOf(path))
                }
            }
        }

        return bucketMap.values.sortedBy { Pinyin.toPinyin(it.name).lowercase() }
    }
}
