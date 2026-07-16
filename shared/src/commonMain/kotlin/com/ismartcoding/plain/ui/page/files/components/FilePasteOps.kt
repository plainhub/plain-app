package com.ismartcoding.plain.ui.page.files.components

import com.ismartcoding.plain.i18n.*

import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.copyFileOrDir
import com.ismartcoding.plain.platform.getCanonicalPath
import com.ismartcoding.plain.platform.getNewPath
import com.ismartcoding.plain.platform.moveFileOrDir
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.withContext

internal suspend fun executeCutFiles(
    filesVM: FilesViewModel,
    onComplete: () -> Unit,
) {
    DialogHelper.showLoading()
    withContext(kotlinx.coroutines.Dispatchers.Default) {
        filesVM.cutFiles.forEach {
            val srcCanonical = getCanonicalPath(it.id)
            val dstCanonical = getCanonicalPath(filesVM.selectedPath)

            val srcIsDir = runCatching { getCanonicalPath(it.id).endsWith("/") }.getOrDefault(false)
            // Note: getCanonicalPath returns a path string; we detect directory-ness by trailing slash
            // from the source DFile.isDir flag instead.
            val srcIsDirFlag = it.isDir
            if (srcIsDirFlag &&
                (dstCanonical == srcCanonical ||
                        dstCanonical.startsWith(srcCanonical + "/"))
            ) {
                DialogHelper.showErrorMessage(LocaleHelper.getStringAsync(Res.string.cannot_move_folder_into_itself))
                return@forEach
            }

            val srcName = srcCanonical.substringAfterLast("/")
            val dstPath = "$dstCanonical/$srcName"
            try {
                val success = if (!com.ismartcoding.plain.platform.fileExists(dstPath)) {
                    moveFileOrDir(srcCanonical, dstPath)
                } else {
                    moveFileOrDir(srcCanonical, getNewPath(dstPath))
                }
                if (!success) {
                    // Fallback already handled inside moveFileOrDir; nothing else to do.
                }
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: LocaleHelper.getStringAsync(Res.string.unknown_error))
            }
        }
        filesVM.cutFiles.clear()
    }
    DialogHelper.hideLoading()
    onComplete()
    filesVM.showPasteBar.value = false
}

internal suspend fun executeCopyFiles(
    filesVM: FilesViewModel,
    onComplete: () -> Unit,
) {
    DialogHelper.showLoading()
    withContext(kotlinx.coroutines.Dispatchers.Default) {
        filesVM.copyFiles.forEach {
            val srcCanonical = getCanonicalPath(it.id)
            val dstCanonical = getCanonicalPath(filesVM.selectedPath)

            if (it.isDir &&
                (dstCanonical == srcCanonical ||
                        dstCanonical.startsWith(srcCanonical + "/"))
            ) {
                DialogHelper.showErrorMessage(LocaleHelper.getStringAsync(Res.string.cannot_copy_folder_into_itself))
                return@forEach
            }

            val srcName = srcCanonical.substringAfterLast("/")
            val dstPath = "$dstCanonical/$srcName"
            try {
                if (!com.ismartcoding.plain.platform.fileExists(dstPath)) {
                    copyFileOrDir(srcCanonical, dstPath)
                } else {
                    copyFileOrDir(srcCanonical, getNewPath(dstPath))
                }
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: LocaleHelper.getStringAsync(Res.string.unknown_error))
            }
        }
        filesVM.copyFiles.clear()
    }
    DialogHelper.hideLoading()
    onComplete()
    filesVM.showPasteBar.value = false
}
