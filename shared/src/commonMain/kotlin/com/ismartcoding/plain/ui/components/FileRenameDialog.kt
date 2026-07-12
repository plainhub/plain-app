package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.ui.base.TextFieldDialog
import kotlinx.coroutines.launch

@Composable
fun FileRenameDialog(
    path: String,
    onDismiss: () -> Unit,
    onRename: suspend (path: String, newName: String) -> String?,
    onRenamed: suspend (newPath: String) -> Unit,
) {
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
                val newPath = withIO { onRename(path, name.value) }
                if (newPath != null) {
                    onRenamed(newPath)
                }
                onDismiss()
            }
        },
    )
}
