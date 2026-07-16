package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.DStorageStatsItem
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.lib.logcat.LogCat
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.io.path.moveTo
import kotlin.time.Instant

actual fun getInternalStoragePath(): String = FileSystemHelper.getInternalStoragePath()

actual fun getInternalStorageName(): String = FileSystemHelper.getInternalStorageName()

actual fun getSDCardPath(): String = FileSystemHelper.getSDCardPath(appContext)

actual fun getUsbDiskPaths(): List<String> = FileSystemHelper.getUsbDiskPaths()

actual fun listFilesInDir(dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> =
    FileSystemHelper.getFilesList(dir, showHidden, sortBy)

actual suspend fun searchFilesInDir(query: String, root: String, sortBy: FileSortBy): List<DFile> =
    FileSystemHelper.search(query, root, sortBy)

actual fun searchFilesByName(query: String, dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile> =
    FileSystemHelper.search(query, dir, showHidden).sorted(sortBy)

actual suspend fun getRecentFiles(): List<DFile> = withIO {
    if (isQPlus()) {
        com.ismartcoding.plain.features.media.FileMediaStoreHelper.getRecentFilesAsync(appContext)
    } else {
        FileSystemHelper.getRecentFiles().map { file ->
            DFile(
                name = file.name,
                path = file.absolutePath,
                permission = "",
                createdAt = null,
                updatedAt = Instant.fromEpochMilliseconds(file.lastModified()),
                size = file.length(),
                isDir = false,
                children = 0,
                mediaId = "",
            )
        }
    }
}

actual fun createDirectory(path: String): DFile = FileSystemHelper.createDirectory(path)

actual fun createFile(path: String): DFile = FileSystemHelper.createFile(path)

actual fun scanFiles(paths: Array<String>) {
    appContext.scanFileByConnection(paths)
}

actual suspend fun renameAndScanFile(path: String, newName: String): String? {
    val newFile = FileHelper.rename(path, newName)
    appContext.scanFileByConnection(path)
    if (newFile != null) {
        appContext.scanFileByConnection(newFile.absolutePath)
    }
    return newFile?.absolutePath
}

actual fun getInternalStorageStats(): DStorageStatsItem = FileSystemHelper.getInternalStorageStats()

actual fun getSDCardStorageStats(): DStorageStatsItem = FileSystemHelper.getSDCardStorageStats(appContext)

actual fun getUSBStorageStats(): List<DStorageStatsItem> = FileSystemHelper.getUSBStorageStats()

actual fun listZipEntries(zipVirtualPath: String, sortBy: FileSortBy): List<DFile> {
    val zipFilePath = ZipBrowserHelper.getZipFilePath(zipVirtualPath)
    val internalDir = ZipBrowserHelper.getInternalPath(zipVirtualPath)
    val prefix = when {
        internalDir.isEmpty() -> ""
        internalDir.endsWith("/") -> internalDir
        else -> "$internalDir/"
    }
    val entries = linkedMapOf<String, DFile>()
    try {
        ZipInputStream(File(zipFilePath).inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (entryName.startsWith(prefix)) {
                    val relative = entryName.removePrefix(prefix)
                    if (relative.isNotEmpty()) {
                        val slashIndex = relative.indexOf('/')
                        when {
                            slashIndex == -1 -> {
                                if (!entries.containsKey(relative)) {
                                    entries[relative] = DFile(
                                        name = relative,
                                        path = ZipBrowserHelper.joinPath(zipFilePath, "$prefix$relative"),
                                        permission = "",
                                        createdAt = null,
                                        updatedAt = if (entry.time > 0) Instant.fromEpochMilliseconds(entry.time) else Instant.fromEpochMilliseconds(0),
                                        size = entry.size.coerceAtLeast(0),
                                        isDir = false,
                                        children = 0,
                                    )
                                }
                            }
                            slashIndex == relative.length - 1 -> {
                                val dirName = relative.dropLast(1)
                                if (dirName.isNotEmpty() && !entries.containsKey(dirName)) {
                                    entries[dirName] = DFile(
                                        name = dirName,
                                        path = ZipBrowserHelper.joinPath(zipFilePath, "$prefix$dirName/"),
                                        permission = "",
                                        createdAt = null,
                                        updatedAt = if (entry.time > 0) Instant.fromEpochMilliseconds(entry.time) else Instant.fromEpochMilliseconds(0),
                                        size = 0,
                                        isDir = true,
                                        children = 0,
                                    )
                                }
                            }
                            else -> {
                                val dirName = relative.substring(0, slashIndex)
                                if (!entries.containsKey(dirName)) {
                                    entries[dirName] = DFile(
                                        name = dirName,
                                        path = ZipBrowserHelper.joinPath(zipFilePath, "$prefix$dirName/"),
                                        permission = "",
                                        createdAt = null,
                                        updatedAt = Instant.fromEpochMilliseconds(0),
                                        size = 0,
                                        isDir = true,
                                        children = 0,
                                    )
                                }
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    } catch (e: Exception) {
        LogCat.e(e.toString())
    }
    return entries.values.toList().sorted(sortBy)
}

actual fun extractZipEntryToCache(zipVirtualPath: String): String? {
    val zipFilePath = ZipBrowserHelper.getZipFilePath(zipVirtualPath)
    val internalPath = ZipBrowserHelper.getInternalPath(zipVirtualPath).trimEnd('/')
    if (internalPath.isEmpty()) return null
    val rawName = internalPath.substringAfterLast('/')
    val safeName = rawName.replace("[/\\\\:*?\"<>|]".toRegex(), "_").take(80)
    val cacheKey = "${zipFilePath.hashCode().toUInt()}_${internalPath.hashCode().toUInt()}_$safeName"
    val tempFile = File(appContext.cacheDir, "zip_preview_$cacheKey")
    if (tempFile.exists()) return tempFile.absolutePath
    try {
        ZipInputStream(File(zipFilePath).inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.trimEnd('/') == internalPath) {
                    tempFile.outputStream().use { out -> zis.copyTo(out) }
                    return tempFile.absolutePath
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    } catch (e: Exception) {
        LogCat.e(e.toString())
    }
    return null
}

actual fun deleteFileOrDir(path: String): Boolean = File(path).deleteRecursively()

actual fun getNewPath(path: String): String = File(path).newPath()

actual fun getCanonicalPath(path: String): String =
    runCatching { File(path).canonicalPath }.getOrDefault(path)

actual fun copyFileOrDir(srcPath: String, destPath: String): Boolean {
    val src = File(srcPath)
    val dest = File(destPath)
    return if (src.isDirectory) src.copyRecursively(dest, true)
    else src.copyTo(dest, true).let { true }
}

actual fun moveFileOrDir(srcPath: String, destPath: String): Boolean {
    val src = File(srcPath)
    val dest = File(destPath)
    return runCatching {
        if (!dest.exists()) {
            src.toPath().moveTo(dest.toPath(), true)
        } else {
            src.toPath().moveTo(kotlin.io.path.Path(dest.newPath()), true)
        }
        true
    }.getOrElse {
        val target = if (!dest.exists()) dest else File(dest.newPath())
        val ok = if (src.isDirectory) src.copyRecursively(target, true)
        else src.copyTo(target, true).let { true }
        if (ok) src.deleteRecursively() else false
    }
}

actual fun zipFiles(sourcePaths: List<String>, targetPath: String): Boolean =
    com.ismartcoding.plain.helpers.ZipHelper.zip(sourcePaths, targetPath)

actual fun unzipFile(zipPath: String, destPath: String): Boolean =
    com.ismartcoding.plain.helpers.ZipHelper.unzip(File(zipPath), File(destPath))

actual fun statFile(path: String): DFile? {
    val file = File(path)
    if (!file.exists()) return null
    return DFile(
        name = file.name,
        path = file.absolutePath,
        permission = "",
        createdAt = null,
        updatedAt = Instant.fromEpochMilliseconds(file.lastModified()),
        size = if (file.isDirectory) 0L else file.length(),
        isDir = file.isDirectory,
        children = 0,
        mediaId = "",
    )
}
