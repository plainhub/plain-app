package com.ismartcoding.plain.chat.peer

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.*

object PeerChatParser {
    suspend fun decrypt(token: ByteArray, clientId: String, publicKey: ByteArray, raw: ByteArray): PeerChatParseResult {
        val result = RustPeerWireStore.authenticateEnvelope(token, publicKey, raw)
        val code = HttpStatusCode.fromValue(result.getValue("status").jsonPrimitive.int)
        return if (code.value == 200) PeerChatParseResult(code, result.getValue("content").jsonPrimitive.content, result.getValue("signature").jsonPrimitive.content, result.getValue("timestamp").jsonPrimitive.long) else PeerChatParseResult(code)
    }
}
