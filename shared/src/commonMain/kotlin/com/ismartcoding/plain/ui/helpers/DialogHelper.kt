package com.ismartcoding.plain.ui.helpers

import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.events.ConfirmDialogEvent
import com.ismartcoding.plain.events.LoadingDialogEvent
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.ui.base.ToastManager
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString as getComposeString

object DialogHelper {
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

    fun showMessage(r: ApiResult) {
        ToastManager.showErrorToast(r.errorMessage())
    }

    fun showMessage(ex: Throwable) {
        ToastManager.showErrorToast(ex.toString())
    }

    fun showLoading(message: String = "") {
        sendEvent(LoadingDialogEvent(true, message))
    }

    fun hideLoading() {
        sendEvent(LoadingDialogEvent(false))
    }

    fun showConfirmDialog(
        title: String,
        message: String,
        confirmButton: Pair<String, () -> Unit>? = null,
        dismissButton: Pair<String, () -> Unit>? = null,
    ) {
        if (confirmButton != null) {
            sendEvent(ConfirmDialogEvent(title, message, confirmButton, dismissButton))
        } else {
            coIO {
                sendEvent(ConfirmDialogEvent(title, message, Pair(getComposeString(Res.string.ok)) {}, dismissButton))
            }
        }
    }

    fun showConfirmDialog(
        title: String,
        message: String,
        callback: () -> Unit,
    ) {
        coIO {
            sendEvent(ConfirmDialogEvent(title, message, Pair(getComposeString(Res.string.ok), callback), null))
        }
    }

    fun showErrorDialog(
        message: String,
        callback: () -> Unit = {},
    ) {
        coIO {
            sendEvent(ConfirmDialogEvent(getComposeString(Res.string.error), message, Pair(getComposeString(Res.string.ok), callback), null))
        }
    }

    fun confirmToAction(
        resource: StringResource,
        callback: () -> Unit,
    ) {
        coIO { confirmToAction(getComposeString(resource), callback) }
    }

    fun confirmToAction(
        message: String,
        callback: () -> Unit,
    ) {
        coIO {
            sendEvent(ConfirmDialogEvent("", message,
                confirmButton = Pair(getComposeString(Res.string.ok)) { callback() },
                dismissButton = Pair(getComposeString(Res.string.cancel)) {}))
        }
    }

    fun confirmToLeave(
        callback: () -> Unit,
    ) {
        coIO {
            sendEvent(ConfirmDialogEvent(
                getComposeString(Res.string.leave_page_title),
                getComposeString(Res.string.leave_page_message),
                confirmButton = Pair(getComposeString(Res.string.leave)) { callback() },
                dismissButton = Pair(getComposeString(Res.string.cancel)) {}))
        }
    }

    fun confirmToDelete(
        callback: () -> Unit,
    ) {
        coIO { confirmToAction(getComposeString(Res.string.confirm_to_delete), callback) }
    }

    fun showTextCopiedMessage(text: String) {
        coIO {
            showConfirmDialog("", getComposeString(Res.string.copied_to_clipboard_format, text))
        }
    }
}
