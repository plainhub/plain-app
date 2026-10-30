package com.ismartcoding.plain.ui.files


import com.ismartcoding.plain.models.File as FileEntry

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.files.FilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun performCutFiles(
    filesVM: FilesViewModel,
    files: List<FileEntry>,
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
    files: List<FileEntry>,
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
    files: List<FileEntry>,
    onDone: () -> Unit,
) {
    if (files.isEmpty()) return
    scope.launch {
        DialogHelper.showLoading()
        val firstFile = files[0]
        val destPath = firstFile.path + ".zip"
        val success = withIO {
            try {
                filesVM.source.files.createZipArchive(files.map { it.path }, destPath)
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
