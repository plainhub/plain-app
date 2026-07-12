package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.relaunchApp
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun persistMdnsHostname(
    scope: CoroutineScope,
    hostname: String,
) {
    scope.launch {
        MdnsHostnamePreference.putAsync(hostname)
    }
}

fun persistPort(
    scope: CoroutineScope,
    isHttps: Boolean,
    port: Int,
) {
    scope.launch(IODispatcher) {
        if (isHttps) {
            HttpsPortPreference.putAsync(port)
        } else {
            HttpPortPreference.putAsync(port)
        }
    }
}

fun showRestartAppDialog() {
    coIO {
        DialogHelper.showConfirmDialog(
            title = LocaleHelper.getStringAsync(Res.string.restart_app_title),
            message = LocaleHelper.getStringAsync(Res.string.restart_app_message),
            confirmButton = Pair(LocaleHelper.getStringAsync(Res.string.relaunch_app)) { relaunchApp() },
        )
    }
}