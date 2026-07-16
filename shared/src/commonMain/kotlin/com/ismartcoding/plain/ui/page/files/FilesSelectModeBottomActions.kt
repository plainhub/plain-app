package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.platform.deleteFileOrDir
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getNewPath
import com.ismartcoding.plain.platform.renameAndScanFile
import com.ismartcoding.plain.platform.scanFiles
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.platform.unzipFile
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonCopy
import com.ismartcoding.plain.ui.base.IconTextSmallButtonCut
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonRename
import com.ismartcoding.plain.ui.base.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.base.IconTextSmallButtonUnzip
import com.ismartcoding.plain.ui.base.IconTextSmallButtonZip
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.exitSelectMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilesSelectModeBottomActions(
    filesVM: FilesViewModel,
    onShowPasteBar: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val selectedFiles = filesVM.itemsFlow.value.filter { file -> filesVM.selectedIds.contains(file.path) }
    val showRenameDialog = remember { mutableStateOf(false) }

    if (showRenameDialog.value && filesVM.selectedIds.size == 1) {
        val file = selectedFiles[0]
        FileRenameDialog(
            path = file.path,
            onDismiss = { showRenameDialog.value = false },
            onRename = { p, name -> renameAndScanFile(p, name) },
            onRenamed = { newPath ->
                val oldPath = file.path
                file.name = newPath.substringAfterLast("/")
                file.path = newPath
                if (file.isDir) {
                    filesVM.breadcrumbs.find { b -> b.path == oldPath }?.let { b ->
                        b.path = newPath
                        b.name = file.name
                    }
                }
                filesVM.exitSelectMode()
            },
        )
    }

    PBottomAppBar {
        BottomActionButtons {
            IconTextSmallButtonCut {
                performCutFiles(filesVM, selectedFiles, onShowPasteBar) { filesVM.exitSelectMode() }
            }
            IconTextSmallButtonCopy {
                performCopyFiles(filesVM, selectedFiles, onShowPasteBar) { filesVM.exitSelectMode() }
            }
            IconTextSmallButtonShare {
                shareFiles(filesVM.selectedIds.toList())
            }
            IconTextSmallButtonDelete {
                DialogHelper.confirmToDelete {
                    scope.launch {
                        val paths = filesVM.selectedIds.toSet()
                        DialogHelper.showLoading()
                        withIO {
                            FilePathValidator.requireAllSafe(paths.toList())
                            paths.forEach { deleteFileOrDir(it) }
                            scanFiles(paths.toTypedArray())
                            filesVM.loadAsync()
                        }
                        DialogHelper.hideLoading()
                        filesVM.exitSelectMode()
                    }
                }
            }
            IconTextSmallButtonZip {
                performZipFiles(scope, filesVM, selectedFiles) { filesVM.exitSelectMode() }
            }
            if (selectedFiles.size == 1 && selectedFiles[0].path.endsWith(".zip")) {
                IconTextSmallButtonUnzip {
                    scope.launch {
                        DialogHelper.showLoading()
                        val file = selectedFiles[0]
                        var destPath = file.path.removeSuffix(".zip")
                        if (fileExists(destPath)) {
                            destPath = getNewPath(destPath)
                        }
                        val success = withIO {
                            unzipFile(file.path, destPath)
                        }
                        if (success) {
                            withIO {
                                scanFiles(arrayOf(destPath))
                                filesVM.loadAsync()
                            }
                        } else {
                            DialogHelper.showMessage(Res.string.error)
                        }
                        DialogHelper.hideLoading()
                        filesVM.exitSelectMode()
                    }
                }
            }
            if (filesVM.selectedIds.size == 1) {
                IconTextSmallButtonRename {
                    showRenameDialog.value = true
                }
            }
        }
    }
}
