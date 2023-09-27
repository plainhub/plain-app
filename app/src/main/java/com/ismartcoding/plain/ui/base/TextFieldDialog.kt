package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import com.ismartcoding.plain.R

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    visible: Boolean = false,
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

    PAlertDialog(
        modifier = modifier,
        visible = visible,
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
            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                onConfirm = onConfirm,
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = {
                    focusManager.clearFocus()
                    onConfirm(value)
                },
            ) {
                Text(
                    text = confirmText,
                    color =
                        if (value.isNotBlank()) {
                            Color.Unspecified
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                        },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = dismissText)
            }
        },
    )
}
