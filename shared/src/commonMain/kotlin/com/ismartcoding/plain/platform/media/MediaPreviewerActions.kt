package com.ismartcoding.plain.platform.media

import com.ismartcoding.plain.features.files.FilesClient

import com.ismartcoding.plain.platform.files.deleteFileAt
import com.ismartcoding.plain.platform.files.saveFileToDownloads
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.rust.graphql.ContentSource

import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.image_save_to
import com.ismartcoding.plain.i18n.image_save_to_failed
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.files.RemoteFiles
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.CancellationException

fun canSavePreviewMedia(source: ContentSource): Boolean = Capability.FILE_EXPORT in source.capabilities

suspend fun savePreviewMedia(m: PreviewItem, dir: String?) {
    check(canSavePreviewMedia(m.source)) { "Unsupported capability FILE_EXPORT" }
    val name = (m.data as? MessageFile)?.fileName?.takeIf { it.isNotEmpty() }
        ?: m.path.getFilenameFromPath()
    if (m.source is ContentSource.Remote) {
        val directory = dir ?: m.source.info.downloadsDir
        val saved = m.source.files.copyToDirectory(m.path, directory, name)
        showPreviewSaveResult(saved)
        return
    }
    if (!m.path.isUrl()) {
        savePreviewFile(m.path, name, dir)
        return
    }
    if (useCachedPreviewFile(m.path) { savePreviewFile(it, name, dir) }) return
    DialogHelper.showLoading()
    var temporary: String? = null
    try {
        val path = RemoteFiles.download(m.path, name)
        temporary = path
        savePreviewFile(path, name, dir)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DialogHelper.showMessage(error.toString())
    } finally {
        DialogHelper.hideLoading()
        temporary?.let { withIO { deleteFileAt(it) } }
    }
}

private suspend fun savePreviewFile(path: String, name: String, directory: String?) {
    val saved = if (directory == null) saveFileToDownloads(path, name) else FilesClient.copyToDirectory(path, directory, name)
    showPreviewSaveResult(saved)
}

private suspend fun showPreviewSaveResult(saved: String) {
    if (saved.isNotEmpty()) {
        DialogHelper.showMessage(LocaleHelper.getStringFAsync(Res.string.image_save_to, saved))
    } else {
        DialogHelper.showMessage(LocaleHelper.getStringAsync(Res.string.image_save_to_failed))
    }
}

internal expect suspend fun useCachedPreviewFile(path: String, consume: suspend (String) -> Unit): Boolean
