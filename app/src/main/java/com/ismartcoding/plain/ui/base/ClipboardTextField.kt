package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun ClipboardTextField(
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    value: String = "",
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    isPassword: Boolean = false,
    errorText: String = "",
    keyboardOptions: KeyboardOptions =
        KeyboardOptions(
            imeAction = ImeAction.Done,
        ),
    focusManager: FocusManager? = null,
    requestFocus: Boolean = false,
    onConfirm: (String) -> Unit = {},
) {
    val imeAction = keyboardOptions.imeAction
    Column(modifier = modifier) {
        PTextField(
            readOnly = readOnly,
            value = value,
            singleLine = singleLine,
            onValueChange = onValueChange,
            placeholder = placeholder,
            isPassword = isPassword,
            errorMessage = errorText,
            requestFocus = requestFocus,
            keyboardActions =
            KeyboardActions(
                onDone =
                if (imeAction == ImeAction.Done) {
                    action(focusManager, onConfirm, value)
                } else {
                    null
                },
                onGo =
                if (imeAction == ImeAction.Go) {
                    action(focusManager, onConfirm, value)
                } else {
                    null
                },
                onNext =
                if (imeAction == ImeAction.Next) {
                    action(focusManager, onConfirm, value)
                } else {
                    null
                },
                onPrevious =
                if (imeAction == ImeAction.Previous) {
                    action(focusManager, onConfirm, value)
                } else {
                    null
                },
                onSearch =
                if (imeAction == ImeAction.Search) {
                    action(focusManager, onConfirm, value)
                } else {
                    null
                },
                onSend =
                if (imeAction == ImeAction.Send) {
                    action(focusManager, onConfirm, value)
                } else {
                    null
                },
            ),
            keyboardOptions = keyboardOptions,
        )
        if (errorText.isNotEmpty()) {
            SelectionContainer {
                Text(
                    modifier =
                    Modifier
                        .padding(horizontal = 16.dp),
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun action(
    focusManager: FocusManager?,
    onConfirm: (String) -> Unit,
    value: String,
): KeyboardActionScope.() -> Unit =
    {
        focusManager?.clearFocus()
        onConfirm(value)
    }
