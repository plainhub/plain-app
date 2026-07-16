package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.DStorageStatsItem
import com.ismartcoding.plain.features.file.FileSortBy

actual fun getInternalStoragePath(): String = ""

actual fun getInternalStorageName(): String = "Internal Storage"

actual fun getSDCardPath(): String = ""

actual fun getUsbDiskPaths(): List<String> = emptyList()

actual fun listFilesInDir(dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> = emptyList()

actual suspend fun searchFilesInDir(query: String, root: String, sortBy: FileSortBy): List<DFile> = emptyList()

actual fun searchFilesByName(query: String, dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> = emptyList()

actual suspend fun getRecentFiles(): List<DFile> = emptyList()

actual fun createDirectory(path: String): DFile = DFile(
    name = path.substringAfterLast('/'),
    path = path,
    permission = "",
    createdAt = null,
    updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
    size = 0,
    isDir = true,
    children = 0,
)

actual fun createFile(path: String): DFile = DFile(
    name = path.substringAfterLast('/'),
    path = path,
    permission = "",
    createdAt = null,
    updatedAt = kotlin.time.Instant.fromEpochMilliseconds(0),
    size = 0,
    isDir = false,
    children = 0,
)

actual fun scanFiles(paths: Array<String>) {}

actual suspend fun renameAndScanFile(path: String, newName: String): String? = null

actual fun getInternalStorageStats(): DStorageStatsItem = DStorageStatsItem(0, 0)

actual fun getSDCardStorageStats(): DStorageStatsItem = DStorageStatsItem(0, 0)

actual fun getUSBStorageStats(): List<DStorageStatsItem> = emptyList()

actual fun listZipEntries(zipVirtualPath: String, sortBy: FileSortBy): List<DFile> = emptyList()

actual fun extractZipEntryToCache(zipVirtualPath: String): String? = null

actual fun deleteFileOrDir(path: String): Boolean = false

actual fun getNewPath(path: String): String {
    val dotIndex = path.lastIndexOf('.')
    val slashIndex = path.lastIndexOf('/')
    val base = if (dotIndex > slashIndex) path.substring(0, dotIndex) else path
    val ext = if (dotIndex > slashIndex) path.substring(dotIndex) else ""
    return "$base (1)$ext"
}

actual fun getCanonicalPath(path: String): String = path

actual fun copyFileOrDir(srcPath: String, destPath: String): Boolean = false

actual fun moveFileOrDir(srcPath: String, destPath: String): Boolean = false

actual fun zipFiles(sourcePaths: List<String>, targetPath: String): Boolean = false

actual fun unzipFile(zipPath: String, destPath: String): Boolean = false

actual fun statFile(path: String): DFile? = null
