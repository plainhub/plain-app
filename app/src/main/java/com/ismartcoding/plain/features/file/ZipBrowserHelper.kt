package com.ismartcoding.plain.features.file

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.extensions.sorted
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.time.Instant

object ZipBrowserHelper {
    const val ZIP_SEPARATOR = "!zip!/"

    fun isZipPath(path: String): Boolean = path.contains(ZIP_SEPARATOR)

    fun getZipFilePath(path: String): String = path.substringBefore(ZIP_SEPARATOR)

    fun getInternalPath(path: String): String = path.substringAfter(ZIP_SEPARATOR, "")

    fun joinPath(zipFilePath: String, internalPath: String): String =
        "$zipFilePath$ZIP_SEPARATOR$internalPath"

    /** Returns the display name for the breadcrumb/title of a zip virtual path. */
    fun getDisplayName(zipVirtualPath: String): String {
        val internalPath = getInternalPath(zipVirtualPath)
        val trimmed = internalPath.trimEnd('/')
        return if (trimmed.isEmpty()) {
            getZipFilePath(zipVirtualPath).substringAfterLast("/")
        } else {
            trimmed.substringAfterLast("/")
        }
    }

    /**
     * Lists the direct children of [zipVirtualPath] (a virtual path inside a zip archive).
     * Directories are synthesized from paths of contained entries even when no explicit
     * directory entry exists in the zip.
     */
    fun listEntries(zipVirtualPath: String, sortBy: FileSortBy): List<DFile> {
        val zipFilePath = getZipFilePath(zipVirtualPath)
        val internalDir = getInternalPath(zipVirtualPath)
        // Normalize prefix: must end with "/" (or be empty for root)
        val prefix = when {
            internalDir.isEmpty() -> ""
            internalDir.endsWith("/") -> internalDir
            else -> "$internalDir/"
        }
        // Use LinkedHashMap to preserve first-seen order, then sort at the end
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
                                    // Direct file in this directory
                                    if (!entries.containsKey(relative)) {
                                        entries[relative] = DFile(
                                            name = relative,
                                            path = joinPath(zipFilePath, "$prefix$relative"),
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
                                    // Explicit directory entry (e.g. "docs/")
                                    val dirName = relative.dropLast(1)
                                    if (dirName.isNotEmpty() && !entries.containsKey(dirName)) {
                                        entries[dirName] = DFile(
                                            name = dirName,
                                            path = joinPath(zipFilePath, "$prefix$dirName/"),
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
                                    // File inside a subdirectory — synthesize the directory entry
                                    val dirName = relative.substring(0, slashIndex)
                                    if (!entries.containsKey(dirName)) {
                                        entries[dirName] = DFile(
                                            name = dirName,
                                            path = joinPath(zipFilePath, "$prefix$dirName/"),
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

    /**
     * Extracts a single zip entry to a file in [context.cacheDir] and returns it.
     * The result is cached by a key derived from the zip file path and internal entry path,
     * so repeated calls for the same entry skip re-extraction.
     * Returns null when the entry is not found or an I/O error occurs.
     */
    fun extractEntryToCache(context: android.content.Context, zipVirtualPath: String): File? {
        val zipFilePath = getZipFilePath(zipVirtualPath)
        val internalPath = getInternalPath(zipVirtualPath).trimEnd('/')
        if (internalPath.isEmpty()) return null
        val rawName = internalPath.substringAfterLast('/')
        val safeName = rawName.replace("[/\\\\:*?\"<>|]".toRegex(), "_").take(80)
        val cacheKey = "${zipFilePath.hashCode().toUInt()}_${internalPath.hashCode().toUInt()}_$safeName"
        val tempFile = File(context.cacheDir, "zip_preview_$cacheKey")
        if (tempFile.exists()) return tempFile
        try {
            ZipInputStream(File(zipFilePath).inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.trimEnd('/') == internalPath) {
                        tempFile.outputStream().use { out -> zis.copyTo(out) }
                        return tempFile
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
}
