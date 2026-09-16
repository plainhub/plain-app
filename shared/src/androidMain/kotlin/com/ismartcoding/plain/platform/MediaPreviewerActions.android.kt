package com.ismartcoding.plain.platform

import coil3.imageLoader
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.helpers.DownloadHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.image_save_to
import com.ismartcoding.plain.i18n.image_save_to_failed
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import java.io.File

actual val canSavePreviewMedia: Boolean = true

actual suspend fun sharePreviewMedia(m: PreviewItem) {
    val context = appContextValue ?: return
    if (m.mediaId.isNotEmpty()) {
        ShareHelper.shareUris(context, listOf(ImageMediaStoreHelper.getItemUri(m.mediaId)))
    } else if (m.path.isUrl()) {
        val cachedPath = context.imageLoader.diskCache?.openSnapshot(m.path)?.data
        val tempFile = File.createTempFile("imagePreviewShare", "." + m.path.getFilenameExtension(), File(context.cacheDir, "/image_cache"))
        if (cachedPath != null) {
            cachedPath.toFile().copyTo(tempFile, true)
            ShareHelper.shareFile(context, tempFile, m.getMimeType().ifEmpty { "image/*" })
        } else {
            DialogHelper.showLoading()
            val r = DownloadHelper.downloadToTempAsync(m.path, tempFile)
            DialogHelper.hideLoading()
            if (r.success) {
                ShareHelper.shareFile(context, File(r.path), m.getMimeType().ifEmpty { "image/*" })
            } else {
                DialogHelper.showMessage(r.message)
            }
        }
    } else {
        ShareHelper.shareFile(context, File(m.path), m.getMimeType().ifEmpty { "image/*" })
    }
}

actual suspend fun savePreviewMedia(m: PreviewItem, dir: String?) {
    val fileName = (m.data as? DMessageFile)?.fileName?.takeIf { it.isNotEmpty() }
        ?: m.path.getFilenameFromPath()
    if (m.path.isUrl()) {
        val context = appContextValue ?: return
        val cachedPath = context.imageLoader.diskCache?.openSnapshot(m.path)?.data
        if (cachedPath != null) {
            savePreviewMediaFile(cachedPath.toString(), fileName, dir)
            return
        }
        DialogHelper.showLoading()
        val tempFile = File.createTempFile("imagePreviewSave", "." + m.path.getFilenameExtension(), File(context.cacheDir, "/image_cache"))
        val r = DownloadHelper.downloadToTempAsync(m.path, tempFile)
        DialogHelper.hideLoading()
        if (!r.success) {
            DialogHelper.showMessage(r.message)
            return
        }
        savePreviewMediaFile(r.path, fileName, dir)
        withIO { tempFile.delete() }
    } else {
        savePreviewMediaFile(m.path, fileName, dir)
    }
}

private suspend fun savePreviewMediaFile(srcPath: String, fileName: String, dir: String?) {
    val savedPath = withIO {
        if (dir == null) FileHelper.copyFileToDownloads(srcPath, fileName)
        else copyFileToDir(srcPath, dir, fileName)
    }
    if (savedPath.isNotEmpty()) {
        DialogHelper.showMessage(LocaleHelper.getStringFAsync(Res.string.image_save_to, savedPath))
    } else {
        DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.image_save_to_failed))
    }
}
