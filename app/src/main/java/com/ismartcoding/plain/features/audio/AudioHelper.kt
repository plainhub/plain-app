package com.ismartcoding.plain.features.audio

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.BaseContentHelper

object AudioHelper : BaseContentHelper() {
    override val uriExternal: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
    }

    override fun getWhere(query: String): ContentWhere {
        val where = ContentWhere()
        where.add("${MediaStore.Audio.Media.DURATION}>0")
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            queryGroups.forEach {
                if (it.name == "text") {
                    where.addLikes(arrayListOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST), arrayListOf(it.value, it.value))
                } else if (it.name == "name") {
                    where.addEqual(MediaStore.Audio.Media.TITLE, it.value)
                } else if (it.name == "bucket_id") {
                    where.add("${MediaStore.Audio.Media.BUCKET_ID} = ?", it.value)
                } else if (it.name == "artist") {
                    where.addEqual(MediaStore.Audio.Media.ARTIST, it.value)
                } else if (it.name == "ids") {
                    val ids = it.value.split(",")
                    if (ids.isNotEmpty()) {
                        where.addIn(MediaStore.Audio.Media._ID, ids)
                    }
                }
            }
        }
        return where
    }

    fun search(context: Context, query: String, limit: Int, offset: Int, sortBy: FileSortBy): List<DAudio> {
        val cursor = getSearchCursor(context, query, limit, offset, sortBy.toSortBy())
        val result = mutableListOf<DAudio>()
        if (cursor?.moveToFirst() == true) {
            do {
                val id = cursor.getStringValue(MediaStore.Audio.Media._ID)
                val title = cursor.getStringValue(MediaStore.Audio.Media.TITLE)
                val artist = cursor.getStringValue(MediaStore.Audio.Media.ARTIST).replace(MediaStore.UNKNOWN_STRING, "")
                val size = cursor.getLongValue(MediaStore.Audio.Media.SIZE)
                val duration = cursor.getLongValue(MediaStore.Audio.Media.DURATION) / 1000
                val path = cursor.getStringValue(MediaStore.Audio.Media.DATA)
                result.add(DAudio(id, title, artist, path, duration, size))
            } while (cursor.moveToNext())
        }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getBuckets(context: Context): List<DMediaBucket> {
        val bucketMap = mutableMapOf<String, DMediaBucket>()

        val projection = arrayOf(
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )

        val cursor = context.contentResolver.query(
            uriExternal,
            projection,
            "${MediaStore.Audio.Media.BUCKET_DISPLAY_NAME} != ''",
            null,
            null
        )

        cursor?.use { c ->
            while (c.moveToNext()) {
                val bucketId = c.getStringValue(MediaStore.Audio.Media.BUCKET_ID)
                val bucketName = c.getStringValue(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME)
                val path = c.getStringValue(MediaStore.Audio.Media.DATA)
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