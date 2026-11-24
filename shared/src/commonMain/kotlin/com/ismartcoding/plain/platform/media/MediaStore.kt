package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.features.media.actions.MediaAction
import com.ismartcoding.plain.features.media.actions.MediaActionClient

/**
 * Move media items of [dataType] identified by [ids] to the trash.
 */
suspend fun trashMedia(dataType: DataType, ids: Set<String>) {
    MediaActionClient.run(dataType, MediaAction.TRASH, ids)
}

/**
 * Restore media items of [dataType] identified by [ids] from the trash.
 */
suspend fun restoreMedia(dataType: DataType, ids: Set<String>) {
    MediaActionClient.run(dataType, MediaAction.RESTORE, ids)
}

/**
 * Delete the selected items from media trash.
 */
suspend fun deleteMedia(dataType: DataType, ids: Set<String>) {
    MediaActionClient.run(dataType, MediaAction.DELETE, ids)
}

/**
 * Move media files of [dataType] identified by [ids] into the directory [destDir].
 * Returns true when all items were moved successfully.
 */
suspend fun moveMedia(dataType: DataType, ids: Set<String>, destDir: String): Boolean =
    MediaActionClient.run(dataType, MediaAction.MOVE, ids, destDir = destDir) == ids.size

expect fun getMediaItemUriString(dataType: DataType, id: String, path: String): String

/**
 * Returns the absolute path of the platform's public Downloads directory.
 * Empty string on unsupported platforms.
 */
fun getDownloadsDirPath(): String = downloadsDirectory

private val downloadsDirectory: String by lazy { platformDownloadsDirPath() }

internal expect fun platformDownloadsDirPath(): String
