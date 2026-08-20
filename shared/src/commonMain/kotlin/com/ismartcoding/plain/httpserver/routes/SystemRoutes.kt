package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.getOwnPackageName
import com.ismartcoding.plain.platform.stopHttpEngineAsync
import com.ismartcoding.plain.preferences.PasswordTypePreference
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.HttpStatus
import com.ismartcoding.plain.httpserver.http.respondJson
import com.ismartcoding.plain.httpserver.setOnlineClientIds
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class InitResponse(
    val signaturePublicKey: String,
    val password: String = "",
)

/**
 * `/health`, `/shutdown`, `/init` — simple system endpoints shared between
 * Android (Ktor) and iOS (SwiftNIO future). All business logic lives here;
 * the platform layer only dispatches the request.
 */
fun HttpRouter.addSystemRoutes() {
    get("/health") { call ->
        call.respondText(getOwnPackageName())
    }

    get("/shutdown") { call ->
        if (call.remoteHost != "localhost") {
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return@get
        }
        HttpServerManager.wsSessions.toList().forEach { it.close() }
        HttpServerManager.wsSessions.clear()
        setOnlineClientIds(emptySet())
        call.respondNoBody(HttpStatus.GONE)
        // Give the response a moment to flush before the engine tears down the
        // connection that is still writing it.
        delay(100)
        stopHttpEngineAsync()
    }

    post("/init") { call ->
        val clientId = call.header("c-id") ?: ""
        if (clientId.isEmpty()) {
            call.respondText("`c-id` is missing in the headers", status = HttpStatus.BAD_REQUEST)
            return@post
        }
        if (!TempData.canDesktopAccess()) {
            call.respondText("desktop_access_disabled", status = HttpStatus.FORBIDDEN)
            return@post
        }
        HttpServerManager.clientIpCache.put(clientId, call.remoteHost)

        val bodyBytes = runCatching { call.receiveBody() }.getOrNull()
        if (bodyBytes != null && bodyBytes.isNotEmpty()) {
            val token = HttpServerManager.tokenCache.get(clientId)
            if (token != null) {
                val decrypted = chaCha20Decrypt(token, bodyBytes)
                if (decrypted != null) {
                    call.respondJson(InitResponse(SignatureHelper.getRawPublicKeyBase64Async()))
                    return@post
                }
            }
        }

        val signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async()
        if (PasswordTypePreference.getValueAsync() == PasswordType.NONE) {
            val password = HttpServerManager.resetPasswordAsync()
            call.respondJson(InitResponse(signaturePublicKey, password))
        } else {
            call.respondJson(InitResponse(signaturePublicKey))
        }
    }
}
