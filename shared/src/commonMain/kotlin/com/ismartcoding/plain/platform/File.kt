package com.ismartcoding.plain.platform

import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat

expect fun getFileId(path: String): String

suspend fun releaseAppFile(fidSuffix: String) = withIO {
    val hash = fidSuffix.substringBefore(".")
    val dao = AppDatabase.instance.appFileDao()
    dao.decrementRefCount(hash)
    val updated = dao.getById(hash) ?: return@withIO
    if (updated.refCount <= 0) {
        dao.delete(hash)
        deleteFileAt(updated.realPath)
        LogCat.d("AppFileStore: deleted orphan file $hash")
    }
}

expect fun deleteFileAt(path: String)

expect fun createLongTextFile(text: String): DMessageContent

/**
 * Copy a file into the system Downloads folder.
 * Returns the destination path on success, empty string on failure.
 */
expect fun saveFileToDownloads(path: String, fileName: String): String

/**
 * Convert a filesystem path to a URI string suitable for viewers (e.g. PDF viewer).
 */
expect fun fileToUriString(path: String): String

/**
 * Returns the asset path for the icon representing the given file extension.
 */
expect fun getFileIconPath(extension: String): String

/**
 * Whether a file exists at the given [path].
 */
expect fun fileExists(path: String): Boolean
