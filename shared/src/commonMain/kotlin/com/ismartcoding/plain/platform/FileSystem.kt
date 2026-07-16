package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.DStorageStatsItem
import com.ismartcoding.plain.features.file.FileSortBy

/**
 * Path to the primary internal storage (e.g. /storage/emulated/0).
 * Empty string on iOS.
 */
expect fun getInternalStoragePath(): String

/**
 * Display name for internal storage.
 */
expect fun getInternalStorageName(): String

/**
 * Path to the SD card, or empty string if none.
 */
expect fun getSDCardPath(): String

/**
 * Paths to USB storage devices.
 */
expect fun getUsbDiskPaths(): List<String>

/**
 * List files in [dir], optionally including hidden files, sorted by [sortBy].
 */
expect fun listFilesInDir(dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile>

/**
 * Search files recursively under [root] matching [query], optionally including hidden files.
 * Delegates query parsing (text, parent, file_size, show_hidden) to the platform implementation.
 */
expect suspend fun searchFilesInDir(query: String, root: String, sortBy: FileSortBy): List<DFile>

/**
 * Recursively search files under [dir] whose name contains [query] (case-insensitive).
 * When [showHidden] is false, hidden files are skipped. Results are sorted by [sortBy].
 */
expect fun searchFilesByName(query: String, dir: String, showHidden: Boolean, sortBy: FileSortBy): List<DFile>

/**
 * Returns the most recently modified files (up to 100) from MediaStore.
 */
expect suspend fun getRecentFiles(): List<DFile>

/**
 * Create a directory at [path] (including parents). Returns the resulting DFile.
 */
expect fun createDirectory(path: String): DFile

/**
 * Create an empty file at [path]. Returns the resulting DFile.
 */
expect fun createFile(path: String): DFile

/**
 * Notify the media scanner that files at [paths] were added/removed.
 * No-op on iOS.
 */
expect fun scanFiles(paths: Array<String>)

/**
 * Rename a file from [path] to [newName] and notify the media scanner.
 * Returns the new absolute path on success, null on failure.
 */
expect suspend fun renameAndScanFile(path: String, newName: String): String?

/**
 * Storage stats for internal storage.
 */
expect fun getInternalStorageStats(): DStorageStatsItem

/**
 * Storage stats for the SD card.
 */
expect fun getSDCardStorageStats(): DStorageStatsItem

/**
 * Storage stats for each USB storage device.
 */
expect fun getUSBStorageStats(): List<DStorageStatsItem>

/**
 * List entries inside a zip file at [zipVirtualPath], sorted by [sortBy].
 * Returns empty list if the path is not a zip or cannot be read.
 */
expect fun listZipEntries(zipVirtualPath: String, sortBy: FileSortBy): List<DFile>

/**
 * Extract a single zip entry to a temporary cache file and return its path.
 * Returns null if extraction fails.
 */
expect fun extractZipEntryToCache(zipVirtualPath: String): String?

/**
 * Delete a file or directory (recursively) at [path]. Returns true on success.
 */
expect fun deleteFileOrDir(path: String): Boolean

/**
 * Given an existing [path], returns a sibling path with a "(1)", "(2)"... suffix
 * inserted before the extension that does not yet exist. Used to avoid overwriting
 * an existing file when moving/copying/zipping.
 */
expect fun getNewPath(path: String): String

/**
 * Canonical absolute path of [path] (resolving symlinks). Falls back to the
 * input path on platforms without a canonical notion.
 */
expect fun getCanonicalPath(path: String): String

/**
 * Recursively copy a file or directory from [srcPath] to [destPath].
 * Returns true on success.
 */
expect fun copyFileOrDir(srcPath: String, destPath: String): Boolean

/**
 * Move a file or directory from [srcPath] to [destPath]. Tries an atomic rename
 * first and falls back to copy-then-delete. Returns true on success.
 */
expect fun moveFileOrDir(srcPath: String, destPath: String): Boolean

/**
 * Zip the given [sourcePaths] (files and/or directories) into a new archive at
 * [targetPath]. Returns true on success.
 */
expect fun zipFiles(sourcePaths: List<String>, targetPath: String): Boolean

/**
 * Unzip the archive at [zipPath] into the directory [destPath] (created if needed).
 * Returns true on success.
 */
expect fun unzipFile(zipPath: String, destPath: String): Boolean

/**
 * Returns file metadata (size, last-modified, is-directory) for [path], or null
 * if the file does not exist. Children count is 0 for non-directories.
 */
expect fun statFile(path: String): DFile?
