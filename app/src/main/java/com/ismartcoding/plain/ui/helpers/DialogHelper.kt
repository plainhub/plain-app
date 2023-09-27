package com.ismartcoding.plain.ui.helpers

import android.content.Context
import android.widget.Toast
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.api.GraphqlApiResult
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.LoadingDialog
import com.ismartcoding.plain.ui.models.ShowMessageEvent

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
            if (loadingDialog?.isAdded == true) {
                loadingDialog?.dismissAllowingStateLoss()
            }
            loadingDialog = null
        }
    }

    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        callback: (() -> Unit)? = null,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ ->
                callback?.invoke()
            }
            .create()
            .show()
    }

    fun showErrorDialog(
        context: Context,
        message: String,
        callback: (() -> Unit)? = null,
    ) {
        showConfirmDialog(context, getString(R.string.error), message, callback)
    }

    fun confirmToAction(
        context: Context,
        messageId: Int,
        callback: () -> Unit,
    ) {
        confirmToAction(context, getString(messageId), callback)
    }

    fun confirmToAction(
        context: Context,
        message: String,
        callback: () -> Unit,
    ) {
        MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ ->
                callback()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
            }
            .create()
            .show()
    }

    fun confirmToLeave(
        context: Context,
        callback: () -> Unit,
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.leave_page_title)
            .setMessage(R.string.leave_page_message)
            .setPositiveButton(R.string.leave) { _, _ ->
                callback()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
            }
            .create()
            .show()
    }

    fun confirmToDelete(
        context: Context,
        callback: () -> Unit,
    ) {
        confirmToAction(context, R.string.confirm_to_delete, callback)
    }
}
