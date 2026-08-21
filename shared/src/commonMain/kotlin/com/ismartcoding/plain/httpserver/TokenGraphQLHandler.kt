package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpStatus

/**
 * Shared "token-mode" GraphQL request flow, used by both [MainGraphQLService]
 * (`/graphql`, session token) and [GuestGraphQLService] (`/guest_graphql`,
 * share token). Keeps decrypt → replay-guard → execute → encrypt identical
 * so the two entry points never drift.
 *
 * @param key the ChaCha20 key (session token for main, derived `shared_token`
 *   for guest).
 * @param execute runs the decrypted GraphQL query string through the schema,
 *   returning the JSON response string.
 * @return `true` when a response was sent, `false` when the caller should
 *   abort (error response already sent).
 */
object TokenGraphQLHandler {
    suspend fun handle(
        clientId: String,
        key: ByteArray,
        call: HttpCall,
        execute: suspend (query: String, call: HttpCall) -> String,
    ): Boolean {
        val decryptedBytes = chaCha20Decrypt(key, call.receiveBody())
        val decryptedStr = decryptedBytes?.decodeToString() ?: ""
        if (decryptedStr.isEmpty()) {
            call.respondNoBody(HttpStatus.UNAUTHORIZED)
            return false
        }

        val parsed = ReplayGuard.parse(decryptedStr)
        if (parsed == null) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return false
        }
        val err = ReplayGuard.validate(clientId, parsed)
        if (err != null) {
            call.respondNoBody(HttpStatus.BAD_REQUEST)
            return false
        }

        HttpServerManager.clientRequestTs[clientId] = TimeHelper.nowMillis()
        sendEvent(WebRequestReceivedEvent())
        val result = execute(parsed.body, call)
        call.respond(
            chaCha20Encrypt(key, result),
            contentType = "application/octet-stream",
        )
        return true
    }
}