package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.base.TextFieldDialog
import kotlinx.coroutines.launch

@Composable
fun FileRenameDialog(path: String, onDismiss: () -> Unit, onDoneAsync: suspend (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val oldName = remember {
        mutableStateOf(path.substringAfterLast("/"))
    }
    val name = remember {
        mutableStateOf(oldName.value)
    }
    TextFieldDialog(
        title = stringResource(Res.string.rename),
        value = name.value,
        placeholder = oldName.value,
        onValueChange = {
            name.value = it
        },
        onDismissRequest = {
            onDismiss()
        },
        confirmText = stringResource(Res.string.save),
        onConfirm = {
            scope.launch {
                withIO {
                    val newFile = FileHelper.rename(path, name.value)
                    MainApp.instance.scanFileByConnection(path)
                    if (newFile != null) {
                        MainApp.instance.scanFileByConnection(newFile.absolutePath)
                        onDoneAsync(newFile.absolutePath)
                    }
                }
                onDismiss()
            }
        },
    )
}