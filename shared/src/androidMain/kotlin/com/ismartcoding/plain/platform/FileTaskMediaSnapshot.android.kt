package com.ismartcoding.plain.platform

import android.provider.MediaStore
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.enums.DataType

actual fun queryFileTaskMedia(paths: List<String>): List<FileTaskMediaIdentity> {
    if (paths.isEmpty()) return emptyList()
    require(paths.size <= 128)
    check(Permission.WRITE_EXTERNAL_STORAGE.isGranted()) { "Media identity permission denied" }
    val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.Files.FileColumns.MIME_TYPE)
    val where = "${MediaStore.Files.FileColumns.DATA} IN (${paths.joinToString(",") { "?" }})"
    val rows = mutableListOf<FileTaskMediaIdentity>()
    checkNotNull(appContext.contentResolver.query(MediaStore.Files.getContentUri("external"), projection, where, paths.toTypedArray(), null)) { "Media identity provider unavailable" }.use { cursor ->
        while (cursor.moveToNext()) {
            val type = when (cursor.getInt(2)) {
                MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> DataType.AUDIO
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> DataType.VIDEO
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> DataType.IMAGE
                else -> if (DocMediaStoreHelper.isDocumentMimeType(cursor.getString(3))) DataType.DOC else null
            } ?: continue
            val path = cursor.getString(1) ?: continue
            if (path in paths) rows.add(FileTaskMediaIdentity(type, cursor.getLong(0).toString(), path))
        }
    }
    return rows
}
