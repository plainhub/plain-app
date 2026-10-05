package com.ismartcoding.plain.ui.page.sharedfolder

import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.features.share.SharedLinkClient
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.createDownloadTempFile

/**
 * Download/sync primitives for the shared folder browser. Every entry point
 * builds on [downloadToHandle], which streams a guest URL into a
 * [DownloadTempFileHandle], reports byte progress and cleans up on failure.
 * Root owns batch orchestration; page previews use this OS temporary handle adapter.
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

}
