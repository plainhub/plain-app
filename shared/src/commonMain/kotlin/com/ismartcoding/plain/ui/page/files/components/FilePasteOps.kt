package com.ismartcoding.plain.ui.page.files.components

import com.ismartcoding.plain.features.file.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.CancellationException

internal suspend fun executeCutFiles(filesVM: FilesViewModel, onComplete: () -> Unit) =
    executePaste(filesVM,FileTaskType.MOVE,onComplete)

internal suspend fun executeCopyFiles(filesVM: FilesViewModel, onComplete: () -> Unit) =
    executePaste(filesVM,FileTaskType.COPY,onComplete)

private suspend fun executePaste(filesVM: FilesViewModel, type: FileTaskType, onComplete: () -> Unit) {
    val pending = if (type == FileTaskType.MOVE) filesVM.cutFiles else filesVM.copyFiles
    val files = pending.toList()
    if (files.isEmpty()) return
    DialogHelper.showLoading()
    try {
        val task = FileTaskHelper.execute(type,files.map { FileTaskOp(it.path,filesVM.selectedPath) })
        val completed = task.completedOps.map { it.src }.toSet()
        pending.removeAll { it.path in completed }
        if (task.status == FileTaskStatus.ERROR) DialogHelper.showErrorMessage(task.error)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { DialogHelper.showErrorMessage(error.message ?: error.toString()) }
    finally {
        DialogHelper.hideLoading()
        filesVM.showPasteBar.value = pending.isNotEmpty()
        onComplete()
    }
}
