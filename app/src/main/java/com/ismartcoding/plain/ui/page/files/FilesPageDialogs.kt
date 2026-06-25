package com.ismartcoding.plain.ui.page.files
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.preferences.FileSortByPreference
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.components.FileSortDialog
import com.ismartcoding.plain.ui.components.FolderKanbanDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun FilesPageDialogs(
    filesVM: FilesViewModel, context: Context, scope: CoroutineScope,
) {
    if (filesVM.showSortDialog.value) {
        FileSortDialog(filesVM.sortBy, onSelected = {
            scope.launch(Dispatchers.IO) { FileSortByPreference.putAsync(it); filesVM.sortBy.value = it; filesVM.loadAsync(context) }
        }, onDismiss = { filesVM.showSortDialog.value = false })
    }

    if (filesVM.showCreateFolderDialog.value) {
        val folderNameValue = remember { mutableStateOf("") }
        TextFieldDialog(
            title = stringResource(Res.string.create_folder), value = folderNameValue.value,
            placeholder = stringResource(Res.string.name),
            onValueChange = { folderNameValue.value = it },
            onDismissRequest = { filesVM.showCreateFolderDialog.value = false },
            onConfirm = { name ->
                scope.launch {
                    DialogHelper.showLoading()
                        val error = withIO { runCatching { FileSystemHelper.createDirectory(filesVM.selectedPath + "/" + name) }.exceptionOrNull() }
                        DialogHelper.hideLoading()
                        if (error != null) { DialogHelper.showMessage(error); return@launch }
                        filesVM.loadAsync(context)
                    filesVM.showCreateFolderDialog.value = false
                }
            }
        )
    }

    if (filesVM.showCreateFileDialog.value) {
        val fileNameValue = remember { mutableStateOf("") }
        TextFieldDialog(
            title = stringResource(Res.string.create_file), value = fileNameValue.value,
            placeholder = stringResource(Res.string.name),
            onValueChange = { fileNameValue.value = it },
            onDismissRequest = { filesVM.showCreateFileDialog.value = false },
            onConfirm = { name ->
                scope.launch {
                    DialogHelper.showLoading()
                        val error = withIO { runCatching { FileSystemHelper.createFile(filesVM.selectedPath + "/" + name) }.exceptionOrNull() }
                        DialogHelper.hideLoading()
                        if (error != null) { DialogHelper.showMessage(error); return@launch }
                        filesVM.loadAsync(context)
                    filesVM.showCreateFileDialog.value = false
                }
            }
        )
    }

    if (filesVM.showFolderKanbanDialog.value) {
        FolderKanbanDialog(filesVM = filesVM, onDismiss = { filesVM.showFolderKanbanDialog.value = false })
    }

    FileInfoBottomSheet(filesVM = filesVM)
}
