package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.helpers.IODispatcher
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
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