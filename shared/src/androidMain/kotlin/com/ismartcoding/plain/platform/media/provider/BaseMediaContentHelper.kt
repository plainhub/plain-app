package com.ismartcoding.plain.platform.media.provider

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.features.files.SortBy
import com.ismartcoding.plain.lib.extensions.count
import com.ismartcoding.plain.lib.extensions.getLongValue
import com.ismartcoding.plain.lib.extensions.getPagingCursor
import com.ismartcoding.plain.lib.extensions.getSearchCursor
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.extensions.getTimeSecondsValue
import com.ismartcoding.plain.rust.host.provider.MediaBucketItemFacts
import com.ismartcoding.plain.features.media.MediaType

abstract class BaseMediaContentHelper {
    protected abstract val uriExternal: Uri
    protected abstract fun getProjection(): Array<String>
    protected abstract val mediaType: MediaType

    fun getItemUri(id: String): Uri {
        return Uri.withAppendedPath(uriExternal, id)
    }

    suspend fun countAsync(context: Context, where: ContentWhere): Int = withIO {
        context.contentResolver.count(uriExternal, where)
    }

    suspend fun getPagingCursorAsync(context: Context, where: ContentWhere, limit: Int, offset: Int, sortBy: SortBy): Cursor? = withIO {
        context.contentResolver.getPagingCursor(
            uriExternal,
            getProjection(),
            where,
            limit,
            offset,
            sortBy,
        )
    }

    suspend fun getIdsAsync(context: Context, where: ContentWhere): Set<String> = withIO {
        context.contentResolver.getSearchCursor(
            uriExternal, arrayOf(BaseColumns._ID), where
        )?.map { cursor, cache ->
            cursor.getStringValue(BaseColumns._ID, cache)
        }?.toSet() ?: emptySet()
    }

    fun getBucketItemFactsAsync(context: Context, where: ContentWhere): List<MediaBucketItemFacts> {
        val projection = arrayOf(
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        return context.contentResolver.query(
            uriExternal,
            projection,
            where.toSelection(),
            where.args.toTypedArray(),
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )?.map { cursor, cache ->
            val name = cursor.getStringValue(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, cache)
            MediaBucketItemFacts(
                id = cursor.getStringValue(MediaStore.MediaColumns.BUCKET_ID, cache),
                name = name,
                size = cursor.getLongValue(MediaStore.MediaColumns.SIZE, cache),
                path = cursor.getStringValue(MediaStore.MediaColumns.DATA, cache),
                updatedAt = cursor.getTimeSecondsValue(MediaStore.MediaColumns.DATE_MODIFIED, cache).toString(),
            )
        }.orEmpty()
    }
}
