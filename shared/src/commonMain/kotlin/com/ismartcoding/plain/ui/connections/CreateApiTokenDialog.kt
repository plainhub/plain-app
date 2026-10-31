package com.ismartcoding.plain.ui.connections

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.components.TextFieldDialog
import org.jetbrains.compose.resources.stringResource

/** Text-input dialog for creating an API token; blank names stay disabled. */
@Composable
fun CreateApiTokenDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    TextFieldDialog(
        title = stringResource(Res.string.create_api_token),
        description = stringResource(Res.string.api_token_name),
        placeholder = stringResource(Res.string.api_token_name_hint),
        confirmText = stringResource(Res.string.create),
        onDismissRequest = onDismiss,
        onConfirm = { onCreate(it.trim()) },
    )
}
