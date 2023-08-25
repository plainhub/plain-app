package com.ismartcoding.plain.features.image

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

object ImageHelper : BaseContentHelper() {
    override val uriExternal: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.TITLE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
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

    fun search(context: Context, query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<DImage> {
        val cursor = getSearchCursor(context, query, limit, offset, sortBy.toSortBy())
        val result = mutableListOf<DImage>()
        if (cursor?.moveToFirst() == true) {
            do {
                val id = cursor.getStringValue(MediaStore.Images.Media._ID)
                val title = cursor.getStringValue(MediaStore.Images.Media.TITLE)
                val size = cursor.getLongValue(MediaStore.Images.Media.SIZE)
                val path = cursor.getStringValue(MediaStore.Images.Media.DATA)
                result.add(DImage(id, title, path, size))
            } while (cursor.moveToNext())
        }
        return result
    }

    fun getBuckets(context: Context): List<DMediaBucket> {
        val bucketMap = mutableMapOf<String, DMediaBucket>()

        // Columns to retrieve from the MediaStore query
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA
        )

        // Querying the MediaStore for images
        val cursor = context.contentResolver.query(
            uriExternal,
            projection,
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} != ''",
            null,
            null
        )

        cursor?.use { c ->
            while (c.moveToNext()) {
                val bucketId = c.getStringValue(MediaStore.Images.Media.BUCKET_ID)
                val bucketName = c.getStringValue(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val path = c.getStringValue(MediaStore.Images.Media.DATA)
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