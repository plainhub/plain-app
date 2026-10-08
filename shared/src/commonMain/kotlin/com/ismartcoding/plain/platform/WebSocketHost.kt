package com.ismartcoding.plain.platform

import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.serialization.json.*

internal object WebSocketHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement {
        when (method) {
            "systemWebLoginRequest" -> {
                val facts = JsonHelper.jsonDecodeFromElement<WebLoginRequestFacts>(params)
                sendEvent(ConfirmToAcceptLoginEvent(facts.clientId, facts.request, facts.requestId, facts.clientIp))
            }
            "systemWebLoginCompleted" -> {
                val facts = JsonHelper.jsonDecodeFromElement<WebLoginCompletedFacts>(params)
                if (facts.chatPaired) PeerManager.load()
                val request = facts.request
                sendWebLoginNotification(request.browserName, request.browserVersion, request.osName, request.osVersion, facts.clientIp)
            }
            "systemWebSocketRegistered" -> onWebSocketSessionStarted()
            "systemScreenMirrorControls" -> JsonHelper.jsonDecodeFromElement<ScreenMirrorControlsFacts>(params).inputs.forEach { dispatchScreenMirrorControl(it) }
            "systemScreenMirrorResetTouch" -> resetScreenMirrorTouchStream()
            else -> error("Unsupported WebSocket platform operation")
        }
        return JsonNull
    }
}
