package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DownloadResult
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.toNSData
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import platform.Foundation.writeToFile

actual val canSavePreviewMedia: Boolean = false

actual suspend fun sharePreviewMedia(m: PreviewItem) {
    val path = m.path
    if (path.isUrl()) {
        DialogHelper.showLoading()
        val tempFile = withIO { downloadUrlToTemp(path) }
        DialogHelper.hideLoading()
        if (tempFile.success) {
            shareFile(tempFile.path)
        } else {
            DialogHelper.showMessage(tempFile.message)
        }
    } else {
        shareFiles(listOf(path))
    }
}

actual suspend fun savePreviewMedia(m: PreviewItem) {
    // iOS has no public-directory save path; no-op (button is hidden via canSavePreviewMedia).
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private suspend fun downloadUrlToTemp(url: String): DownloadResult {
    val tempDir = platform.Foundation.NSTemporaryDirectory()
    val ext = url.getFilenameExtension().ifEmpty { "bin" }
    val tempPath = "$tempDir/preview_share_${TimeHelper.nowMillis()}.$ext"
    return try {
        val client = createBrowserHttpClient()
        val r = client.get(url)
        r.use {
            if (it.isOk()) {
                val bytes = it.bodyAsBytes()
                bytes.toNSData().writeToFile(tempPath, atomically = true)
                DownloadResult(tempPath, true)
            } else {
                DownloadResult("", false, "${it.status.value} ${it.status.description}")
            }
        }
    } catch (e: Exception) {
        DownloadResult("", false, e.message ?: "download failed")
    }
}
