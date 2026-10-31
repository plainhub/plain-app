package com.ismartcoding.plain.ui.components


import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.device_name
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.save
import com.ismartcoding.plain.i18n.feed_err_server
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.graphql.GraphqlHttpException

@Composable
fun DeviceRenameDialog(
    name: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val serverError = stringResource(Res.string.feed_err_server)
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
            scope.launchSafe(onError = { error ->
                LogCat.e(error)
                val message = if (error is GraphqlHttpException && error.status in 500..599) serverError else error.message ?: error.toString()
                ToastManager.showErrorToast(message)
            }) {
                onSave(newName.value)
                onDismiss()
            }
        },
    )
}
