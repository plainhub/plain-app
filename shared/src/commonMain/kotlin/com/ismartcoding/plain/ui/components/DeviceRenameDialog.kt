package com.ismartcoding.plain.ui.components


import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.device_name
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.save

@Composable
fun DeviceRenameDialog(
    name: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val newName = remember {
        mutableStateOf(name)
    }
    TextFieldDialog(
        title = stringResource(Res.string.device_name),
        value = newName.value,
        placeholder = name,
        onValueChange = {
            newName.value = it
        },
        onDismissRequest = {
            onDismiss()
        },
        confirmText = stringResource(Res.string.save),
        onConfirm = {
            scope.launchSafe {
                onSave(newName.value)
                onDismiss()
            }
        },
    )
}
