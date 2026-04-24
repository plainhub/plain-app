package com.ismartcoding.plain.docs

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.forEach
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeSecondsValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.extensions.queryCursor
import com.ismartcoding.lib.extensions.toSortName
import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.extensions.normalizeComparison
import com.ismartcoding.plain.extensions.parseSizeToBytes
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.BaseMediaContentHelper
import com.ismartcoding.plain.helpers.QueryHelper

object DocMediaStoreHelper : BaseMediaContentHelper() {
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Files.getContentUri("external")
    override val mediaType: MediaType = MediaType.FILE

    private val extraDocumentMimeTypes = arrayListOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/javascript"
    )

    override fun getProjection(): Array<String> {
        val cols = mutableListOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
        if (isQPlus()) {
            cols.add(MediaStore.MediaColumns.BUCKET_ID)
        }
        return cols.toTypedArray()
    }

    override fun buildBaseWhere(filterFields: List<FilterField>): ContentWhere {
        val where = ContentWhere()

        val mimeTypePlaceholders = extraDocumentMimeTypes.joinToString(",") { "?" }
        where.add("(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} IN ($mimeTypePlaceholders))")
        where.args.add("text/%")
        where.args.addAll(extraDocumentMimeTypes)
        where.addGt(MediaStore.Files.FileColumns.SIZE, "0")

        filterFields.forEach {
            when (it.name) {
                "text" -> where.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?", "%${it.value}%")
                "ext" -> where.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?", "%.${it.value}")
                "parent" -> where.add("${MediaStore.Files.FileColumns.DATA} LIKE ?", "${it.value}/%")
                "type" -> where.add("${MediaStore.Files.FileColumns.MIME_TYPE} = ?", it.value)
                "file_size" -> {
                    val (rawOp, rawValue) = it.normalizeComparison(defaultOp = "=")
                    val bytes = rawValue.parseSizeToBytes() ?: return@forEach
                    val op = when (rawOp) {
                        ">", ">=", "<", "<=", "!=", "=" -> rawOp
                        else -> "="
                    }
                    where.add("${MediaStore.Files.FileColumns.SIZE} $op ?", bytes.toString())
                }
                "bucket_id" -> if (isQPlus()) {
                    where.addEqual(MediaStore.MediaColumns.BUCKET_ID, it.value)
                }
                "trash" -> where.trash = it.value.toBooleanStrictOrNull()
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
    ): List<DDoc> {
        return getPagingCursorAsync(context, query, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)
            val title = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
            val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE, cache)
            val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA, cache)
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_MODIFIED, cache)
            val bucketId = if (isQPlus()) {
                cursor.getStringValue(MediaStore.MediaColumns.BUCKET_ID, cache)
            } else ""
            DDoc(id, title, path, 0, size, bucketId, createdAt, updatedAt)
        } ?: emptyList()
    }

    suspend fun getTagRelationStubsAsync(
        context: Context,
        query: String,
    ): List<TagRelationStub> {
        return getSearchCursorAsync(context, query)?.map { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)
            val title = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
            val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE, cache)
            TagRelationStub(id, title, size)
        } ?: emptyList()
    }

    suspend fun getDocExtGroupsAsync(context: Context, query: String = ""): List<Pair<String, Int>> {
        val where = buildBaseWhere(QueryHelper.parseAsync(query))
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

    fun getDocBucketsAsync(context: Context): List<DMediaBucket> {
        if (!isQPlus()) return emptyList()
        val bucketMap = mutableMapOf<String, DMediaBucket>()
        val projection = arrayOf(
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATA,
        )
        val mimeTypePlaceholders = extraDocumentMimeTypes.joinToString(",") { "?" }
        val selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} IN ($mimeTypePlaceholders)) AND ${MediaStore.Files.FileColumns.SIZE} > 0 AND ${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} != ''"
        val selectionArgs = (listOf("text/%") + extraDocumentMimeTypes).toTypedArray()
        context.contentResolver.query(uriExternal, projection, selection, selectionArgs, null)?.forEach { cursor, cache ->
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
        return bucketMap.values.sortedBy {  it.name.toSortName() }
    }
}