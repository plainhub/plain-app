package com.ismartcoding.plain.ui.page.files

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.ZipHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonCopy
import com.ismartcoding.plain.ui.base.IconTextSmallButtonCut
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonRename
import com.ismartcoding.plain.ui.base.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.base.IconTextSmallButtonUnzip
import com.ismartcoding.plain.ui.base.IconTextSmallButtonZip
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.exitSelectMode
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilesSelectModeBottomActions(
    filesVM: FilesViewModel,
    onShowPasteBar: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedFiles = filesVM.itemsFlow.value.filter { file -> filesVM.selectedIds.contains(file.path) }
    val showRenameDialog = remember { mutableStateOf(false) }

    if (showRenameDialog.value && filesVM.selectedIds.size == 1) {
        FileRenameDialog(
            file = selectedFiles[0],
            filesVM = filesVM,
            showDialog = showRenameDialog,
        )
    }

    PBottomAppBar {
        BottomActionButtons {
            IconTextSmallButtonCut {
                filesVM.cutFiles.clear()
                filesVM.cutFiles.addAll(selectedFiles.map { it.copy() })
                filesVM.copyFiles.clear()
                onShowPasteBar(true)
                filesVM.exitSelectMode()
            }
            IconTextSmallButtonCopy {
                filesVM.copyFiles.clear()
                filesVM.copyFiles.addAll(selectedFiles.map { it.copy() })
                filesVM.cutFiles.clear()
                onShowPasteBar(true)
                filesVM.exitSelectMode()
            }
            IconTextSmallButtonShare {
                ShareHelper.sharePaths(context, filesVM.selectedIds.toSet())
            }
            IconTextSmallButtonDelete {
                DialogHelper.confirmToDelete {
                    scope.launch {
                        val paths = filesVM.selectedIds.toSet()
                        DialogHelper.showLoading()
                        withIO {
                            FilePathValidator.requireAllSafe(paths.toList())
                            paths.forEach { File(it).deleteRecursively() }
                            MainApp.instance.scanFileByConnection(paths.toTypedArray())
                            filesVM.loadAsync(context)
                        }
                        DialogHelper.hideLoading()
                        filesVM.exitSelectMode()
                    }
                }
            }
            IconTextSmallButtonZip {
                if (selectedFiles.isNotEmpty()) {
                    scope.launch {
                        DialogHelper.showLoading()
                        val file = selectedFiles[0]
                        val destFile = File(file.path + ".zip")
                        var destPath = destFile.path
                        if (destFile.exists()) {
                            destPath = destFile.newPath()
                        }
                        val success = withIO {
                            try {
                                ZipHelper.zip(selectedFiles.map { it.path }, destPath)
                            } catch (e: Exception) {
                                DialogHelper.showErrorMessage(e.message ?: e.toString())
                                false
                            }
                        }
                        if (success) {
                            withIO { filesVM.loadAsync(context) }
                        }
                        DialogHelper.hideLoading()
                        filesVM.exitSelectMode()
                    }
                }
            }
            if (selectedFiles.size == 1 && selectedFiles[0].path.endsWith(".zip")) {
                IconTextSmallButtonUnzip {
                    scope.launch {
                        DialogHelper.showLoading()
                        val file = selectedFiles[0]
                        val destFile = File(file.path.removeSuffix(".zip"))
                        var destPath = destFile.path
                        if (destFile.exists()) {
                            destPath = destFile.newPath()
                        }
                        val success = withIO {
                            ZipHelper.unzip(File(file.path), File(destPath))
                        }
                        if (success) {
                            withIO {
                                MainApp.instance.scanFileByConnection(destPath)
                                filesVM.loadAsync(context)
                            }
                        } else {
                            DialogHelper.showMessage(R.string.error)
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