package com.ismartcoding.plain.features

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.paging
import com.ismartcoding.lib.extensions.sort
import com.ismartcoding.lib.extensions.where
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.logcat.LogCat
import java.io.File

abstract class BaseContentHelper {
    abstract val uriExternal: Uri
    abstract val idKey: String

    fun getItemUri(id: String): Uri {
        return Uri.withAppendedPath(uriExternal, id)
    }

    abstract fun getWhere(query: String): ContentWhere

    open fun getWheres(query: String): List<ContentWhere> {
        return listOf(getWhere(query))
    }

    abstract fun getProjection(): Array<String>

    open fun count(
        context: Context,
        query: String,
    ): Int {
        return getWheres(query).sumOf { count(context, it) }
    }

    protected open fun getBaseWhere(groups: List<FilterField>): ContentWhere {
        return ContentWhere()
    }

    protected open fun getWhere(
        query: String,
        field: String,
    ): ContentWhere {
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            val where = getBaseWhere(queryGroups)
            val idsGroup = queryGroups.firstOrNull { it.name == "ids" }
            if (idsGroup != null) {
                val ids = idsGroup.value.split(",")
                if (ids.isNotEmpty()) {
                    where.addIn(field, ids)
                }
            }
            return where
        }

        return ContentWhere()
    }

    protected open fun getWheres(
        query: String,
        field: String,
    ): List<ContentWhere> {
        val wheres = mutableListOf<ContentWhere>()
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            val where = getBaseWhere(queryGroups)
            val idsGroup = queryGroups.firstOrNull { it.name == "ids" }
            if (idsGroup != null) {
                val ids = idsGroup.value.split(",")
                if (ids.isNotEmpty()) {
                    ids.chunked(2000).forEach {
                        val w = where.copy()
                        w.addIn(field, it)
                        wheres.add(w)
                    }
                } else {
                    wheres.add(where)
                }
            } else {
                wheres.add(where)
            }
        } else {
            wheres.add(ContentWhere())
        }

        return wheres
    }

    private fun count(
        context: Context,
        where: ContentWhere,
    ): Int {
        var result = 0
        if (isQPlus()) {
            context.contentResolver.query(
                uriExternal,
                null,
                Bundle().apply {
                    where(where.toSelection(), where.args)
                },
                null,
            )?.run {
                moveToFirst()
                result = count
                close()
            }
        } else {
            try {
                context.contentResolver.query(
                    uriExternal,
                    arrayOf("count(*) AS count"),
                    where.toSelection(),
                    where.args.toTypedArray(),
                    null,
                )?.run {
                    moveToFirst()
                    if (count > 0) {
                        result = getInt(0)
                    }
                    close()
                }
            } catch (ex: Exception) {
                // Fatal Exception: java.lang.IllegalArgumentException: Non-token detected in 'count(*) AS count'
                context.contentResolver.query(
                    uriExternal,
                    null,
                    Bundle().apply {
                        where(where.toSelection(), where.args)
                    },
                    null,
                )?.run {
                    moveToFirst()
                    result = count
                    close()
                }
            }
        }

        return result
    }

    fun getSearchCursor(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: SortBy,
    ): Cursor? {
        return if (isRPlus()) {
            // From Android 11, LIMIT and OFFSET should be retrieved using Bundle
            getSearchCursorWithBundle(context, query, limit, offset, sortBy)
        } else {
            try {
                getSearchCursorWithSortOrder(context, query, limit, offset, sortBy)
            } catch (ex: Exception) {
                // Huawei OS android 10 may throw error `Invalid token LIMIT`
                getSearchCursorWithBundle(context, query, limit, offset, sortBy)
            }
        }
    }

    fun getIds(
        context: Context,
        query: String,
    ): Set<String> {
        val cursor = getSearchCursor(context, query)
        val ids = mutableSetOf<String>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
                ids.add(cursor.getStringValue(idKey, cache))
            } while (cursor.moveToNext())
        }

        return ids
    }

    private fun getSearchCursorWithBundle(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: SortBy,
    ): Cursor? {
        return try {
            val where = getWhere(query)
            val sourceUri =
                uriExternal.buildUpon()
                    .appendQueryParameter("limit", limit.toString())
                    .appendQueryParameter("offset", offset.toString())
                    .build()
            context.contentResolver.query(
                sourceUri,
                getProjection(),
                Bundle().apply {
                    paging(offset, limit)
                    sort(sortBy)
                    where(where.toSelection(), where.args)
                },
                null,
            )
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            null
        }
    }

    protected fun getSearchCursorWithSortOrder(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: SortBy?,
    ): Cursor? {
        val where = getWhere(query)
        return context.contentResolver.query(
            uriExternal,
            getProjection(),
            where.toSelection(),
            where.args.toTypedArray(),
            if (sortBy != null) "${sortBy.field} ${sortBy.direction} LIMIT $offset, $limit" else "LIMIT $offset, $limit",
        )
    }

    protected fun getSearchCursor(
        context: Context,
        query: String,
    ): Cursor? {
        val where = getWhere(query)
        return context.contentResolver.query(
            uriExternal,
            getProjection(),
            where.toSelection(),
            where.args.toTypedArray(),
            null,
        )
    }

    open fun deleteByIds(
        context: Context,
        ids: Set<String>,
    ) {
        ids.chunked(500).forEach { chunk ->
            val selection = "${BaseColumns._ID} IN (${StringHelper.getQuestionMarks(chunk.size)})"
            val selectionArgs = chunk.map { it }.toTypedArray()
            context.contentResolver.delete(uriExternal, selection, selectionArgs)
        }
    }

    fun deleteAll(context: Context) {
        context.contentResolver.delete(uriExternal, null, null)
    }

    fun deleteRecordsAndFilesByIds(
        context: Context,
        ids: Set<String>,
    ): Set<String> {
        val paths = mutableSetOf<String>()
        ids.chunked(500).forEach { chunk ->
            val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA)
            val where = ContentWhere()
            where.addIn(MediaStore.MediaColumns._ID, chunk)
            var deletedCount = 0
            val cursor =
                context.contentResolver.query(
                    uriExternal,
                    projection,
                    where.toSelection(),
                    where.args.toTypedArray(),
                    null,
                )
            if (cursor != null) {
                cursor.moveToFirst()
                val cache = mutableMapOf<String, Int>()
                while (!cursor.isAfterLast) {
                    val id = cursor.getStringValue(MediaStore.MediaColumns._ID, cache)
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
                            deletedCount++
                        }
                        cursor.moveToNext()
                    } catch (ex: Exception) {
                        cursor.moveToNext()
                        LogCat.e(ex.toString())
                    }
                }
                cursor.close()
            }
        }

        return paths
    }
}
