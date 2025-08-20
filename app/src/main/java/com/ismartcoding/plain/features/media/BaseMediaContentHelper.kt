package com.ismartcoding.plain.features.media

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.extensions.count
import com.ismartcoding.lib.extensions.forEach
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getPagingCursor
import com.ismartcoding.lib.extensions.getSearchCursor
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.helpers.QueryHelper
import java.io.File

abstract class BaseMediaContentHelper {
    protected abstract val uriExternal: Uri
    protected abstract fun buildBaseWhere(filterFields: List<FilterField>): ContentWhere
    protected abstract fun getProjection(): Array<String>
    protected abstract val mediaType: MediaType

    fun getItemUri(id: String): Uri {
        return Uri.withAppendedPath(uriExternal, id)
    }

    suspend fun countAsync(context: Context, query: String): Int {
        return buildWheres(query).sumOf { where ->
            context.contentResolver.count(uriExternal, where)
        }
    }

    private suspend fun buildWhere(query: String): ContentWhere {
        val fields = QueryHelper.parseAsync(query)
        val where = buildBaseWhere(fields)
        val idsField = fields.find { it.name == "ids" }
        if (idsField != null) {
            where.addIn(BaseColumns._ID, idsField.value.split(","))
        }
        return where
    }

    private suspend fun buildWheres(query: String): List<ContentWhere> {
        val fields = QueryHelper.parseAsync(query)
        if (fields.isNotEmpty()) {
            val wheres = mutableListOf<ContentWhere>()
            val where = buildBaseWhere(fields)
            val idsField = fields.find { it.name == "ids" }
            if (idsField != null) {
                val ids = idsField.value.split(",")
                if (ids.isNotEmpty()) {
                    ids.chunked(2000).forEach { cIds ->
                        val w = where.copy()
                        w.addIn(BaseColumns._ID, cIds)
                        wheres.add(w)
                    }
                } else {
                    wheres.add(where)
                }
            } else {
                wheres.add(where)
            }
            return wheres
        }

        return listOf(ContentWhere()) // query all
    }

    suspend fun getPagingCursorAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: SortBy,
    ): Cursor? {
        return context.contentResolver.getPagingCursor(
            uriExternal,
            getProjection(),
            buildWhere(query),
            limit,
            offset,
            sortBy,
        )
    }

    suspend fun getIdsAsync(
        context: Context,
        query: String,
    ): Set<String> {
        return context.contentResolver.getSearchCursor(
            uriExternal, arrayOf(BaseColumns._ID), buildWhere(query)
        )?.map { cursor, cache ->
            cursor.getStringValue(BaseColumns._ID, cache)
        }?.toSet() ?: emptySet()
    }

    suspend fun getTrashedIdsAsync(
        context: Context,
        query: String,
    ): Set<String> {
        return context.contentResolver.getSearchCursor(
            uriExternal, arrayOf(BaseColumns._ID), buildWhere(query).apply { trash = true }
        )?.map { cursor, cache ->
            cursor.getStringValue(BaseColumns._ID, cache)
        }?.toSet() ?: emptySet()
    }

    protected suspend fun getSearchCursorAsync(
        context: Context,
        query: String,
    ): Cursor? {
        return context.contentResolver.getSearchCursor(
            uriExternal, getProjection(), buildWhere(query)
        )
    }

    fun deleteRecordsAndFilesByIdsAsync(
        context: Context,
        ids: Set<String>,
        trash: Boolean? = null,
    ): Set<String> {
        val paths = mutableSetOf<String>()
        val projection = arrayOf(BaseColumns._ID, MediaStore.MediaColumns.DATA)
        ids.chunked(500).forEach { chunk ->
            val where = ContentWhere()
            where.addIn(BaseColumns._ID, chunk)
            where.trash = trash
            context.contentResolver.getSearchCursor(uriExternal, projection, where)?.forEach { cursor, cache ->
                val id = cursor.getStringValue(BaseColumns._ID, cache)
                val path = cursor.getStringValue(MediaStore.MediaColumns.DATA, cache)
                paths.add(path)
                try {
                    // File.delete can throw a security exception
                    val f = File(path)
                    if (f.deleteRecursively()) {
                        context.contentResolver.delete(
                            getItemUri(id),
                            null,
                            null,
                        )
                    }
                } catch (ex: Exception) {
                    LogCat.e(ex.toString())
                }
            }
        }

        return paths
    }

    fun getPathsByIdsAsync(
        context: Context,
        ids: Set<String>,
    ): Set<String> {
        val paths = mutableSetOf<String>()
        val projection = arrayOf(BaseColumns._ID, MediaStore.MediaColumns.DATA)
        ids.chunked(500).forEach { chunk ->
            val where = ContentWhere()
            where.addIn(BaseColumns._ID, chunk)
            context.contentResolver.getSearchCursor(uriExternal, projection, where)?.forEach { cursor, cache ->
                val path = cursor.getStringValue(MediaStore.MediaColumns.DATA, cache)
                paths.add(path)
            }
        }

        return paths
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun trashByIdsAsync(
        context: Context,
        ids: Set<String>,
    ) {
        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Trash")
            put(MediaStore.MediaColumns.IS_TRASHED, 1)
        }
        ids.forEach { id ->
            try {
                context.contentResolver.update(getItemUri(id), contentValues, null, null)
            } catch (ex: Exception) {
                LogCat.w("Failed to trash media id=$id: ${ex.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun restoreByIdsAsync(
        context: Context,
        ids: Set<String>,
    ) {
        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Trash")
            put(MediaStore.MediaColumns.IS_TRASHED, 0)
        }
        ids.forEach { id ->
            try {
                context.contentResolver.update(getItemUri(id), contentValues, null, null)
            } catch (ex: Exception) {
                LogCat.w("Failed to restore media id=$id: ${ex.message}")
            }
        }
    }

    fun getBucketsAsync(context: Context): List<DMediaBucket> {
        val bucketMap = mutableMapOf<String, DMediaBucket>()

        // Columns to retrieve from the MediaStore query
        val projection =
            arrayOf(
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATA,
            )

        // Querying the MediaStore for images
        context.contentResolver.query(
            uriExternal,
            projection,
            if (mediaType == MediaType.AUDIO) {
                "${MediaStore.Audio.Media.DURATION} > 0 AND ${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} != ''"
            } else {
                "${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} != ''"
            },
            null,
            null,
        )?.forEach { cursor, cache ->
            val bucketId = cursor.getStringValue(MediaStore.MediaColumns.BUCKET_ID, cache)
            val bucketName = cursor.getStringValue(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, cache)
            val size = cursor.getLongValue(MediaStore.MediaColumns.SIZE, cache)
            val path = cursor.getStringValue(MediaStore.MediaColumns.DATA, cache)
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

        return bucketMap.values.sortedBy { Pinyin.toPinyin(it.name).lowercase() }
    }
}
