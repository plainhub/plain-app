package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.preference.WebPreference
import com.ismartcoding.plain.features.IgnoreBatteryOptimizationEvent
import com.ismartcoding.plain.features.StartHttpServerEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.ui.helpers.DialogHelper
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch

class WebConsoleViewModel : ViewModel() {
    fun enableWebConsole(
        context: Context,
        enable: Boolean,
    ) {
        viewModelScope.launch {
            withIO { WebPreference.putAsync(context, enable) }
            if (enable) {
                //requestIgnoreBatteryOptimization()
                sendEvent(StartHttpServerEvent())
            }
        }
    }

    fun dig(
        context: Context,
        httpPort: Int,
    ) {
        viewModelScope.launch {
            val client = HttpClientManager.httpClient()
            DialogHelper.showLoading()
            val errorMessage = context.getString(R.string.http_server_error)
            try {
                var hasError = false
                withIO {
                    client.ws(urlString = UrlHelper.getWsTestUrl()) {
                        val reason = this.closeReason.getCompleted()
                        LogCat.d("closeReason: $reason")
                        if (reason?.message != BuildConfig.APPLICATION_ID) {
                            hasError = true
                        }
                    }
                }

                if (!hasError) {
                    val r = withIO { client.get(UrlHelper.getHealthCheckUrl()) }
                    hasError = r.status != HttpStatusCode.OK || r.bodyAsText() != BuildConfig.APPLICATION_ID
                }
                DialogHelper.hideLoading()
                if (hasError) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.error))
                        .setMessage(errorMessage)
                        .setPositiveButton(R.string.ok) { _, _ ->
                        }
                        .setNegativeButton(R.string.relaunch_app) { _, _ ->
                            AppHelper.relaunch(context)
                        }
                        .create()
                        .show()
                } else {
                    DialogHelper.showConfirmDialog(context, context.getString(R.string.confirm), context.getString(R.string.http_server_ok))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                LogCat.e(ex.toString())
                DialogHelper.hideLoading()
                MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(R.string.error))
                    .setMessage(errorMessage)
                    .setPositiveButton(R.string.ok) { _, _ ->
                    }
                    .setNegativeButton(R.string.relaunch_app) { _, _ ->
                        AppHelper.relaunch(context)
                    }
                    .create()
                    .show()
            }
        }
    }

    fun requestIgnoreBatteryOptimization() {
        val packageName = BuildConfig.APPLICATION_ID
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            sendEvent(IgnoreBatteryOptimizationEvent())
        }
    }
}
