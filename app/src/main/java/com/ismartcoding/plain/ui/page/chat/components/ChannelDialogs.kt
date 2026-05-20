package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.TextFieldDialog

@Composable
fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    TextFieldDialog(
        title = stringResource(Res.string.new_channel),
        placeholder = stringResource(Res.string.channel_name_hint),
        onDismissRequest = onDismiss,
        onConfirm = { name ->
            onConfirm(name.trim())
        },
        validator = { it.trim().isNotBlank() },
    )
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
        placeholder = stringResource(Res.string.channel_name_hint),
        onDismissRequest = onDismiss,
        onConfirm = { name ->
            onConfirm(name.trim())
        },
        validator = { it.trim().isNotBlank() },
    )
}
