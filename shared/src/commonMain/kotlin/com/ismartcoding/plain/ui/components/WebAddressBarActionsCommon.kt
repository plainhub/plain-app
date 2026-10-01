package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.relaunchApp
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun persistMdnsHostname(
    scope: CoroutineScope,
    hostname: String,
) {
    scope.launch {
        SystemPrefs.setMdnsHostname(hostname)
    }
}

fun persistPort(
    scope: CoroutineScope,
    isHttps: Boolean,
    port: Int,
) {
    scope.launch(IODispatcher) {
        if (isHttps) {
            UserPrefs.httpsPort.value = port
        } else {
            UserPrefs.httpPort.value = port
        }
    }
}
