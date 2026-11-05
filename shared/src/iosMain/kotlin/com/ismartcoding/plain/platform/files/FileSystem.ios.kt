@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.files

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

actual fun getSDCardPath(): String = ""

actual fun getUsbDiskPaths(): List<String> = emptyList()

actual fun getInternalStorageStats(): StorageStats {
    val path = getInternalStoragePath()
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

actual fun getSDCardStorageStats(): StorageStats = StorageStats(0, 0)

actual fun getUSBStorageStats(): List<StorageStats> = emptyList()

internal actual fun grantedStorageDirectories(): List<String> =
    checkNotNull(com.ismartcoding.plain.platform.system.IosPlatformRegistry.filePicker()).grantedDirectories()
