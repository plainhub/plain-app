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

object DocsHelper : BaseContentHelper() {
    private val extraDocumentMimeTypes = arrayListOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/javascript"
    )

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

        // Base filter: doc MIME types and non-empty files
        val mimeTypePlaceholders = extraDocumentMimeTypes.joinToString(",") { "?" }
        where.add("(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} IN ($mimeTypePlaceholders))")
        where.args.add("text/%")
        where.args.addAll(extraDocumentMimeTypes)
        where.addGt(MediaStore.Files.FileColumns.SIZE, "0")

        var showHidden = false
        if (query.isNotEmpty()) {
            QueryHelper.parseAsync(query).forEach {
                when (it.name) {
                    "text" -> where.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?", "%${it.value}%")
                    "ext" -> where.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?", "%.${it.value}")
                    "parent" -> where.add("${MediaStore.Files.FileColumns.PARENT} = ?", getIdByPathAsync(MainApp.instance, it.value) ?: "-1")
                    "type" -> where.add("${MediaStore.Files.FileColumns.MIME_TYPE} = ?", it.value)
                    "show_hidden" -> showHidden = it.value.toBoolean()
                    "file_size" -> {
                        val (rawOp, rawValue) = it.normalizeComparison(defaultOp = "=")
                        val bytes = rawValue.parseSizeToBytes() ?: return@forEach
                        val op = when (rawOp) {
                            ">", ">=", "<", "<=", "!=", "=" -> rawOp
                            else -> "="
                        }
                        where.add("${MediaStore.Files.FileColumns.SIZE} $op ?", bytes.toString())
                    }
                    "ids" -> where.addIn(MediaStore.Files.FileColumns._ID, it.value.split(","))
                }
            }
        }

        if (!showHidden) {
            where.addNotStartsWith(MediaStore.Files.FileColumns.DISPLAY_NAME, ".")
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
        return context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), buildWhereAsync(query),
            limit, offset, sortBy.toFileSortBy()
        )?.map { cursor, cache ->
            cursor.toFile(cache)
        } ?: emptyList()
    }

    suspend fun getDocExtGroupsAsync(context: Context, query: String = ""): List<Pair<String, Int>> {
        val where = buildWhereAsync(query)
        val extCounts = mutableMapOf<String, Int>()
        context.contentResolver.queryCursor(
            uriExternal,
            arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME),
            where.toSelection(),
            where.args.toTypedArray()
        )?.forEach { cursor, cache ->
            val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext.isNotEmpty()) {
                extCounts[ext] = extCounts.getOrDefault(ext, 0) + 1
            }
        }
        return extCounts.map { Pair(it.key.uppercase(), it.value) }.sortedBy { it.first }
    }

    private fun getIdByPathAsync(context: Context, path: String): String? {
        return context.contentResolver
            .queryCursor(uriExternal, arrayOf(MediaStore.Files.FileColumns._ID), "${MediaStore.Files.FileColumns.DATA} = ?", arrayOf(path))?.find { cursor, cache ->
                cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)
            }
    }
}
