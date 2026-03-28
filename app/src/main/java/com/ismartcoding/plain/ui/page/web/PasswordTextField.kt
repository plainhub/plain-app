package com.ismartcoding.plain.ui.page.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R

@Composable
fun PasswordTextField(
    value: String = "", isChanged: () -> Boolean,
    onValueChange: (String) -> Unit = {}, onConfirm: (String) -> Unit = {},
) {
    Box(modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 0.dp)) {
        val clipboardManager = LocalClipboardManager.current
        val focusRequester = remember { FocusRequester() }
        TextField(modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
            maxLines = 1, value = value, onValueChange = { onValueChange(it) },
            visualTransformation = VisualTransformation.None,
            placeholder = { Text(text = stringResource(R.string.password), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    if (isChanged()) { Button(onClick = { onConfirm(value) }) { Text(stringResource(R.string.save)) } }
                } else {
                    IconButton(onClick = { onValueChange(clipboardManager.getText()?.text ?: "") }) {
                        Icon(painter = painterResource(R.drawable.content_paste), contentDescription = stringResource(R.string.paste), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
    }
}
