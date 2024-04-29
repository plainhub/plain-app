package com.ismartcoding.plain.features.file

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.FileType
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.features.BaseContentHelper
import kotlinx.datetime.Instant
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
    override val idKey: String = MediaStore.Files.FileColumns._ID

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

    override fun getWhere(query: String): ContentWhere {
        val where = ContentWhere()
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            queryGroups.forEach {
                if (it.name == "text") {
                    where.add("${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?", "%${it.value}%")
                } else if (it.name == "ids") {
                    val ids = it.value.split(",")
                    if (ids.isNotEmpty()) {
                        where.addIn(MediaStore.Files.FileColumns._ID, ids)
                    }
                }
            }
        }
        return where
    }

    fun search(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
        sortBy: FileSortBy,
    ): List<DFile> {
        val cursor = getSearchCursor(context, query, limit, offset, sortBy.toSortBy())
        val result = mutableListOf<DFile>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
                val id = cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)
                val title = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
                val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE, cache)
                val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA, cache)
                val createdAt = cursor.getTimeValue(MediaStore.Files.FileColumns.DATE_ADDED, cache)
                val updatedAt = cursor.getTimeValue(MediaStore.Files.FileColumns.DATE_MODIFIED, cache)
                val mediaType = cursor.getIntValue(MediaStore.Files.FileColumns.MEDIA_TYPE, cache)
                result.add(
                    DFile(
                        title,
                        path,
                        "",
                        createdAt,
                        updatedAt,
                        size,
                        mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_NONE,
                        0,
                        id
                    ),
                )
            } while (cursor.moveToNext())
        }
        return result
    }

    fun getById(context: Context, id: String): DFile? {
        var file: DFile? = null
        context.queryCursor(uriExternal, getProjection(), "${MediaStore.Files.FileColumns._ID} = ?", arrayOf(id)) { cursor, indexCache ->
            try {
                val title = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, indexCache)
                val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE, indexCache)
                val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA, indexCache)
                val createdAt = Instant.fromEpochMilliseconds(
                    cursor.getLongValue(MediaStore.Files.FileColumns.DATE_ADDED, indexCache) * 1000L,
                )
                val updatedAt = Instant.fromEpochMilliseconds(
                    cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED, indexCache) * 1000L,
                )
                file = DFile(
                    title,
                    path,
                    "",
                    createdAt,
                    updatedAt,
                    size,
                    false,
                    0,
                    id
                )
            } catch (e: Exception) {
                LogCat.e(e.toString())
            }
        }

        return file
    }

    fun getAllByFileType(
        context: Context, volumeName: String,
        fileType: FileType, sortBy: FileSortBy,
        showHidden: Boolean = false
    ): List<DFile> {
        val items = ArrayList<DFile>()
        val uri = MediaStore.Files.getContentUri(volumeName)
        val projection = getProjection()
        context.queryCursor(uri, projection) { cursor, cache ->
            try {
                val id = cursor.getStringValue(MediaStore.Files.FileColumns._ID, cache)

                val fullMimetype = cursor.getStringValue(MediaStore.Files.FileColumns.MIME_TYPE, cache).lowercase(Locale.getDefault())
                val name = cursor.getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
                if (!showHidden && name.startsWith(".")) {
                    return@queryCursor
                }

                val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE, cache)
                if (size == 0L) {
                    return@queryCursor
                }

                val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA, cache)
                val createdAt = Instant.fromEpochMilliseconds(
                    cursor.getLongValue(MediaStore.Files.FileColumns.DATE_ADDED, cache) * 1000L,
                )
                val updatedAt = Instant.fromEpochMilliseconds(
                    cursor.getLongValue(MediaStore.Files.FileColumns.DATE_MODIFIED, cache) * 1000L,
                )
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
            } catch (e: Exception) {
            }
        }

        return items.sorted(sortBy)
    }
}
