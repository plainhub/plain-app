package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.platform.LocaleHelper

import com.ismartcoding.plain.i18n.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.events.IgnoreBatteryOptimizationEvent
import com.ismartcoding.plain.events.KeepAwakeChangedEvent
import com.ismartcoding.plain.platform.checkHttpServerAsync
import com.ismartcoding.plain.platform.isIgnoringBatteryOptimizations
import com.ismartcoding.plain.platform.relaunchApp
import com.ismartcoding.plain.preferences.KeepAwakePreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class WebConsoleViewModel : ViewModel() {
    fun dig() {
        viewModelScope.launch {
            DialogHelper.showLoading()
            val errorMessage = LocaleHelper.getStringAsync(Res.string.http_server_error)
            val serverUp = checkHttpServerAsync()
            DialogHelper.hideLoading()
            if (!serverUp) {
                DialogHelper.showConfirmDialog(
                    LocaleHelper.getStringAsync(Res.string.error),
                    errorMessage,
                    confirmButton = Pair(LocaleHelper.getStringAsync(Res.string.ok)) {},
                    dismissButton = Pair(LocaleHelper.getStringAsync(Res.string.relaunch_app)) { relaunchApp() },
                )
            } else {
                DialogHelper.showConfirmDialog(LocaleHelper.getStringAsync(Res.string.confirm), LocaleHelper.getStringAsync(Res.string.http_server_ok))
            }
        }
    }

    fun requestIgnoreBatteryOptimization() {
        if (!isIgnoringBatteryOptimizations()) {
            sendEvent(IgnoreBatteryOptimizationEvent())
        }
    }

    fun enableKeepAwake(enable: Boolean) {
        viewModelScope.launchSafe {
            KeepAwakePreference.putAsync(enable)
            sendEvent(KeepAwakeChangedEvent(enable))
        }
    }
}
