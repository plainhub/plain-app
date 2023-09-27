package com.ismartcoding.plain.features.video

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.features.BaseContentHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.tag.TagRelationStub

object VideoHelper : BaseContentHelper() {
    override val uriExternal: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    override val idKey: String = MediaStore.Video.Media._ID

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_ID,
        )
    }

    override fun getWhere(query: String): ContentWhere {
        return getWhere(query, MediaStore.Video.Media._ID)
    }

    override fun getBaseWhere(groups: List<FilterField>): ContentWhere {
        val where = ContentWhere()
        // where.add("${MediaStore.Video.Media.DURATION}>0")
        groups.forEach {
            if (it.name == "text") {
                where.add("${MediaStore.Video.Media.TITLE} LIKE ?", "%${it.value}%")
            } else if (it.name == "bucket_id") {
                where.add("${MediaStore.Video.Media.BUCKET_ID} = ?", it.value)
            }
        }
        return where
    }

    override fun getWheres(query: String): List<ContentWhere> {
        return getWheres(query, MediaStore.Video.Media._ID)
    }

    fun search(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DVideo> {
        val cursor = getSearchCursor(context, query, limit, offset, sortBy.toSortBy())
        val result = mutableListOf<DVideo>()
        cursor?.use { c ->
            val cache = mutableMapOf<String, Int>()
            while (c.moveToNext()) {
                val id = cursor.getStringValue(MediaStore.Video.Media._ID, cache)
                val title = cursor.getStringValue(MediaStore.Video.Media.TITLE, cache)
                val size = cursor.getLongValue(MediaStore.Video.Media.SIZE, cache)
                val duration = cursor.getLongValue(MediaStore.Video.Media.DURATION, cache) / 1000
                val path = cursor.getStringValue(MediaStore.Video.Media.DATA, cache)
                val bucketId = cursor.getStringValue(MediaStore.Video.Media.BUCKET_ID, cache)
                result.add(DVideo(id, title, path, duration, size, bucketId))
            }
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
                val id = cursor.getStringValue(MediaStore.Video.Media._ID, cache)
                val title = cursor.getStringValue(MediaStore.Video.Media.TITLE, cache)
                val size = cursor.getLongValue(MediaStore.Video.Media.SIZE, cache)
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
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
            )

        val cursor =
            context.contentResolver.query(
                uriExternal,
                projection,
                "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} != ''",
                null,
                null,
            )

        cursor?.use { c ->
            val cache = mutableMapOf<String, Int>()
            while (c.moveToNext()) {
                val bucketId = c.getStringValue(MediaStore.Video.Media.BUCKET_ID, cache)
                val bucketName = c.getStringValue(MediaStore.Video.Media.BUCKET_DISPLAY_NAME, cache)
                val path = c.getStringValue(MediaStore.Video.Media.DATA, cache)
                val bucket = bucketMap[bucketId]
                if (bucket != null) {
                    if (bucket.topItems.size < 4) {
                        bucket.topItems.add(path)
                    }
                    bucket.itemCount++
                } else {
                    bucketMap[bucketId] = DMediaBucket(bucketId, bucketName, 1, mutableListOf(path))
                }
            }
        }

        return bucketMap.values.sortedBy { it.name.lowercase() }
    }
}
