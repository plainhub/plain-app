package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
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
        title = stringResource(id = R.string.rename),
        value = name.value,
        placeholder = oldName.value,
        onValueChange = {
            name.value = it
        },
        onDismissRequest = {
            onDismiss()
        },
        confirmText = stringResource(id = R.string.save),
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