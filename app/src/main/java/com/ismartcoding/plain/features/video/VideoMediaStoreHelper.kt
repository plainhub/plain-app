package com.ismartcoding.plain.features.video

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
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.features.BaseContentHelper
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.data.TagRelationStub

object VideoMediaStoreHelper : BaseContentHelper() {
    // https://stackoverflow.com/questions/63111091/java-lang-illegalargumentexception-volume-external-primary-not-found-in-android
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    override val idKey: String = MediaStore.Video.Media._ID

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.ORIENTATION,
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
                val createdAt = cursor.getTimeSecondsValue(MediaStore.Video.Media.DATE_ADDED, cache)
                val updatedAt = cursor.getTimeSecondsValue(MediaStore.Video.Media.DATE_MODIFIED, cache)
                val width = cursor.getIntValue(MediaStore.Video.Media.WIDTH, cache)
                val height = cursor.getIntValue(MediaStore.Video.Media.HEIGHT, cache)
                val rotation = cursor.getIntValue(MediaStore.Video.Media.ORIENTATION, cache)
                val path = cursor.getStringValue(MediaStore.Video.Media.DATA, cache)
                val bucketId = cursor.getStringValue(MediaStore.Video.Media.BUCKET_ID, cache)
                result.add(DVideo(id, title, path, duration, size, width, height, rotation, bucketId, createdAt, updatedAt))
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
                MediaStore.Video.Media.SIZE,
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
                val size = c.getLongValue(MediaStore.Video.Media.SIZE, cache)
                val path = c.getStringValue(MediaStore.Video.Media.DATA, cache)
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

        return bucketMap.values.sortedBy { it.name.lowercase() }
    }
}
