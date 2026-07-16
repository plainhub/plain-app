package com.ismartcoding.plain.web.websocket

import com.ismartcoding.plain.events.WebSocketData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.web.HttpServerManager

object WebSocketHelper {
    suspend fun sendEventAsync(event: WebSocketEvent) = withIO {
        HttpServerManager.wsSessions.toList().forEach {
            val data = event.data
            if (data is WebSocketData.Text) {
                val token = HttpServerManager.tokenCache[it.clientId]
                if (token != null) {
                    it.send(addIntPrefixToByteArray(event.type.value, chaCha20Encrypt(token, data.value)))
                }
            } else if (data is WebSocketData.Binary) {
                it.send(addIntPrefixToByteArray(event.type.value, data.value))
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
