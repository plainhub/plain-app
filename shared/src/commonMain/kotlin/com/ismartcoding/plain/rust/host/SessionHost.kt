package com.ismartcoding.plain.rust.host

import com.ismartcoding.plain.features.sessions.WebLoginClosedFacts
import com.ismartcoding.plain.features.sessions.WebLoginCompletedFacts
import com.ismartcoding.plain.features.sessions.WebLoginRequestFacts
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.notifications.sendWebLoginNotification
import com.ismartcoding.plain.ui.app.MainViewModel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

internal object SessionHost {
    private val methods = setOf("systemWebLoginRequested", "systemWebLoginClosed", "systemWebLoginCompleted", "systemWebLoginSnapshot")
    fun handles(method: String): Boolean = method in methods

    fun handle(method: String, params: JsonObject): HostResult {
        when (method) {
            "systemWebLoginRequested" -> MainViewModel.pendingLoginRequest.value = JsonHelper.jsonDecodeFromElement<WebLoginRequestFacts>(params)
            "systemWebLoginSnapshot" -> MainViewModel.pendingLoginRequest.value = params.getValue("requests").jsonArray.lastOrNull()?.let { JsonHelper.jsonDecodeFromElement<WebLoginRequestFacts>(it) }
            "systemWebLoginClosed" -> {
                val closed = JsonHelper.jsonDecodeFromElement<WebLoginClosedFacts>(params)
                if (MainViewModel.pendingLoginRequest.value?.requestId == closed.requestId) MainViewModel.pendingLoginRequest.value = null
            }
            "systemWebLoginCompleted" -> {
                val facts = JsonHelper.jsonDecodeFromElement<WebLoginCompletedFacts>(params)
                val request = facts.request
                sendWebLoginNotification(request.appName, request.osName, facts.clientIp)
            }
            else -> error("Unsupported session host method")
        }
        return hostResult(true)
    }
}
