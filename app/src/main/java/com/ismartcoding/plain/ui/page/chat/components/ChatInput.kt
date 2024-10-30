package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.theme.secondaryTextColor

@Composable
fun ChatInput(
    value: String,
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
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
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
                    color = MaterialTheme.colorScheme.secondaryTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
        if (hasFocus) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PIconButton(
                    icon = R.drawable.image,
                    contentDescription = stringResource(R.string.images),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.IMAGE_VIDEO, multiple = true))
                }
                PIconButton(
                    icon = R.drawable.files,
                    contentDescription = stringResource(R.string.files),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, multiple = true))
                }
                Spacer(modifier = Modifier.weight(1f))
                PIconButton(
                    icon = R.drawable.send,
                    contentDescription = stringResource(R.string.send_message),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    onSend()
                }
            }
        } else {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
