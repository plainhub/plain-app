package com.ismartcoding.plain.web.websocket

import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.websocket.*

object WebSocketHelper {
    suspend fun sendEventAsync(event: WebSocketEvent) {
        val json = JsonHelper.jsonEncode(event)
        HttpServerManager.wsSessions.toList().forEach {
            if (event.encrypted) {
                val token = HttpServerManager.tokenCache[it.clientId]
                if (token != null) {
                    it.session.send(CryptoHelper.aesEncrypt(token, json))
                }
            } else {
                it.session.send(json.toByteArray())
            }
        }
    }
}
