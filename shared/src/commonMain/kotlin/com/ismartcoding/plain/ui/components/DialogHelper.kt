package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString as getComposeString

object DialogHelper {
    val loading = mutableStateOf(false)
    val confirmation = mutableStateOf<ConfirmDialog?>(null)

    fun showMessage(
        message: String,
        durationMs: Long = 2000L,
    ) {
        ToastManager.showInfoToast(message, durationMs)
    }

    fun showErrorMessage(
        message: String,
        durationMs: Long = 2000L,
    ) {
        ToastManager.showErrorToast(message, durationMs)
    }

    fun showSuccess(resource: StringResource) {
        coIO { ToastManager.showSuccessToast(getComposeString(resource), 2000L) }
    }

    fun showMessage(resource: StringResource) {
        coIO { showMessage(getComposeString(resource)) }
    }

    fun showMessage(ex: Throwable) {
        ToastManager.showErrorToast(ex.toString())
    }

    fun showLoading() {
        loading.value = true
    }

    fun hideLoading() {
        loading.value = false
    }

    fun showConfirmDialog(
        title: String,
        message: String,
        confirmButton: Pair<String, () -> Unit>? = null,
        dismissButton: Pair<String, () -> Unit>? = null,
        danger: Boolean = false,
        checkbox: ConfirmDialogCheckbox? = null,
    ) {
        if (confirmButton != null) {
            confirmation.value = ConfirmDialog(title, message, confirmButton, dismissButton, danger, checkbox)
        } else {
            coIO {
                confirmation.value = ConfirmDialog(title, message, Pair(getComposeString(Res.string.ok)) {}, dismissButton, danger, checkbox)
            }
        }
    }

    fun showConfirmDialog(
        title: String,
        message: String,
        danger: Boolean = false,
        callback: () -> Unit = {},
    ) {
        coIO {
            confirmation.value = ConfirmDialog(title, message, Pair(getComposeString(Res.string.ok), callback), null, danger)
        }
    }

    fun showErrorDialog(
        message: String,
        callback: () -> Unit = {},
    ) {
        coIO {
            confirmation.value = ConfirmDialog(getComposeString(Res.string.error), message, Pair(getComposeString(Res.string.ok), callback), null)
        }
    }
}

suspend inline fun confirmActionAsync(
    title: StringResource,
    message: StringResource,
    noinline callback: () -> Unit = {},
    danger: Boolean = false,
) {
    DialogHelper.confirmation.value = ConfirmDialog(
        getComposeString(title), getComposeString(message),
        Pair(getComposeString(Res.string.confirm), callback),
        Pair(getComposeString(Res.string.cancel)) { }, danger
    )
}
