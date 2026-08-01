@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.extensions.normalizeComparison
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.DStorageStatsItem
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.helpers.FilterField
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.time.Instant
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.toKotlinInstant
import platform.Foundation.NSDate
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory

actual fun getInternalStoragePath(): String = appDir()

actual fun getInternalStorageName(): String = "Internal Storage"

actual fun getSDCardPath(): String = ""

actual fun getUsbDiskPaths(): List<String> = emptyList()

actual fun listFilesInDir(dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> {
    val mgr = NSFileManager.defaultManager
    if (!mgr.fileExistsAtPath(dir)) return emptyList()
    val names = mgr.contentsOfDirectoryAtPath(dir, null)?.filterIsInstance<String>() ?: return emptyList()
    val files = ArrayList<DFile>()
    for (name in names) {
        if (!showHidden && name.startsWith(".")) continue
        val fullPath = joinPath(dir, name)
        val dfile = buildDFile(fullPath, name, showHidden, computeChildren = true) ?: continue
        files.add(dfile)
    }
    return files.sorted(sortBy)
}

actual suspend fun searchFilesInDir(query: String, root: String, sortBy: FileSortBy): List<DFile> = withIO {
    val filterFields = QueryHelper.parseAsync(query)
    val showHidden = filterFields.find { it.name == "show_hidden" }?.value?.toBoolean() ?: false
    val text = filterFields.find { it.name == "text" }?.value ?: ""
    val parent = filterFields.find { it.name == "parent" }?.value ?: ""
    val fileSizeFields = filterFields.filter { it.name == "file_size" }
    val dir = parent.ifEmpty { root }
    val items = if (text.isNotEmpty() || fileSizeFields.isNotEmpty()) {
        searchRecursive(text, dir, showHidden).sorted(sortBy)
    } else {
        listFilesInDir(dir, showHidden, sortBy)
    }
    if (fileSizeFields.isEmpty()) return@withIO items
    return@withIO items.filter { !it.isDir && matchFileSizeFilters(it.size, fileSizeFields) }
}

actual fun searchFilesByName(query: String, dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> =
    listFilesInDir(dir, showHidden, sortBy).filter { it.name.contains(query, ignoreCase = true) }

actual suspend fun getRecentFiles(): List<DFile> = emptyList()

actual fun createDirectory(path: String): DFile {
    NSFileManager.defaultManager.createDirectoryAtPath(
        path, withIntermediateDirectories = true, attributes = null, error = null,
    )
    return DFile(
        name = path.substringAfterLast('/'),
        path = path,
        permission = "",
        createdAt = null,
        updatedAt = Instant.fromEpochMilliseconds(0),
        size = 0,
        isDir = true,
        children = 0,
    )
}

actual fun createFile(path: String): DFile {
    NSFileManager.defaultManager.createFileAtPath(path, null, null)
    return DFile(
        name = path.substringAfterLast('/'),
        path = path,
        permission = "",
        createdAt = null,
        updatedAt = Instant.fromEpochMilliseconds(0),
        size = 0,
        isDir = false,
        children = 0,
    )
}

actual fun scanFiles(paths: Array<String>) {}

actual suspend fun renameAndScanFile(path: String, newName: String): String? = withIO {
    val parent = path.substringBeforeLast('/')
    val newPath = joinPath(parent, newName)
    try {
        if (NSFileManager.defaultManager.moveItemAtPath(path, newPath, error = null)) newPath else null
    } catch (e: Exception) {
        LogCat.e("renameAndScanFile: ${e.message}")
        null
    }
}

actual fun getInternalStorageStats(): DStorageStatsItem {
    val path = appDir()
    return try {
        val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null)
            ?: return DStorageStatsItem(0, 0)
        val total = (attrs[NSFileSystemSize] as? Long) ?: 0L
        val free = (attrs[NSFileSystemFreeSize] as? Long) ?: 0L
        DStorageStatsItem(total, free)
    } catch (e: Exception) {
        DStorageStatsItem(0, 0)
    }
}

actual fun getSDCardStorageStats(): DStorageStatsItem = DStorageStatsItem(0, 0)

actual fun getUSBStorageStats(): List<DStorageStatsItem> = emptyList()

actual fun listZipEntries(zipVirtualPath: String, sortBy: FileSortBy): List<DFile> = emptyList()

actual fun extractZipEntryToCache(zipVirtualPath: String): String? = null

actual fun deleteFileOrDir(path: String): Boolean {
    return try {
        NSFileManager.defaultManager.removeItemAtPath(path, null)
    } catch (e: Exception) {
        false
    }
}

actual fun getNewPath(path: String): String {
    val dotIndex = path.lastIndexOf('.')
    val slashIndex = path.lastIndexOf('/')
    val base = if (dotIndex > slashIndex) path.substring(0, dotIndex) else path
    val ext = if (dotIndex > slashIndex) path.substring(dotIndex) else ""
    return "$base (1)$ext"
}

actual fun getCanonicalPath(path: String): String = path

actual fun copyFileOrDir(srcPath: String, destPath: String): Boolean {
    return try {
        NSFileManager.defaultManager.copyItemAtPath(srcPath, destPath, error = null)
    } catch (e: Exception) {
        false
    }
}

actual fun moveFileOrDir(srcPath: String, destPath: String): Boolean {
    return try {
        NSFileManager.defaultManager.moveItemAtPath(srcPath, destPath, error = null)
    } catch (e: Exception) {
        false
    }
}

actual fun zipFiles(sourcePaths: List<String>, targetPath: String): Boolean = false

actual fun unzipFile(zipPath: String, destPath: String): Boolean = false

actual fun statFile(path: String): DFile? {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
    val name = path.substringAfterLast('/')
    return buildDFile(path, name, showHidden = false, computeChildren = false)
}

private fun joinPath(dir: String, name: String): String =
    if (dir.endsWith("/")) "$dir$name" else "$dir/$name"

private fun buildDFile(
    path: String,
    name: String,
    showHidden: Boolean,
    computeChildren: Boolean,
): DFile? {
    val mgr = NSFileManager.defaultManager
    val attrs = try {
        mgr.attributesOfItemAtPath(path, null) ?: return null
    } catch (e: Exception) {
        return null
    }
    val isDir = attrs[NSFileType] == NSFileTypeDirectory
    val size = if (isDir) 0L else (attrs[NSFileSize] as? Long) ?: 0L
    val updatedAt = (attrs[NSFileModificationDate] as? NSDate?)?.toKotlinInstant()
        ?: Instant.fromEpochMilliseconds(0)
    val children = if (isDir && computeChildren) countChildren(path, showHidden) else 0
    return DFile(
        name = name,
        path = path,
        permission = "",
        createdAt = null,
        updatedAt = updatedAt,
        size = size,
        isDir = isDir,
        children = children,
        mediaId = "",
    )
}

private fun countChildren(dir: String, showHidden: Boolean): Int {
    val names = NSFileManager.defaultManager.contentsOfDirectoryAtPath(dir, null)?.filterIsInstance<String>() ?: return 0
    return if (showHidden) names.size else names.count { !it.startsWith(".") }
}

private fun searchRecursive(query: String, root: String, showHidden: Boolean): List<DFile> {
    val mgr = NSFileManager.defaultManager
    if (!mgr.fileExistsAtPath(root)) return emptyList()
    val enumerator = mgr.enumeratorAtPath(root) ?: return emptyList()
    val results = ArrayList<DFile>()
    while (true) {
        val raw = enumerator.nextObject() ?: break
        val rel = raw.toString()
        if (!showHidden && rel.split('/').any { it.startsWith(".") }) continue
        val name = rel.substringAfterLast('/')
        if (query.isNotEmpty() && !name.contains(query, ignoreCase = true)) continue
        val fullPath = joinPath(root, rel)
        val dfile = buildDFile(fullPath, name, showHidden, computeChildren = false) ?: continue
        results.add(dfile)
    }
    return results
}

private fun matchFileSizeFilters(size: Long, fileSizeFields: List<FilterField>): Boolean {
    for (f in fileSizeFields) {
        val (op, rawValue) = f.normalizeComparison(defaultOp = "=")
        val bytes = rawValue.toLongOrNull() ?: return false
        val ok = when (op) {
            ">" -> size > bytes
            ">=" -> size >= bytes
            "<" -> size < bytes
            "<=" -> size <= bytes
            "!=" -> size != bytes
            "=", "" -> size == bytes
            else -> true
        }
        if (!ok) return false
    }
    return true
}
