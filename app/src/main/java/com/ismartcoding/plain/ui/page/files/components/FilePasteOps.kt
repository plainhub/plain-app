package com.ismartcoding.plain.ui.page.files.components

import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.moveTo

internal suspend fun executeCutFiles(
    filesVM: FilesViewModel,
    onComplete: () -> Unit,
) {
    DialogHelper.showLoading()
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        filesVM.cutFiles.forEach {
            val srcFile = File(it.id)
            val dstDir = File(filesVM.selectedPath)
            val srcCanonical = srcFile.canonicalFile
            val dstCanonical = dstDir.canonicalFile

            if (srcCanonical.isDirectory &&
                (dstCanonical.path == srcCanonical.path ||
                        dstCanonical.path.startsWith(srcCanonical.path + "/"))
            ) {
                DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.cannot_move_folder_into_itself))
                return@forEach
            }

            val dstFile = File(dstCanonical, srcCanonical.name)
            try {
                if (!dstFile.exists()) {
                    srcCanonical.toPath().moveTo(dstFile.toPath(), true)
                } else {
                    srcCanonical.toPath().moveTo(Path(dstFile.newPath()), true)
                }
            } catch (e: Exception) {
                try {
                    val target = if (!dstFile.exists()) dstFile else File(dstFile.newPath())
                    if (srcCanonical.isDirectory) {
                        srcCanonical.copyRecursively(target, true)
                        srcCanonical.deleteRecursively()
                    } else {
                        srcCanonical.copyTo(target, true)
                        srcCanonical.delete()
                    }
                } catch (ex: Exception) {
                    DialogHelper.showErrorMessage(ex.message ?: LocaleHelper.getString(R.string.unknown_error))
                }
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
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        filesVM.copyFiles.forEach {
            val srcFile = File(it.id)
            val dstDir = File(filesVM.selectedPath)
            val srcCanonical = srcFile.canonicalFile
            val dstCanonical = dstDir.canonicalFile

            if (srcCanonical.isDirectory &&
                (dstCanonical.path == srcCanonical.path ||
                        dstCanonical.path.startsWith(srcCanonical.path + "/"))
            ) {
                DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.cannot_copy_folder_into_itself))
                return@forEach
            }

            val dstFile = File(dstCanonical, srcCanonical.name)
            try {
                if (!dstFile.exists()) {
                    srcCanonical.copyRecursively(dstFile, true)
                } else {
                    srcCanonical.copyRecursively(File(dstFile.newPath()), true)
                }
            } catch (e: Exception) {
                DialogHelper.showErrorMessage(e.message ?: LocaleHelper.getString(R.string.unknown_error))
            }
        }
        filesVM.copyFiles.clear()
    }
    DialogHelper.hideLoading()
    onComplete()
    filesVM.showPasteBar.value = false
}
