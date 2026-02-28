package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.TextFieldDialog

@Composable
fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    TextFieldDialog(
        title = stringResource(R.string.new_channel),
        placeholder = stringResource(R.string.channel_name_hint),
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
        title = stringResource(R.string.rename_channel),
        value = currentName,
        placeholder = stringResource(R.string.channel_name_hint),
        onDismissRequest = onDismiss,
        onConfirm = { name ->
            onConfirm(name.trim())
        },
        validator = { it.trim().isNotBlank() },
    )
}
