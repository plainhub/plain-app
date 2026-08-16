package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.discover.PairingCore
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.HttpStatus
import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * `POST /nearby` — LAN transport for pairing messages. The request body is
 * the same [NearbyMessageType]-prefixed wire format the BLE nearby service
 * uses ("PAIR_REQUEST:{…}" — see [PairingCore.formatMessage]).
 */
fun HttpRouter.addNearbyRoutes() {
    post("/nearby") { call ->
        val body = call.receiveText()
        val type = NearbyMessageType.entries.firstOrNull { body.startsWith(it.toPrefix()) } ?: run {
            LogCat.e("NearbyRoutes: unknown message type, body=${body.take(50)}")
            call.respondText("unknown message type", status = HttpStatus.BAD_REQUEST)
            return@post
        }
        val payload = body.removePrefix(type.toPrefix())
        LogCat.d("NearbyRoutes: type=$type from=${call.remoteHost}")

        when (type) {
            NearbyMessageType.PAIR_REQUEST -> {
                PairingCore.handlePairRequest(JsonHelper.jsonDecode(payload), call.remoteHost, isBle = false)
            }
            NearbyMessageType.PAIR_RESPONSE -> {
                PairingCore.handlePairResponse(JsonHelper.jsonDecode(payload), senderIp = call.remoteHost)
            }
            NearbyMessageType.PAIR_CANCEL -> {
                PairingCore.handlePairCancel(JsonHelper.jsonDecode(payload))
            }
            NearbyMessageType.DISCOVER, NearbyMessageType.DISCOVER_REPLY -> Unit
        }
        call.respondText("1")
    }
}
