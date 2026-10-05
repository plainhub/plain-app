package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.ShareBatchType
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.ZipStreamEntry
import com.ismartcoding.plain.platform.createDownloadTempFile
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.platform.createFileWriteHandle
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
            runCatching { handle.close() }
            runCatching { handle.delete() }
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
            runCatching { handle.close() }
            runCatching { handle.delete() }
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
            runCatching { handle.close() }
            runCatching { handle.delete() }
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
        } catch (cancelled: CancellationException) {
            throw cancelled
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
        val plan = SharedLinkClient.plan(ShareBatchType.ZIP, link, entries, "", "${getDownloadsDirPath().trimEnd('/')}/PlainApp")
        onEnumerated(plan.totalFiles, plan.totalSize)

        val tempFiles = mutableListOf<Pair<DownloadTempFileHandle, String>>() // handle to entry name
        try {
            plan.targets.forEach { target ->
                val entry = target.entry
                val entryName = target.entryName
                onFileStart(entry)
                val handle = createDownloadTempFile("zipitem_${com.ismartcoding.plain.helpers.StringHelper.shortUUID()}")
                tempFiles.add(handle to entryName)
                withIO {
                    downloadToHandle(
                        SharedLinkClient.fileUrl(link, urlToken, entry.virtualPath),
                        handle,
                    ) { done, total ->
                        if (total > 0) onFileProgress(entry, done.toFloat() / total)
                    }
                }
            }
            if (tempFiles.isEmpty()) return false
            onPacking()
            val destination = createFileWriteHandle(destZipPath)
            try {
                withIO {
                    SharedLinkClient.packZip(tempFiles.map { (handle, name) -> ZipStreamEntry(handle.filePath, name) }) { bytes, length ->
                        destination.write(bytes, 0, length)
                    }
                }
                destination.close()
                return true
            } catch (error: Exception) {
                runCatching { destination.close() }
                destination.delete()
                throw error
            }
        } finally {
            tempFiles.forEach { (handle, _) -> handle.delete() }
        }
    }
}
