package com.ismartcoding.plain.docs

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.plain.data.DDoc
import com.ismartcoding.plain.helpers.escapeLike
import com.ismartcoding.plain.lib.extensions.forEach
import com.ismartcoding.plain.lib.extensions.getLongValue
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.getTimeSecondsValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.extensions.queryCursor
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.data.DMediaBucketItemFact
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.extensions.normalizeComparison
import com.ismartcoding.plain.extensions.parseSizeToBytes
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.toSortBy
import com.ismartcoding.plain.features.media.BaseMediaContentHelper

object DocMediaStoreHelper : BaseMediaContentHelper() {
    override val providerType: DataType = DataType.DOC
    override val uriExternal: Uri = if (isQPlus()) MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Files.getContentUri("external")
    override val mediaType: MediaType = MediaType.FILE

    private val extraDocumentMimeTypes = arrayListOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/javascript"
    )

    fun isDocumentMimeType(value: String?): Boolean = value != null && (value.startsWith("text/") || value in extraDocumentMimeTypes)

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

    suspend fun searchAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DDoc> = withIO {
        getPagingCursorAsync(context, query, limit, offset, sortBy.toSortBy())?.map { cursor, cache ->
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
        val where = com.ismartcoding.plain.features.system.RustSystemProviders.providerWhere(providerType, query)
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

    fun getDocBucketItemFactsAsync(context: Context): List<DMediaBucketItemFact> {
        if (!isQPlus()) return emptyList()
        val mimeTypePlaceholders = extraDocumentMimeTypes.joinToString(",") { "?" }
        val selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} IN ($mimeTypePlaceholders)) AND ${MediaStore.Files.FileColumns.SIZE} > 0 AND ${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} != ''"
        val selectionArgs = (listOf("text/%") + extraDocumentMimeTypes).toTypedArray()
        return queryBucketItemFacts(context, selection, selectionArgs)
    }
}
