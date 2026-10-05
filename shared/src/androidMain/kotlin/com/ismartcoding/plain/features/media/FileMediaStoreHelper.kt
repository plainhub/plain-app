package com.ismartcoding.plain.features.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.lib.extensions.find
import com.ismartcoding.plain.lib.extensions.forEach
import com.ismartcoding.plain.lib.extensions.getPagingCursor
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.extensions.queryCursor
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.extensions.toFile
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.toFileSortBy
import com.ismartcoding.plain.features.system.RustSystemProviders
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.helpers.QueryHelper

object FileMediaStoreHelper : BaseContentHelper() {
    override val uriExternal: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    override fun getProjection(): Array<String> {
        return arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
    }

    override suspend fun buildWhereAsync(query: String): ContentWhere {
        val parentPath = QueryHelper.parseAsync(query).firstOrNull { it.name == "parent" }?.value
        val parentId = parentPath?.let { getIdByPathAsync(appContext, it) ?: "-1" }
        return RustSystemProviders.providerWhere(DataType.FILE, query, parentId)
    }

    suspend fun searchAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DFile> = withIO {
        val items = context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), buildWhereAsync(query),
            limit, offset, sortBy.toFileSortBy()
        )?.map { cursor, cache ->
            cursor.toFile(cache)
        } ?: emptyList()
        val folderIds = items.filter { it.isDir }.map { it.mediaId }
        val counts = getChildrenCountAsync(context, folderIds)
        return@withIO items.map {
            it.copy(childCount = counts[it.mediaId] ?: 0)
        }
    }

    fun getByIdAsync(context: Context, id: String): DFile? {
        return context.contentResolver
            .queryCursor(uriExternal, getProjection(), "${MediaStore.Files.FileColumns._ID} = ?", arrayOf(id))?.find { cursor, cache ->
                cursor.toFile(cache)
            }
    }

    private fun getChildrenCountAsync(context: Context, folderIds: List<String>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        if (folderIds.isEmpty()) {
            return counts
        }
        val where = ContentWhere()
        where.addIn(MediaStore.Files.FileColumns.PARENT, folderIds)
        context.contentResolver
            .queryCursor(uriExternal, arrayOf(MediaStore.Files.FileColumns.PARENT), where.toSelection(), where.args.toTypedArray())?.forEach { cursor, cache ->
                val parentId = cursor.getStringValue(MediaStore.Files.FileColumns.PARENT, cache)
                counts[parentId] = counts.getOrDefault(parentId, 0) + 1
            }

        return counts
    }

    private fun getIdByPathAsync(context: Context, path: String): String? {
        return context.contentResolver
            .queryCursor(uriExternal, arrayOf(MediaStore.Files.FileColumns._ID), "${MediaStore.Files.FileColumns.DATA} = ?", arrayOf(path))?.find { cursor, cache ->
                cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)
            }
    }


    suspend fun getRecentFilesAsync(context: Context): List<DFile> = withIO {
        val where = ContentWhere()
        where.addNotEqual(MediaStore.Files.FileColumns.MIME_TYPE,  "vnd.android.document/directory")
        return@withIO context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), where,
            100, 0, FileSortBy.DATE_DESC.toFileSortBy()
        )?.map { cursor, cache ->
            cursor.toFile(cache)
        } ?: emptyList()
    }
}
