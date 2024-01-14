package com.ismartcoding.plain.ui.base

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String = stringResource(id = R.string.ok),
    cancel: String = stringResource(id = R.string.cancel),
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(cancel)
            }
        }
    )
}
