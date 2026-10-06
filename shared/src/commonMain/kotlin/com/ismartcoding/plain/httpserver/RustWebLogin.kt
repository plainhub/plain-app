package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.withIO
import kotlinx.serialization.json.*

/**
 * The web-login session token has exactly one issuer: Rust. The `sessions`
 * table row is the source of truth; the host only keeps the value Rust
 * returned so the WS frame hot path never makes a round trip.
 *
 * Rust owns password verification, the 2FA decision, ECDH, the Ed25519
 * signature and the session row; the host keeps only the platform-facing
 * work (login confirmation dialog, peer pairing write, notification).
 */
object RustWebLogin {
    const val STATUS_PENDING = "PENDING"
    const val STATUS_COMPLETED = "COMPLETED"

    private suspend fun call(action: String, clientId: String, clientIp: String, request: AuthRequest): JsonObject =
        RustContentApi.postJson("system/ws-login", buildJsonObject {
            put("action", JsonPrimitive(action))
            put("clientId", JsonPrimitive(clientId))
            put("clientIp", JsonPrimitive(clientIp))
            put("request", Json.parseToJsonElement(jsonEncode(request)))
        })

    /**
     * First leg. Returns the Rust-issued token when the login is complete
     * straight away, or [STATUS_PENDING] when the host must ask the user.
     */
    suspend fun issue(clientId: String, clientIp: String, request: AuthRequest): RustLoginResult {
        val result = call("issue", clientId, clientIp, request)
        val status = result["status"]?.jsonPrimitive?.content ?: STATUS_PENDING
        if (status != STATUS_COMPLETED) return RustLoginResult(STATUS_PENDING, "", "")
        return RustLoginResult(
            STATUS_COMPLETED,
            result.getValue("token").jsonPrimitive.content,
            result.getValue("response").toString(),
        )
    }

    /**
     * Second leg, after the user accepted the prompt. Returns the signed
     * response payload plus the issued token, or an empty result when Rust
     * refused (expired prompt, revoked session, failed ECDH).
     */
    suspend fun complete(clientId: String, clientIp: String, request: AuthRequest): RustLoginResult = withIO {
        val result = call("complete", clientId, clientIp, request)
        val status = result["status"]?.jsonPrimitive?.content ?: ""
        if (status != STATUS_COMPLETED) {
            LogCat.e("ws: login completion refused by Rust (${result["status"]})")
            return@withIO RustLoginResult("", "", "")
        }
        RustLoginResult(
            STATUS_COMPLETED,
            result.getValue("token").jsonPrimitive.content,
            result.getValue("response").toString(),
        )
    }

}

data class RustLoginResult(val status: String, val token: String, val responseJson: String) {
    fun response(): AuthResponse? =
        if (responseJson.isEmpty()) null else runCatching { jsonDecode<AuthResponse>(responseJson) }.getOrNull()
}
