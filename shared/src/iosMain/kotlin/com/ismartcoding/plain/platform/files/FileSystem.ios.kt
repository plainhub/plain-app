@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.features.files.DriveType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private val documentsDirectory by lazy {
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0] as String
}

actual fun getInternalStoragePath(): String = documentsDirectory

actual fun getInternalStorageName(): String = "Internal Storage"

internal actual fun storageRoots(): List<StorageRoot> = listOf(StorageRoot(documentsDirectory, DriveType.INTERNAL_STORAGE))

internal actual fun storageStats(path: String): StorageStats? {
    return try {
        val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)
            ?: return StorageStats(0, 0)
        val total = (attrs[NSFileSystemSize] as? Long) ?: 0L
        val free = (attrs[NSFileSystemFreeSize] as? Long) ?: 0L
        StorageStats(total, free)
    } catch (e: Exception) {
        StorageStats(0, 0)
    }
}

internal actual fun grantedStorageDirectories(): List<String> =
    checkNotNull(com.ismartcoding.plain.platform.system.IosPlatformRegistry.filePicker()).grantedDirectories()
