package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.ui.base.TextFieldDialog
import kotlinx.coroutines.launch

@Composable
fun DeviceRenameDialog(name: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val newName = remember {
        mutableStateOf(name)
    }
    TextFieldDialog(
        title = stringResource(id = R.string.device_name),
        value = newName.value,
        placeholder = name,
        onValueChange = {
            newName.value = it
        },
        onDismissRequest = {
            onDismiss()
        },
        confirmText = stringResource(id = R.string.save),
        onConfirm = {
            scope.launch {
                withIO {
                    DeviceNamePreference.putAsync(context, newName.value)
                }
                onDone(newName.value)
                onDismiss()
            }
        },
    )
}