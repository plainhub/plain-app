package com.ismartcoding.plain.features.video

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.BaseContentHelper

object VideoHelper : BaseContentHelper() {
    override val uriExternal: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
        )
    }

    override fun getWhere(query: String): ContentWhere {
        val where = ContentWhere()
        where.add("${MediaStore.Video.Media.DURATION}>0")
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            queryGroups.forEach {
                if (it.name == "text") {
                    where.add("${MediaStore.Video.Media.TITLE} LIKE ?", "%${it.value}%")
                } else if (it.name == "bucket_id") {
                    where.add("${MediaStore.Video.Media.BUCKET_ID} = ?", it.value)
                } else if (it.name == "ids") {
                    val ids = it.value.split(",")
                    if (ids.isNotEmpty()) {
                        where.addIn(MediaStore.Video.Media._ID, ids)
                    }
                }
            }
        }

        return where
    }

    fun search(context: Context, query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<DVideo> {
        val cursor = getSearchCursor(context, query, limit, offset, sortBy.toSortBy())
        val result = mutableListOf<DVideo>()
        if (cursor?.moveToFirst() == true) {
            do {
                val id = cursor.getStringValue(MediaStore.Video.Media._ID)
                val title = cursor.getStringValue(MediaStore.Video.Media.TITLE)
                val size = cursor.getLongValue(MediaStore.Video.Media.SIZE)
                val duration = cursor.getLongValue(MediaStore.Video.Media.DURATION) / 1000
                val path = cursor.getStringValue(MediaStore.Video.Media.DATA)
                result.add(DVideo(id, title, path, duration, size))
            } while (cursor.moveToNext())
        }
        return result
    }

    fun getBuckets(context: Context): List<DMediaBucket> {
        val bucketMap = mutableMapOf<String, DMediaBucket>()

        // Columns to retrieve from the MediaStore query
        val projection = arrayOf(
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA
        )

        val cursor = context.contentResolver.query(
            uriExternal,
            projection,
            "${MediaStore.Video.Media.BUCKET_DISPLAY_NAME} != ''",
            null,
            null
        )

        cursor?.use { c ->
            while (c.moveToNext()) {
                val bucketId = c.getStringValue(MediaStore.Video.Media.BUCKET_ID)
                val bucketName = c.getStringValue(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val path = c.getStringValue(MediaStore.Video.Media.DATA)
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