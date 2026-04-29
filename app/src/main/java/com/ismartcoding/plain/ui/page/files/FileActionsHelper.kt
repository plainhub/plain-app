package com.ismartcoding.plain.ui.page.files

import android.content.Context
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.ZipHelper
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

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
    context: Context,
    filesVM: FilesViewModel,
    files: List<DFile>,
    onDone: () -> Unit,
) {
    if (files.isEmpty()) return
    scope.launch {
        DialogHelper.showLoading()
        val firstFile = files[0]
        val destFile = File(firstFile.path + ".zip")
        var destPath = destFile.path
        if (destFile.exists()) destPath = destFile.newPath()
        val success = withIO {
            try {
                ZipHelper.zip(files.map { it.path }, destPath)
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: e.toString())
                false
            }
        }
        if (success) withIO { filesVM.loadAsync(context) }
        DialogHelper.hideLoading()
        onDone()
    }
}
