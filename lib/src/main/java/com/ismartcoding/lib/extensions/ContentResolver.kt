package com.ismartcoding.lib.extensions

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.annotation.CheckResult
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.OpenableFile
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.lib.logcat.LogCat

fun ContentResolver.getSystemScreenTimeout(): Int {
    return try {
        Settings.System.getInt(this, Settings.System.SCREEN_OFF_TIMEOUT)
    } catch (e: Settings.SettingNotFoundException) {
        LogCat.e("Error getting screen timeout", e)
        5000 * 60 // default 5 minutes
    }
}

@CheckResult
fun ContentResolver.setSystemScreenTimeout(timeout: Int): Boolean {
    return try {
        Settings.System.putInt(this, Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        true
    } catch (e: SecurityException) {
        LogCat.e("Error writing screen timeout", e)
        false
    }
}

fun ContentResolver.count(uri: Uri, where: ContentWhere): Int {
    return if (isQPlus()) {
        countWithBundle(uri, where)
    } else {
        try {
            countWithSql(uri, where)
        } catch (ex: Exception) {
            // Fatal Exception: java.lang.IllegalArgumentException: Non-token detected in 'count(*) AS count'
            countWithBundle(uri, where)
        }
    }
}

fun ContentResolver.countWithSql(uri: Uri, where: ContentWhere): Int {
    var result = 0
    query(
        uri,
        arrayOf("count(*) AS count"),
        where.toSelection(),
        where.args.toTypedArray(),
        null,
    )?.use {
        it.moveToFirst()
        if (it.count > 0) {
            result = it.getInt(0)
        }
    }

    return result
}

fun ContentResolver.countWithBundle(uri: Uri, where: ContentWhere): Int {
    var result = 0
    query(
        uri,
        null,
        Bundle().apply {
            where(where.toSelection(), where.args)
            if (where.trash == true) {
                if (isRPlus()) {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                }
            }
        },
        null,
    )?.use {
        it.moveToFirst()
        result = it.count
    }

    return result
}

fun ContentResolver.getPagingCursor(
    uri: Uri,
    projection: Array<String>,
    where: ContentWhere,
    limit: Int,
    offset: Int,
    sortBy: SortBy,
): Cursor? {
    return if (isRPlus()) {
        // From Android 11, LIMIT and OFFSET should be retrieved using Bundle
        getPagingCursorWithBundle(uri, projection, where, limit, offset, sortBy)
    } else {
        try {
            getPagingCursorWithSql(uri, projection, where, limit, offset, sortBy)
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            // Huawei OS android 10 may throw error `Invalid token LIMIT`
            getPagingCursorWithBundle(uri, projection, where, limit, offset, sortBy)
        }
    }
}

fun ContentResolver.getPagingCursorWithBundle(
    uri: Uri,
    projection: Array<String>,
    where: ContentWhere,
    limit: Int,
    offset: Int,
    sortBy: SortBy,
): Cursor? {
    return try {
        query(
            uri,
            projection,
            Bundle().apply {
                paging(offset, limit)
                sort(sortBy)
                where(where.toSelection(), where.args)
                if (where.trash == true) {
                    if (isRPlus()) {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    }
                }
            },
            null,
        )
    } catch (ex: Exception) {
        LogCat.e(ex.toString())
        null
    }
}

fun ContentResolver.getPagingCursorWithSql(
    uri: Uri,
    projection: Array<String>,
    where: ContentWhere,
    limit: Int,
    offset: Int,
    sortBy: SortBy?,
): Cursor? {
    return query(
        uri,
        projection,
        where.toSelection(),
        where.args.toTypedArray(),
        if (sortBy != null) "${sortBy.field} ${sortBy.direction} LIMIT $offset, $limit" else "LIMIT $offset, $limit",
    )
}

fun ContentResolver.getSearchCursorWithSql(
    uri: Uri,
    projection: Array<String>,
    where: ContentWhere,
): Cursor? {
    return query(
        uri,
        projection,
        where.toSelection(),
        where.args.toTypedArray(),
        null,
    )
}

fun ContentResolver.getSearchCursorWithBundle(
    uri: Uri,
    projection: Array<String>,
    where: ContentWhere,
): Cursor? {
    return try {
        query(
            uri,
            projection,
            Bundle().apply {
                where(where.toSelection(), where.args)
                if (where.trash == true) {
                    if (isRPlus()) {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    }
                }
            },
            null,
        )
    } catch (ex: Exception) {
        LogCat.e(ex.toString())
        null
    }
}

fun ContentResolver.getSearchCursor(
    uri: Uri,
    projection: Array<String>,
    where: ContentWhere,
): Cursor? {
    return if (isRPlus()) {
        getSearchCursorWithBundle(uri, projection, where)
    } else {
        try {
            getSearchCursorWithSql(uri, projection, where)
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            getSearchCursorWithBundle(uri, projection, where)
        }
    }
}

fun ContentResolver.queryCursor(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
): Cursor? {
    return query(uri, projection, selection, selectionArgs, sortOrder)
}

fun ContentResolver.getMediaContentUri(path: String): Uri? {
    val baseUri = path.pathToMediaStoreBaseUri()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = MediaStore.Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    return query(baseUri, projection, selection, selectionArgs, null)?.find { cursor, cache ->
        val id = cursor.getStringValue(MediaStore.Images.Media._ID, cache)
        Uri.withAppendedPath(baseUri, id)
    }
}

fun ContentResolver.queryOpenableFileName(uri: Uri): String {
    return queryCursor(uri)?.find { cursor, cache ->
        cursor.getStringValue(OpenableColumns.DISPLAY_NAME, cache)
    } ?: ""
}

fun ContentResolver.queryOpenableFile(uri: Uri): OpenableFile? {
    return queryCursor(uri)?.find { cursor, cache ->
        OpenableFile(
            cursor.getStringValue(OpenableColumns.DISPLAY_NAME, cache),
            cursor.getLongValue(OpenableColumns.SIZE, cache)
        )
    }
}