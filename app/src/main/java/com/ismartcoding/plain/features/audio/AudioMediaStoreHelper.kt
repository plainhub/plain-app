package com.ismartcoding.plain.features.audio

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.features.BaseContentHelper
import com.ismartcoding.plain.features.file.FileSortBy

object AudioMediaStoreHelper : BaseContentHelper() {
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    override val idKey: String = MediaStore.Audio.Media._ID

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.BUCKET_ID,
        )
    }

    override fun getWhere(query: String): ContentWhere {
        return getWhere(query, MediaStore.Audio.Media._ID)
    }

    override fun getBaseWhere(groups: List<FilterField>): ContentWhere {
        val where = ContentWhere()
        where.addGt(MediaStore.Audio.Media.DURATION, "0")
        groups.forEach {
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
            }
        }
        return where
    }

    override fun getWheres(query: String): List<ContentWhere> {
        return getWheres(query, MediaStore.Audio.Media._ID)
    }

    fun search(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DAudio> {
        val cursor = getSearchCursor(context, query, limit, offset, sortBy.toSortBy())
        val result = mutableListOf<DAudio>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
                val id = cursor.getStringValue(MediaStore.Audio.Media._ID, cache)
                val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE, cache)
                val artist = cursor.getStringValue(MediaStore.Audio.Media.ARTIST, cache).replace(MediaStore.UNKNOWN_STRING, "")
                val size = cursor.getLongValue(MediaStore.Audio.Media.SIZE, cache)
                val duration = cursor.getLongValue(MediaStore.Audio.Media.DURATION, cache) / 1000
                val path = cursor.getStringValue(MediaStore.Audio.Media.DATA, cache)
                val bucketId = cursor.getStringValue(MediaStore.Audio.Media.BUCKET_ID, cache)
                result.add(DAudio(id, title, artist, path, duration, size, bucketId))
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
                val id = cursor.getStringValue(MediaStore.Audio.Media._ID, cache)
                val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE, cache)
                val size = cursor.getLongValue(MediaStore.Audio.Media.SIZE, cache)
                result.add(TagRelationStub(id, title, size))
            } while (cursor.moveToNext())
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getBuckets(context: Context): List<DMediaBucket> {
        val bucketMap = mutableMapOf<String, DMediaBucket>()

        val projection =
            arrayOf(
                MediaStore.Audio.Media.BUCKET_ID,
                MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Audio.Media.DATA,
            )

        val cursor =
            context.contentResolver.query(
                uriExternal,
                projection,
                "${MediaStore.Audio.Media.DURATION} > 0 AND ${MediaStore.Audio.Media.BUCKET_DISPLAY_NAME} != ''",
                null,
                null,
            )

        cursor?.use { c ->
            val cache = mutableMapOf<String, Int>()
            while (c.moveToNext()) {
                val bucketId = c.getStringValue(MediaStore.Audio.Media.BUCKET_ID, cache)
                val bucketName = c.getStringValue(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME, cache)
                val path = c.getStringValue(MediaStore.Audio.Media.DATA, cache)
                val bucket = bucketMap[bucketName]
                if (bucket != null) {
                    if (bucket.topItems.size < 4) {
                        bucket.topItems.add(path)
                    }
                    bucket.itemCount++
                } else {
                    bucketMap[bucketName] = DMediaBucket(bucketId, bucketName, 1, mutableListOf(path))
                }
            }
        }

        return bucketMap.values.sortedBy { it.name.lowercase() }
    }
}
