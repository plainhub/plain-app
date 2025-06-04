package com.ismartcoding.plain.web.websocket

import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.websocket.send

object WebSocketHelper {
    suspend fun sendEventAsync(event: WebSocketEvent) {
//            if (BuildConfig.DEBUG) {
//                LogCat.d("sendEventAsync: ${event.data}")
//            }
        HttpServerManager.wsSessions.toList().forEach {
            if (event.data is String) {
                val token = HttpServerManager.tokenCache[it.clientId]
                if (token != null) {
                    it.session.send(addIntPrefixToByteArray(event.type.value, CryptoHelper.aesEncrypt(token, event.data)))
                }
            } else {
                it.session.send(addIntPrefixToByteArray(event.type.value, event.data as ByteArray))
            }
        }
    }
}

fun addIntPrefixToByteArray(value: Int, byteArray: ByteArray): ByteArray {
    val intBytes = ByteArray(4) // Int is 4 bytes long
    intBytes[0] = (value shr 24).toByte()
    intBytes[1] = (value shr 16).toByte()
    intBytes[2] = (value shr 8).toByte()
    intBytes[3] = value.toByte()

    return intBytes + byteArray
}