package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.ZipStreamEntry
import com.ismartcoding.plain.platform.createDownloadTempFile
import com.ismartcoding.plain.platform.createFileSink
import com.ismartcoding.plain.platform.createFileWriteHandle
import com.ismartcoding.plain.platform.streamZipToSink
import com.ismartcoding.plain.platform.saveTempFileToDownloads

/**
 * Download/sync primitives for the shared folder browser. Every entry point
 * builds on [downloadToHandle], which streams a guest URL into a
 * [DownloadTempFileHandle], reports byte progress and cleans up on failure.
 * Batch orchestration lives in SharedFolderDownloadEngine; page-level preview
 * downloads also use the primitives directly.
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

    /** Download a file entry into the public Downloads dir; returns the saved path, empty if the platform declined. Throws on transfer failure. */
    suspend fun downloadFileToDownloads(
        link: SharedLink,
        urlToken: String,
        entry: SharedFileDto,
        onProgress: (fraction: Float) -> Unit = {},
    ): String {
        val handle = createDownloadTempFile("dl_${entry.name}")
        val saved = try {
            withIO {
                downloadToHandle(SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath), handle) { done, total ->
                    if (total > 0) onProgress(done.toFloat() / total)
                }
            }
            saveTempFileToDownloads(handle, entry.name)
        } catch (e: Exception) {
            handle.delete()
            throw e
        }
        if (saved.isNullOrEmpty()) handle.delete()
        return saved ?: ""
    }

    /** Stream a file entry to `$dir/<name>` (parent dirs auto-created); throws on failure. */
    suspend fun downloadFileToDir(
        link: SharedLink,
        urlToken: String,
        entry: SharedFileDto,
        dir: String,
        onProgress: (fraction: Float) -> Unit = {},
    ) {
        val handle = createFileWriteHandle("${dir.trimEnd('/')}/${entry.name}")
        try {
            withIO {
                downloadToHandle(SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath), handle) { done, total ->
                    if (total > 0) onProgress(done.toFloat() / total)
                }
            }
        } catch (e: Exception) {
            handle.delete()
            throw e
        }
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
     * Zips entries into one archive at [destZipPath], reporting walk results
     * and per-file progress so callers can drive a batch progress bar.
     * Files are downloaded to temp first, zipped, then cleaned up.
     */
    suspend fun zipEntriesTo(
        link: SharedLink,
        urlToken: String,
        entries: List<SharedFileDto>,
        destZipPath: String,
        onEnumerated: (fileCount: Int, totalBytes: Long) -> Unit = { _, _ -> },
        onFileStart: (entry: SharedFileDto) -> Unit = {},
        onFileProgress: (entry: SharedFileDto, fraction: Float) -> Unit = { _, _ -> },
        onPacking: () -> Unit = {},
    ): Boolean {
        // Expand selected directories into their file trees so one zip holds
        // everything: files keep their name, dir files nest under dirName/.
        val items = mutableListOf<Pair<SharedFileDto, String>>() // entry to entry name
        suspend fun walk(entry: SharedFileDto, base: String) {
            if (!entry.isDir) {
                items.add(entry to base)
                return
            }
            val info = SharedLinkClient.fetchSharedInfo(link, entry.virtualPath.takeIf { it.isNotEmpty() })
            info.entries.forEach { child ->
                walk(child, if (base.isEmpty()) child.name else "$base/${child.name}")
            }
        }
        entries.forEach { walk(it, it.name) }
        onEnumerated(items.size, items.sumOf { it.first.size })

        val tempFiles = mutableListOf<Pair<DownloadTempFileHandle, String>>() // handle to entry name
        try {
            items.forEach { (entry, entryName) ->
                onFileStart(entry)
                val handle = createDownloadTempFile("zipitem_${entryName}")
                withIO {
                    downloadToHandle(
                        SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath),
                        handle,
                    ) { done, total ->
                        if (total > 0) onFileProgress(entry, done.toFloat() / total)
                    }
                }
                tempFiles.add(handle to entryName)
            }
            if (tempFiles.isEmpty()) return false
            onPacking()
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
}
