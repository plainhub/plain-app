package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.createDownloadTempFile
import com.ismartcoding.plain.platform.ZipStreamEntry
import com.ismartcoding.plain.platform.createFileSink
import com.ismartcoding.plain.platform.createFileWriteHandle
import com.ismartcoding.plain.platform.streamZipToSink
import com.ismartcoding.plain.platform.getMimeTypeFromExtension
import com.ismartcoding.plain.platform.importDownloadedFile
import com.ismartcoding.plain.platform.saveTempFileToDownloads

/**
 * Download/sync engine for the shared folder browser. Every entry point
 * builds on [downloadToHandle], which streams a guest URL into a
 * [DownloadTempFileHandle] and cleans it up on failure.
 */
internal object SharedFolderTransfer {

    /** Stream [url] into [handle]; the handle is deleted and rethrown on failure. */
    private suspend fun downloadToHandle(
        url: String,
        handle: DownloadTempFileHandle,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        try {
            SharedLinkClient.downloadTo(url, { buffer, length -> handle.write(buffer, 0, length) }, onProgress)
            handle.close()
        } catch (e: Exception) {
            handle.delete()
            throw e
        }
    }

    /**
     * Save one entry into the app file store (the chat-files destination):
     * files via `/fs`, folders as a `/zip/dir` archive.
     */
    suspend fun saveEntryToAppFiles(
        link: SharedLink,
        urlToken: String,
        entry: SharedFileDto,
        onProgress: (fraction: Float) -> Unit = {},
    ): Boolean = try {
        withIO {
            val handle = createDownloadTempFile(entry.virtualPath.hashCode().toString())
            try {
                val url = if (entry.isDir) {
                    SharedLinkClient.zipDirUrl(link, urlToken, entry.virtualPath)
                } else {
                    SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath)
                }
                downloadToHandle(url, handle) { done, total ->
                    if (total > 0) onProgress(done.toFloat() / total)
                }
                val fileName = if (entry.isDir) "${entry.name}.zip" else entry.name
                val mime = if (entry.isDir) {
                    "application/zip"
                } else {
                    entry.mimeType.ifEmpty { getMimeTypeFromExtension(fileName.getFilenameExtension()) }
                }
                importDownloadedFile(handle, fileName, mime)
            } catch (e: Exception) {
                handle.delete()
                throw e
            }
        }
        true
    } catch (_: Exception) {
        false
    }

    /** Download a file entry into the public Downloads dir; returns the saved path, empty on failure. */
    suspend fun downloadFileToDownloads(link: SharedLink, urlToken: String, entry: SharedFileDto): String = try {
        withIO {
            val handle = createDownloadTempFile("dl_${entry.name}")
            val saved = try {
                downloadToHandle(SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath), handle)
                saveTempFileToDownloads(handle, entry.name)
            } catch (e: Exception) {
                handle.delete()
                ""
            }
            if (saved.isNullOrEmpty()) handle.delete()
            saved ?: ""
        }
    } catch (_: Exception) {
        ""
    }

    /** Stream a file entry to `$dir/<name>` (parent dirs auto-created); true on success. */
    suspend fun downloadFileToDir(link: SharedLink, urlToken: String, entry: SharedFileDto, dir: String): Boolean = try {
        withIO {
            val handle = createFileWriteHandle("${dir.trimEnd('/')}/${entry.name}")
            downloadToHandle(SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath), handle)
        }
        true
    } catch (_: Exception) {
        false
    }

    /** Download a directory entry as a zip into Downloads; returns the saved path, empty on failure. */
    suspend fun downloadZipToDownloads(link: SharedLink, urlToken: String, entry: SharedFileDto): String = try {
        withIO {
            val handle = createDownloadTempFile("zip_${entry.name}.zip")
            val saved = try {
                downloadToHandle(SharedLinkClient.zipDirUrl(link, urlToken, entry.virtualPath), handle)
                saveTempFileToDownloads(handle, "${entry.name}.zip")
            } catch (e: Exception) {
                handle.delete()
                ""
            }
            if (saved.isNullOrEmpty()) handle.delete()
            saved ?: ""
        }
    } catch (_: Exception) {
        ""
    }

    /**
     * Download an image entry into a temp file named with the original
     * extension (so the previewer detects the format). Caller owns the
     * returned handle and must delete it; null on failure.
     */
    suspend fun downloadPreviewFile(link: SharedLink, urlToken: String, entry: SharedFileDto): DownloadTempFileHandle? =
        try {
            val handle = createDownloadTempFile("preview_${entry.name}")
            withIO { downloadToHandle(SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath), handle) }
            handle
        } catch (_: Exception) {
            null
        }

    /**
     * Saves a multi-selection into [dir]: folders mirror their tree, files
     * stream to `<dir>/<name>`. Returns the number of entries saved.
     */
    suspend fun saveEntriesToDir(
        link: SharedLink,
        urlToken: String,
        entries: List<SharedFileDto>,
        dir: String,
    ): Int {
        var ok = 0
        entries.forEach { entry ->
            val done = if (entry.isDir) {
                runCatching {
                    withIO { syncDirectory(link, urlToken, entry.virtualPath, "", "${dir.trimEnd('/')}/${entry.name}") }
                }.isSuccess
            } else {
                downloadFileToDir(link, urlToken, entry, dir)
            }
            if (done) ok++
        }
        return ok
    }

    /**
     * Saves a multi-selection into the public Downloads dir: folders mirror
     * their tree under `PlainApp/<name>`, files land as `<name>`. Returns the
     * number of entries saved.
     */
    suspend fun saveEntriesToDownloads(
        link: SharedLink,
        urlToken: String,
        entries: List<SharedFileDto>,
        downloadsRoot: String,
    ): Int {
        var ok = 0
        entries.forEach { entry ->
            val done = if (downloadsRoot.isEmpty()) {
                false
            } else if (entry.isDir) {
                runCatching {
                    withIO { syncDirectory(link, urlToken, entry.virtualPath, "", "${downloadsRoot.trimEnd('/')}/PlainApp/${entry.name}") }
                }.isSuccess
            } else {
                downloadFileToDownloads(link, urlToken, entry).isNotEmpty()
            }
            if (done) ok++
        }
        return ok
    }

    /**
     * Zips the selected **file** entries into one archive at [destZipPath].
     * Files are downloaded to temp first, zipped, then cleaned up. Returns
     * true on success.
     */
    suspend fun zipEntriesTo(
        link: SharedLink,
        urlToken: String,
        entries: List<SharedFileDto>,
        destZipPath: String,
    ): Boolean {
        val tempFiles = mutableListOf<Pair<DownloadTempFileHandle, String>>() // handle to entry name
        try {
            entries.filter { !it.isDir }.forEach { entry ->
                val handle = createDownloadTempFile("zipitem_${entry.name}")
                withIO {
                    downloadToHandle(
                        SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath),
                        handle,
                    )
                }
                tempFiles.add(handle to entry.name)
            }
            if (tempFiles.isEmpty()) return false
            val sink = createFileSink(destZipPath)
            val ok = streamZipToSink(
                tempFiles.map { (handle, name) -> ZipStreamEntry(sourcePath = handle.filePath, entryName = name) },
                sink,
            )
            sink.close()
            return ok
        } finally {
            tempFiles.forEach { (handle, _) -> handle.delete() }
        }
    }

    /**
     * Recursively mirrors a shared directory into [targetDir], preserving the
     * remote tree structure — files stream to their relative paths, no zip.
     * Returns the number of files written.
     */
    suspend fun syncDirectory(
        link: SharedLink,
        urlToken: String,
        virtualPath: String,
        relative: String,
        targetDir: String,
    ): Int {
        var count = 0
        val info = SharedLinkClient.fetchSharedInfo(link, virtualPath.takeIf { it.isNotEmpty() })
        info.entries.sortedWith(compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() }).forEach { entry ->
            val rel = if (relative.isEmpty()) entry.name else "$relative/${entry.name}"
            if (entry.isDir) {
                count += syncDirectory(link, urlToken, entry.virtualPath, rel, targetDir)
            } else {
                val handle = createFileWriteHandle("$targetDir/$rel")
                downloadToHandle(SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath), handle)
                count++
            }
        }
        return count
    }
}
