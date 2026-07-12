package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.TextFieldDialog

@Composable
fun AddToHomeDialog(
    defaultLabel: String,
    onAddToHome: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    TextFieldDialog(
        title = stringResource(Res.string.add_to_home),
        value = defaultLabel,
        description = stringResource(Res.string.shortcut_label_hint),
        onDismissRequest = onDismiss,
        onConfirm = { label ->
            onAddToHome(label)
            onDismiss()
        },
    )
}
