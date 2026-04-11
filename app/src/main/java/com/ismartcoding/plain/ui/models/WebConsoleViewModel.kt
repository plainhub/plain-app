package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.R
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationEvent
import com.ismartcoding.plain.events.KeepAwakeChangedEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.preferences.KeepAwakePreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WebConsoleViewModel : ViewModel() {
    fun dig(context: Context) {
        viewModelScope.launch {
            DialogHelper.showLoading()
            val errorMessage = context.getString(R.string.http_server_error)
            val r = withIO { HttpServerManager.checkServerAsync() }
            DialogHelper.hideLoading()
            if (!r.websocket || !r.http) {
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.error))
                    .setMessage(errorMessage)
                    .setPositiveButton(R.string.ok) { _, _ -> }
                    .setNegativeButton(R.string.relaunch_app) { _, _ -> AppHelper.relaunch(context) }
                    .create().show()
            } else {
                DialogHelper.showConfirmDialog(context.getString(R.string.confirm), context.getString(R.string.http_server_ok))
            }
        }
    }

    fun requestIgnoreBatteryOptimization() {
        if (!powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) {
            sendEvent(IgnoreBatteryOptimizationEvent())
        }
    }

    fun enableKeepAwake(context: Context, enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            KeepAwakePreference.putAsync(context, enable)
            sendEvent(KeepAwakeChangedEvent(enable))
        }
    }
}
