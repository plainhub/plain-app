package com.ismartcoding.plain.platform.notifications

import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.client_connected
import com.ismartcoding.plain.ui.i18n.LocaleHelper

fun sendWebLoginNotification(
    appName: String,
    osName: String,
    clientIp: String,
) {
    val appDisplay = appName.replaceFirstChar { it.uppercase() }
    val description = listOf(clientIp, appDisplay, osName)
        .filter { it.isNotBlank() }.joinToString(" · ")
    postWebLoginNotification(LocaleHelper.getString(Res.string.client_connected), description)
}

internal expect fun postWebLoginNotification(title: String, description: String)

expect fun sendChatMessageNotification(targetId: String, targetName: String, messageText: String)
