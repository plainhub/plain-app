package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.finishHttpServerStopAsync
import com.ismartcoding.plain.platform.getOwnPackageName
import com.ismartcoding.plain.preferences.PasswordTypePreference
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.closeAllWsSessions
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.HttpStatus
import com.ismartcoding.plain.httpserver.http.respondJson
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class InitResponse(
    val signaturePublicKey: String,
    val password: String = "",
)

/** Grace period for the 410 response to flush before the engine teardown kills its connection. */
private const val SHUTDOWN_RESPONSE_FLUSH_MS = 100L

/** Loopback source addresses accepted for `/shutdown`; anything else is rejected with 403. */
private val SHUTDOWN_ALLOWED_HOSTS = setOf("localhost", "127.0.0.1", "::1")

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
        if (call.remoteHost !in SHUTDOWN_ALLOWED_HOSTS) {
            call.respondNoBody(HttpStatus.FORBIDDEN)
            return@get
        }
        closeAllWsSessions()
        call.respondNoBody(HttpStatus.GONE)
        // Teardown must NOT run inside this call coroutine: the engine stop
        // joins all call coroutines, so an in-handler stop would wait on
        // itself until the 5s shutdown timeout. Fire-and-forget in an
        // independent scope instead; finishHttpServerStopAsync is idempotent
        // and serialized on the lifecycle mutex with the in-process stopper.
        coIO {
            delay(SHUTDOWN_RESPONSE_FLUSH_MS)
            finishHttpServerStopAsync()
        }
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
