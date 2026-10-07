package com.ismartcoding.plain.ble

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.api.ContentApiSession
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable

internal object BleDataChannel {
    private val client by lazy { createPlainHttpClient(PlainHttpClientSpec.HttpHost) }
    suspend fun outgoing(token: String, exchange: suspend (ByteArray) -> ByteArray) = withTimeout(120_000) {
        val target = session()
        client.webSocket("${target.baseUrl.replace("http://", "ws://")}/chat/ble-exchange/$token", target.headers()) { socket ->
            val bytes = withTimeout(15_000) { checkNotNull(socket.incoming.receive().binary) { "Expected BLE request bytes" } }
            val response = coroutineScope {
                val operation = async { exchange(bytes) }
                select<ByteArray> {
                    operation.onAwait { it }
                    socket.incoming.onReceiveCatching { error("BLE data channel closed or received an unexpected frame") }
                }
            }
            socket.sendBinary(response)
        }
    }
    suspend fun incoming(remoteHost: String, characteristicUuid: String, message: ByteArray): ByteArray = withTimeout(120_000) {
        val target = session()
        var response: ByteArray? = null
        client.webSocket("${target.baseUrl.replace("http://", "ws://")}/chat/ble-incoming", target.headers()) { socket ->
            socket.sendText(JsonHelper.jsonEncode(ConnectionFacts(remoteHost, characteristicUuid)))
            socket.sendBinary(message)
            response = withTimeout(120_000) { checkNotNull(socket.incoming.receive().binary) { "Expected BLE response bytes" } }
        }
        checkNotNull(response)
    }
    private suspend fun session(): ContentApiSession { return RustContentApi.transportSession() }
    @Serializable private data class ConnectionFacts(val remoteHost: String, val characteristicUuid: String)
}
