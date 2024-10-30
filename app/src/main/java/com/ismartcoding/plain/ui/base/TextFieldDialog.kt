package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.ismartcoding.plain.R

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    title: String = "",
    icon: ImageVector? = null,
    value: String = "",
    placeholder: String = "",
    isPassword: Boolean = false,
    errorText: String = "",
    dismissText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    onValueChange: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onConfirm: (String) -> Unit = {},
    keyboardOptions: KeyboardOptions =
        KeyboardOptions(
            imeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
        ),
) {
    val focusManager = LocalFocusManager.current
    AlertDialog(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        icon = {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                )
            }
        },
        title = {
            Text(
                text = title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            ClipboardTextField(
                modifier = modifier,
                readOnly = readOnly,
                value = value,
                singleLine = singleLine,
                onValueChange = onValueChange,
                placeholder = placeholder,
                isPassword = isPassword,
                errorText = errorText,
                keyboardOptions = keyboardOptions,
                focusManager = focusManager,
                requestFocus = true,
                onConfirm = onConfirm,
            )
        },
        confirmButton = {
            Button(
                enabled = value.isNotBlank(),
                onClick = {
                    focusManager.clearFocus()
                    onConfirm(value)
                },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = dismissText)
            }
        },
    )
}
