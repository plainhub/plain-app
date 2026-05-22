package com.ismartcoding.plain.ui.components
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import android.content.Context
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun persistMdnsHostname(
    context: Context,
    scope: CoroutineScope,
    hostname: String,
) {
    scope.launch {
        withIO { MdnsHostnamePreference.putAsync(hostname) }
    }
}

fun persistPort(
    context: Context,
    scope: CoroutineScope,
    isHttps: Boolean,
    port: Int,
) {
    scope.launch(Dispatchers.IO) {
        if (isHttps) {
            HttpsPortPreference.putAsync(port)
        } else {
            HttpPortPreference.putAsync(port)
        }
    }
}

fun showRestartAppDialog(context: Context) {
    androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle(LocaleHelper.getStringSync(Res.string.restart_app_title))
        .setMessage(LocaleHelper.getStringSync(Res.string.restart_app_message))
        .setPositiveButton(LocaleHelper.getStringSync(Res.string.relaunch_app)) { _, _ ->
            AppHelper.relaunch(context)
        }
        .setCancelable(false)
        .create()
        .show()
}
