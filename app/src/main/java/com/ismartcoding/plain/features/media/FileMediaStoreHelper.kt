package com.ismartcoding.plain.features.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.find
import com.ismartcoding.lib.extensions.forEach
import com.ismartcoding.lib.extensions.getPagingCursor
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.extensions.queryCursor
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.extensions.normalizeComparison
import com.ismartcoding.plain.extensions.parseSizeToBytes
import com.ismartcoding.plain.extensions.toFile
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
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
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            var showHidden = false
            QueryHelper.parseAsync(query).forEach {
                when (it.name) {
                    "text" -> {
                        where.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?", "%${it.value}%")
                    }
                    "parent" -> {
                        where.add("${MediaStore.Files.FileColumns.PARENT} = ?", getIdByPathAsync(MainApp.instance, it.value) ?: "-1")
                    }
                    "type" -> {
                        where.add("${MediaStore.Files.FileColumns.MIME_TYPE} = ?", it.value)
                    }
                    "show_hidden" -> {
                        showHidden = it.value.toBoolean()
                    }
                    "file_size" -> {
                        val (rawOp, rawValue) = it.normalizeComparison(defaultOp = "=")
                        val bytes = rawValue.parseSizeToBytes() ?: return@forEach
                        val op = when (rawOp) {
                            ">", ">=", "<", "<=", "!=", "=" -> rawOp
                            else -> "="
                        }
                        where.add("${MediaStore.Files.FileColumns.SIZE} $op ?", bytes.toString())
                    }
                    "ids" -> {
                        where.addIn(MediaStore.Files.FileColumns._ID, it.value.split(","))
                    }
                }
            }

            if (!showHidden) {
                where.addNotStartsWith(MediaStore.Files.FileColumns.DISPLAY_NAME, ".")
            }
        }
        return where
    }

    suspend fun searchAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DFile> {
        val items = context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), buildWhereAsync(query),
            limit, offset, sortBy.toFileSortBy()
        )?.map { cursor, cache ->
            cursor.toFile(cache)
        } ?: emptyList()
        val folderIds = items.filter { it.isDir }.map { it.mediaId }
        val counts = getChildrenCountAsync(context, folderIds)
        return items.map {
            it.copy(children = counts[it.mediaId] ?: 0)
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


    suspend fun getRecentFilesAsync(context: Context): List<DFile> {
        val where = ContentWhere()
        where.addNotEqual(MediaStore.Files.FileColumns.MIME_TYPE,  "vnd.android.document/directory")
        return context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), where,
            100, 0, FileSortBy.DATE_DESC.toFileSortBy()
        )?.map { cursor, cache ->
            cursor.toFile(cache)
        } ?: emptyList()
    }
}
