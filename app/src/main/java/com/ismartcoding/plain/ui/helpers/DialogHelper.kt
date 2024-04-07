package com.ismartcoding.plain.ui.helpers

import android.content.Context
import android.widget.Toast
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.api.GraphqlApiResult
import com.ismartcoding.plain.features.ConfirmDialogEvent
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.LoadingDialog
import com.ismartcoding.plain.ui.models.ShowMessageEvent
import kotlinx.coroutines.delay

object DialogHelper {
    private var loadingDialog: LoadingDialog? = null

    fun showMessage(
        message: String,
        duration: Int = Toast.LENGTH_SHORT,
    ) {
        sendEvent(ShowMessageEvent(message, duration))
    }

    fun showMessage(resId: Int) {
        showMessage(getString(resId))
    }

    fun showMessage(r: ApiResult) {
        showMessage(r.errorMessage())
    }

    fun showMessage(ex: Throwable) {
        showMessage(ex.toString())
    }

    fun <D : Operation.Data> showMessage(response: ApolloResponse<D>) {
        showMessage(response.errors?.joinToString(", ") { it.message } ?: "")
    }

    fun <D : Operation.Data> showMessage(result: GraphqlApiResult<D>) {
        showMessage(result.getErrorMessage())
    }

    fun showLoading(message: String = "") {
        coMain {
            if (loadingDialog == null) {
                loadingDialog = LoadingDialog(message)
                loadingDialog?.show()
            } else {
                loadingDialog?.updateMessage(message)
            }
        }
    }

    fun hideLoading() {
        coMain {
            delay(200)
            if (loadingDialog?.isAdded == true) {
                loadingDialog?.dismissAllowingStateLoss()
            }
            loadingDialog = null
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
        context: Context,
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
        context: Context,
        callback: () -> Unit,
    ) {
        confirmToAction(context, R.string.confirm_to_delete, callback)
    }
}
