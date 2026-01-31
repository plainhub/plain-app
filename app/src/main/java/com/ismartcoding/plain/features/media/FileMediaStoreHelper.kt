package com.ismartcoding.plain.features.media

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.enums.FileType
import com.ismartcoding.plain.extensions.normalizeComparison
import com.ismartcoding.plain.extensions.parseSizeToBytes
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.helpers.QueryHelper
import java.io.File
import java.util.Locale

object FileMediaStoreHelper : BaseContentHelper() {
    private val extraAudioMimeTypes = arrayListOf("application/ogg")
    private val extraDocumentMimeTypes = arrayListOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/javascript"
    )

    private val archiveMimeTypes = arrayListOf(
        "application/zip",
        "application/octet-stream",
        "application/json",
        "application/x-tar",
        "application/x-rar-compressed",
        "application/x-zip-compressed",
        "application/x-7z-compressed",
        "application/x-compressed",
        "application/x-gzip",
        "application/java-archive",
        "multipart/x-zip"
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
            cursorToFile(cursor, cache)
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
                cursorToFile(cursor, cache)
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

    fun getAllByFileTypeAsync(
        context: Context, volumeName: String,
        fileType: FileType, sortBy: FileSortBy,
        showHidden: Boolean = false
    ): List<DFile> {
        val items = ArrayList<DFile>()
        val uri = MediaStore.Files.getContentUri(volumeName)
        val projection = getProjection()
        context.contentResolver.queryCursor(uri, projection)?.forEach { cursor, cache ->
            val id = cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)

            val fullMimetype = cursor.getStringValue(MediaStore.Files.FileColumns.MIME_TYPE, cache).lowercase(Locale.getDefault())
            val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
            if (!showHidden && name.startsWith(".")) {
                return@forEach
            }

            val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE, cache)
            if (size == 0L) {
                return@forEach
            }

            val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA, cache)
            val createdAt = cursor.getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_ADDED, cache)
            val updatedAt = cursor.getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_MODIFIED, cache)
            val mimetype = fullMimetype.substringBefore("/")
            when (fileType) {
                FileType.IMAGE -> {
                    if (mimetype == "image") {
                        items.add(DFile(name, path, "", createdAt, updatedAt, size, false, 0, id))
                    }
                }

                FileType.VIDEO -> {
                    if (mimetype == "video") {
                        items.add(DFile(name, path, "", createdAt, updatedAt, size, false, 0, id))
                    }
                }

                FileType.AUDIO -> {
                    if (mimetype == "audio" || extraAudioMimeTypes.contains(fullMimetype)) {
                        items.add(DFile(name, path, "", createdAt, updatedAt, size, false, 0, id))
                    }
                }

                FileType.DOCUMENT -> {
                    if (mimetype == "text" || extraDocumentMimeTypes.contains(fullMimetype)) {
                        items.add(DFile(name, path, "", createdAt, updatedAt, size, false, 0, id))
                    }
                }

                FileType.ARCHIVE -> {
                    if (archiveMimeTypes.contains(fullMimetype)) {
                        items.add(DFile(name, path, "", createdAt, updatedAt, size, false, 0, id))
                    }
                }

                FileType.OTHER -> {
                    if (!setOf("image", "video", "audio", "text").contains(fullMimetype) &&
                        !extraAudioMimeTypes.contains(fullMimetype) && !extraDocumentMimeTypes.contains(fullMimetype) &&
                        !archiveMimeTypes.contains(fullMimetype)
                    ) {
                        items.add(DFile(name, path, "", createdAt, updatedAt, size, false, 0, id))
                    }
                }
            }
        }

        return items.sorted(sortBy)
    }

    suspend fun getRecentFilesAsync(context: Context): List<DFile> {
        val where = ContentWhere()
        where.addNotEqual(MediaStore.Files.FileColumns.MIME_TYPE,  "vnd.android.document/directory")
        return context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), where,
            100, 0, FileSortBy.DATE_DESC.toFileSortBy()
        )?.map { cursor, cache ->
            cursorToFile(cursor, cache)
        } ?: emptyList()
    }

    private fun cursorToFile(cursor: Cursor, cache: MutableMap<String, Int>): DFile {
        val id = cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)
        val title = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
        val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE, cache)
        val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA, cache)
        val createdAt = cursor.getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_ADDED, cache)
        val updatedAt = cursor.getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_MODIFIED, cache)
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
        val mediaType = cursor.getIntValue(MediaStore.Files.FileColumns.MEDIA_TYPE, cache)
        return DFile(
            title,
            path,
            "",
            createdAt,
            updatedAt,
            size,
            mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_NONE && (mimeType == null || mimeType == "vnd.android.document/directory"),
            0,
            id
        )
    }
}
