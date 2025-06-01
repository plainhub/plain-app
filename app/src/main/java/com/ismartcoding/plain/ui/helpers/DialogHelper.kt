package com.ismartcoding.plain.ui.helpers

import android.widget.Toast
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.features.ConfirmDialogEvent
import com.ismartcoding.plain.features.LoadingDialogEvent
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.base.ToastManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

object DialogHelper {
    private var showLoadingJob: Job? = null
    private var hideLoadingJob: Job? = null
    fun showMessage(
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
    ) {
        val durationMs = if (duration == Toast.LENGTH_SHORT) 2000L else 3500L
        ToastManager.showInfoToast(message, durationMs)
    }

    fun showErrorMessage(
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
    ) {
        val durationMs = if (duration == Toast.LENGTH_SHORT) 2000L else 3500L
        ToastManager.showErrorToast(message, durationMs)
    }

    fun showSuccess(resId: Int) {
        ToastManager.showSuccessToast(getString(resId), 2000L)
    }

    fun showMessage(resId: Int) {
        showMessage(getString(resId))
    }

    fun showMessage(r: ApiResult) {
        ToastManager.showErrorToast(r.errorMessage())
    }

    fun showMessage(ex: Throwable) {
        ToastManager.showErrorToast(ex.toString())
    }

    fun showLoading(message: String = "") {
        hideLoadingJob?.cancel()
        showLoadingJob?.cancel()
        showLoadingJob = coIO {
            delay(200)
            sendEvent(LoadingDialogEvent(true, message))
        }
    }

    fun hideLoading() {
        hideLoadingJob?.cancel()
        showLoadingJob?.cancel()
        hideLoadingJob = coIO {
            delay(500)
            sendEvent(LoadingDialogEvent(false))
        }
    }

    fun showConfirmDialog(
        title: String,
        message: String,
        confirmButton: Pair<String, () -> Unit> = Pair(getString(R.string.ok)) {},
        dismissButton: Pair<String, () -> Unit>? = null,
    ) {
        sendEvent(ConfirmDialogEvent(title, message, confirmButton, dismissButton))
    }

    fun showConfirmDialog(
        title: String,
        message: String,
        callback: () -> Unit = {},
    ) {
        showConfirmDialog(title, message, confirmButton = Pair(getString(R.string.ok), callback))
    }

    fun showErrorDialog(
        message: String,
        callback: () -> Unit = {},
    ) {
        showConfirmDialog(getString(R.string.error), message, confirmButton = Pair(getString(R.string.ok), callback))
    }

    fun confirmToAction(
        messageId: Int,
        callback: () -> Unit,
    ) {
        confirmToAction(getString(messageId), callback)
    }

    fun confirmToAction(
        message: String,
        callback: () -> Unit,
    ) {
        sendEvent(ConfirmDialogEvent("", message, confirmButton = Pair(getString(R.string.ok)) {
            callback()
        }, dismissButton = Pair(getString(R.string.cancel)) {
        }))
    }

    fun confirmToLeave(
        callback: () -> Unit,
    ) {
        sendEvent(ConfirmDialogEvent(getString(R.string.leave_page_title),
            getString(R.string.leave_page_message), confirmButton = Pair(getString(R.string.leave)) {
                callback()
            }, dismissButton = Pair(getString(R.string.cancel)) {
            })
        )
    }

    fun confirmToDelete(
        callback: () -> Unit,
    ) {
        confirmToAction(R.string.confirm_to_delete, callback)
    }

    fun showTextCopiedMessage(text: String) {
        if (!isTPlus()) {
            showConfirmDialog("", MainApp.instance.getString(R.string.copied_to_clipboard_format, text))
        }
    }
}
