package com.ismartcoding.plain.ui.models

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.preference.WebPreference
import com.ismartcoding.plain.features.StartHttpServerEvent
import com.ismartcoding.plain.ui.helpers.DialogHelper
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WebConsoleViewModel : ViewModel() {
    fun enableWebConsole(context: Context, enable: Boolean) {
        viewModelScope.launch {
            withIO { WebPreference.putAsync(context, enable) }
            if (enable) {
                requestIgnoreBatteryOptimization(context)
                sendEvent(StartHttpServerEvent())
            }
        }
    }

    fun dig(context: Context, httpPort: Int) {
        viewModelScope.launch {
            val client = HttpClientManager.httpClient()
            DialogHelper.showLoading()
            val errorMessage =
                "Http server is not started. Kill all apps on your phone and start the PlainApp again. Make sure that there are no other apps occupying the HTTP or HTTPS ports. If the problem persists, please provide more details about the issue by contacting ismartcoding@gmail.com."
            try {
                val r = withIO { client.get("http://127.0.0.1:${httpPort}/health_check") }
                DialogHelper.hideLoading()
                if (r.status == HttpStatusCode.OK) {
                    DialogHelper.showConfirmDialog(context, context.getString(R.string.confirm), "Http server is running well.")
                } else {
                    DialogHelper.showConfirmDialog(
                        context,
                        context.getString(R.string.error),
                        errorMessage
                    )
                }
            } catch (ex: Exception) {
                DialogHelper.hideLoading()
                DialogHelper.showConfirmDialog(
                    context,
                    context.getString(R.string.error),
                    errorMessage
                )
            }
        }
    }

    private fun requestIgnoreBatteryOptimization(context: Context) {
        try {
            val packageName = BuildConfig.APPLICATION_ID
            val pm = context.getSystemService(Application.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                context.startActivity(intent)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}