package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getNewPath
import com.ismartcoding.plain.platform.zipFiles
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun performCutFiles(
    filesVM: FilesViewModel,
    files: List<DFile>,
    onShowPasteBar: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    filesVM.cutFiles.clear()
    filesVM.cutFiles.addAll(files.map { it.copy() })
    filesVM.copyFiles.clear()
    onShowPasteBar(true)
    onDone()
}

internal fun performCopyFiles(
    filesVM: FilesViewModel,
    files: List<DFile>,
    onShowPasteBar: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    filesVM.copyFiles.clear()
    filesVM.copyFiles.addAll(files.map { it.copy() })
    filesVM.cutFiles.clear()
    onShowPasteBar(true)
    onDone()
}

internal fun performZipFiles(
    scope: CoroutineScope,
    filesVM: FilesViewModel,
    files: List<DFile>,
    onDone: () -> Unit,
) {
    if (files.isEmpty()) return
    scope.launch {
        DialogHelper.showLoading()
        val firstFile = files[0]
        var destPath = firstFile.path + ".zip"
        if (fileExists(destPath)) destPath = getNewPath(destPath)
        val success = withIO {
            try {
                zipFiles(files.map { it.path }, destPath)
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: e.toString())
                false
            }
        }
        if (success) filesVM.loadAsync()
        DialogHelper.hideLoading()
        onDone()
    }
}
