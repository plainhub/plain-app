package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import android.content.Context
import android.os.Environment
import coil3.imageLoader
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.helpers.DownloadHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.PathHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import java.io.File

internal suspend fun sharePreviewImage(context: Context, m: PreviewItem) {
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
            val r = withIO { DownloadHelper.downloadToTempAsync(m.path, tempFile) }
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

internal suspend fun savePreviewImage(context: Context, m: PreviewItem) {
    if (m.path.isUrl()) {
        DialogHelper.showLoading()
        val cachedPath = context.imageLoader.diskCache?.openSnapshot(m.path)?.data
        if (cachedPath != null) {
            val r = withIO { FileHelper.copyFileToPublicDir(cachedPath.toString(), Environment.DIRECTORY_PICTURES, newName = m.path.getFilenameFromPath()) }
            DialogHelper.hideLoading()
            if (r.isNotEmpty()) {
                DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r))
            } else {
                DialogHelper.showMessage(LocaleHelper.getString(R.string.image_save_to_failed))
            }
            return
        }
        val dir = PathHelper.getPlainPublicDir(Environment.DIRECTORY_PICTURES)
        val r = withIO { DownloadHelper.downloadAsync(m.path, dir.absolutePath) }
        DialogHelper.hideLoading()
        if (r.success) {
            DialogHelper.showConfirmDialog("", LocaleHelper.getStringF(R.string.image_save_to, "path", r.path))
        } else {
            DialogHelper.showMessage(r.message)
        }
    } else {
        val newName = (m.data as? DMessageFile)?.fileName?.takeIf { it.isNotEmpty() } ?: ""
        val r = withIO { FileHelper.copyFileToPublicDir(m.path, Environment.DIRECTORY_PICTURES, newName = newName) }
        if (r.isNotEmpty()) {
            DialogHelper.showMessage(LocaleHelper.getStringF(R.string.image_save_to, "path", r))
        } else {
            DialogHelper.showMessage(LocaleHelper.getString(R.string.image_save_to_failed))
        }
    }
}
