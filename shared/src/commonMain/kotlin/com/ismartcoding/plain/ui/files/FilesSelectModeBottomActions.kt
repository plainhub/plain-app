package com.ismartcoding.plain.ui.files


import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.system.shareFiles
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.components.IconTextSmallButtonCopy
import com.ismartcoding.plain.ui.components.IconTextSmallButtonCut
import com.ismartcoding.plain.ui.components.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.components.IconTextSmallButtonRename
import com.ismartcoding.plain.ui.components.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.components.IconTextSmallButtonShareLink
import com.ismartcoding.plain.ui.components.IconTextSmallButtonUnzip
import com.ismartcoding.plain.ui.components.IconTextSmallButtonZip
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.files.FilesViewModel
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.state.exitSelectMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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
            onRename = { p, name -> filesVM.source.files.rename(p, name) },
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
                val paths = filesVM.selectedIds.toList()
                scope.launchSafe { shareFiles(paths.map { filesVM.source.fileForOpen(it) }) }
            }
            IconTextSmallButtonShareLink {
                filesVM.sharePaths.clear()
                filesVM.sharePaths.addAll(filesVM.selectedIds)
                filesVM.showCreateShareDialog.value = true
            }
            IconTextSmallButtonDelete {
                scope.launch {
                    confirmActionAsync(
                        Res.string.delete,
                        Res.string.confirm_to_delete,
                        callback = {
                            scope.launch {
                                val paths = filesVM.selectedIds.toSet()
                                DialogHelper.showLoading()
                                try {
                                    withIO {
                                        filesVM.source.files.deleteMany(paths.toList())
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) { DialogHelper.showMessage(error) }
                                finally {
                                    DialogHelper.hideLoading()
                                    filesVM.loadAsync()
                                    filesVM.exitSelectMode()
                                }
                            }
                        },
                        danger = true
                    )
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
                        val destPath = file.path.removeSuffix(".zip")
                        val success = withIO {
                            filesVM.source.files.extractZipArchive(file.path, destPath)
                        }
                        if (success) {
                            withIO {
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
