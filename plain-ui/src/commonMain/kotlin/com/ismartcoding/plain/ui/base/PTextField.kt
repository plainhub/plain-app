package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.ismartcoding.plain.ui.platform.rememberClipboardReader
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.clear as ui_string_clear
import com.ismartcoding.plain.ui.resources.content_paste as ui_drawable_content_paste
import com.ismartcoding.plain.ui.resources.eye as ui_drawable_eye
import com.ismartcoding.plain.ui.resources.eye_off as ui_drawable_eye_off
import com.ismartcoding.plain.ui.resources.hide_password as ui_string_hide_password
import com.ismartcoding.plain.ui.resources.paste as ui_string_paste
import com.ismartcoding.plain.ui.resources.show_password as ui_string_show_password
import com.ismartcoding.plain.ui.resources.x as ui_drawable_x
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTextField(
    readOnly: Boolean,
    value: String,
    label: String = "",
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isPassword: Boolean = false,
    placeholder: String = "",
    errorMessage: String = "",
    requestFocus: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val readClipboard = rememberClipboardReader()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var showPassword by remember(isPassword) { mutableStateOf(false) }

    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo, requestFocus, enabled) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { isWindowFocused ->
            if (isWindowFocused && requestFocus && enabled) {
                focusRequester.requestFocus()
            }
        }
    }

    TextField(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        enabled = enabled,
        readOnly = readOnly,
        value = value,
        label =
            if (label.isEmpty()) {
                null
            } else {
                { Text(label) }
            },
        onValueChange = {
            if (!readOnly) onValueChange(it)
        },
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else visualTransformation,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        isError = errorMessage.isNotEmpty(),
        supportingText = if (errorMessage.isEmpty()) null else { { Text(errorMessage) } },
        singleLine = singleLine,
        trailingIcon = {
            if (value.isNotEmpty() && (isPassword || !readOnly)) {
                IconButton(enabled = enabled, onClick = {
                    if (isPassword) {
                        showPassword = !showPassword
                    } else if (!readOnly) {
                        onValueChange("")
                    }
                }) {
                    Icon(
                        painter =
                            painterResource(
                                if (isPassword) {
                                    if (showPassword) {
                                        UiRes.drawable.ui_drawable_eye
                                    } else {
                                        UiRes.drawable.ui_drawable_eye_off
                                    }
                                } else {
                                    UiRes.drawable.ui_drawable_x
                                }
                            ),
                        contentDescription = if (isPassword) stringResource(if (showPassword) UiRes.string.ui_string_hide_password else UiRes.string.ui_string_show_password) else stringResource(UiRes.string.ui_string_clear),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    )
                }
            } else if (!readOnly && value.isEmpty()) {
                IconButton(enabled = enabled, onClick = {
                    scope.launch { readClipboard()?.let(onValueChange) }
                }) {
                    Icon(
                        painter = painterResource(UiRes.drawable.ui_drawable_content_paste),
                        contentDescription = stringResource(UiRes.string.ui_string_paste),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}
