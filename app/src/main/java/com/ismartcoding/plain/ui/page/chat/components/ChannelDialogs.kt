package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.TextFieldDialog

@Composable
fun CreateChannelDialog(
    visible: MutableState<Boolean>,
    onConfirm: (String) -> Unit,
) {
    if (visible.value) {
        TextFieldDialog(
            title = stringResource(Res.string.new_channel),
            onDismissRequest = {
                visible.value = false
            },
            onConfirm = { name ->
                onConfirm(name.trim())
            },
            validator = { it.trim().isNotBlank() },
        )
    }
}

@Composable
fun RenameChannelDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    TextFieldDialog(
        title = stringResource(Res.string.rename_channel),
        value = currentName,
        onDismissRequest = onDismiss,
        onConfirm = { name ->
            onConfirm(name.trim())
        },
        validator = { it.trim().isNotBlank() },
    )
}
