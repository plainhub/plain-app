package com.ismartcoding.plain.ui.components.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.PickFileTag
import com.ismartcoding.plain.data.enums.PickFileType
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.ui.base.PIconButton

@Composable
fun ChatInput(
    value: String,
    modifier: Modifier = Modifier,
    hint: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onSend: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
) {
    var hasFocus by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            modifier =
                modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .onFocusChanged { focusState -> hasFocus = focusState.hasFocus }
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    hint,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
        if (hasFocus) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PIconButton(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = stringResource(R.string.images),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.IMAGE_VIDEO, multiple = true))
                }
                PIconButton(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = stringResource(R.string.files),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, multiple = true))
                }
                Spacer(modifier = Modifier.weight(1f))
                PIconButton(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = stringResource(R.string.send_message),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    onSend()
                }
            }
        }
    }
}
