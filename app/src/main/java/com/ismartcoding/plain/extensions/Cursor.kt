package com.ismartcoding.plain.extensions

import android.database.Cursor
import android.provider.MediaStore
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeSecondsValue
import com.ismartcoding.plain.features.file.DFile

fun Cursor.toFile(cache: MutableMap<String, Int>): DFile {
    val id = getStringValue(MediaStore.Files.FileColumns._ID, cache)
    val title = getStringValue(MediaStore.Files.FileColumns.DISPLAY_NAME, cache)
    val size = getLongValue(MediaStore.Files.FileColumns.SIZE, cache)
    val path = getStringValue(MediaStore.Files.FileColumns.DATA, cache)
    val createdAt = getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_ADDED, cache)
    val updatedAt = getTimeSecondsValue(MediaStore.Files.FileColumns.DATE_MODIFIED, cache)
    val mimeType =
        getString(getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))
    val mediaType = getIntValue(MediaStore.Files.FileColumns.MEDIA_TYPE, cache)
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