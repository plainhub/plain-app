package com.ismartcoding.plain.ui.page.files

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.ZipHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.events.ActionEvent
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonCopy
import com.ismartcoding.plain.ui.base.IconTextSmallButtonCut
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonRename
import com.ismartcoding.plain.ui.base.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.base.IconTextSmallButtonUnzip
import com.ismartcoding.plain.ui.base.IconTextSmallButtonZip
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.exitSelectMode
import kotlinx.coroutines.launch
import org.zeroturnaround.zip.ZipUtil
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
        val file = selectedFiles[0]
        val name = remember { mutableStateOf(file.name) }
        
        TextFieldDialog(
            title = stringResource(id = R.string.rename),
            value = name.value,
            placeholder = file.name,
            onValueChange = { name.value = it },
            onDismissRequest = { showRenameDialog.value = false },
            confirmText = stringResource(id = R.string.save),
            onConfirm = { newName ->
                scope.launch {
                    DialogHelper.showLoading()
                    val oldName = file.name
                    val oldPath = file.path
                    val dstFile = withIO { FileHelper.rename(file.path, newName) }
                    if (dstFile != null) {
                        withIO {
                            MainApp.instance.scanFileByConnection(file.path)
                            MainApp.instance.scanFileByConnection(dstFile)
                        }
                    }
                    
                    file.name = newName
                    file.path = file.path.replace("/$oldName", "/$newName")
                    if (file.isDir) {
                        filesVM.breadcrumbs.find { b -> b.path == oldPath }?.let { b ->
                            b.path = file.path
                            b.name = newName
                        }
                    }
                    
                    DialogHelper.hideLoading()
                    filesVM.exitSelectMode()
                    showRenameDialog.value = false
                }
            }
        )
    }
    
    PBottomAppBar {
        BottomActionButtons {
            IconTextSmallButtonCut {
                filesVM.cutFiles.clear()
                filesVM.cutFiles.addAll(selectedFiles.map { file ->
                    file.copy()
                })
                filesVM.copyFiles.clear()
                onShowPasteBar(true)
                filesVM.exitSelectMode()
            }
            IconTextSmallButtonCopy {
                filesVM.copyFiles.clear()
                filesVM.copyFiles.addAll(selectedFiles.map { file ->
                    file.copy()
                })
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
                            paths.forEach {
                                File(it).deleteRecursively()
                            }
                            MainApp.instance.scanFileByConnection(paths.toTypedArray())
                        }
                        DialogHelper.hideLoading()
                        sendEvent(ActionEvent(ActionSourceType.FILE, ActionType.DELETED, paths))
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
                        withIO {
                            ZipHelper.zip(selectedFiles.map { it.path }, destPath)
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
                        withIO {
                            ZipUtil.unpack(File(file.path), File(destPath))
                            MainApp.instance.scanFileByConnection(destPath)
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