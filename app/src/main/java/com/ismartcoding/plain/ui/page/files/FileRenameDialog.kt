package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import kotlinx.coroutines.launch

@Composable
internal fun FileRenameDialog(
    file: DFile,
    filesVM: FilesViewModel,
    showDialog: MutableState<Boolean>,
) {
    val scope = rememberCoroutineScope()
    val name = remember { mutableStateOf(file.name) }

    TextFieldDialog(
        title = stringResource(Res.string.rename),
        value = name.value,
        placeholder = file.name,
        onValueChange = { name.value = it },
        onDismissRequest = { showDialog.value = false },
        confirmText = stringResource(Res.string.save),
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
                showDialog.value = false
            }
        }
    )
}
