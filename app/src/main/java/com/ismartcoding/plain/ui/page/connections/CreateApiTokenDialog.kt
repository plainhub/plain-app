package com.ismartcoding.plain.ui.page.connections

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.PTextButton

@Composable
fun CreateApiTokenDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_api_token)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.api_token_name)) },
                placeholder = { Text(stringResource(Res.string.api_token_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            PTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss)
        },
    )
}
