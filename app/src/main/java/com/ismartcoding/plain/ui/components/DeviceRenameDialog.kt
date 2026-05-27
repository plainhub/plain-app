package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.device_name
import com.ismartcoding.plain.i18n.save
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.ui.base.TextFieldDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceRenameDialog(name: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val newName = remember {
        mutableStateOf(name)
    }
    TextFieldDialog(
        title = stringResource(Res.string.device_name),
        value = newName.value,
        placeholder = name,
        onValueChange = {
            newName.value = it
        },
        onDismissRequest = {
            onDismiss()
        },
        confirmText = stringResource(Res.string.save),
        onConfirm = {
            scope.launch {
                withIO {
                    DeviceNamePreference.putAsync(newName.value)
                    TempData.deviceName.value = newName.value
                    sendEvent(WebSocketEvent(EventType.DEVICE_NAME_UPDATED, jsonEncode(newName.value)))
                }
                onDone(newName.value)
                onDismiss()
            }
        },
    )
}