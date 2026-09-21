package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.TextFieldDialog
import org.jetbrains.compose.resources.stringResource

/**
 * Text-input dialog for creating or renaming a playlist; confirm stays
 * disabled while the name is blank.
 */
@Composable
fun PlaylistNameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    TextFieldDialog(
        title = title,
        value = initial,
        placeholder = initial,
        confirmText = confirmText,
        onDismissRequest = onDismiss,
        onConfirm = { onConfirm(it.trim()) },
    )
}
