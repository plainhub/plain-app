package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.PasswordType
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.disposeHttpServer
import com.ismartcoding.plain.platform.getOwnPackageName
import com.ismartcoding.plain.preferences.PasswordTypePreference
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.HttpStatus
import com.ismartcoding.plain.web.setOnlineClientIds

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
        disposeHttpServer()
    }

    post("/init") { call ->
        val clientId = call.header("c-id") ?: ""
        if (clientId.isEmpty()) {
            call.respondText("`c-id` is missing in the headers", status = HttpStatus.BAD_REQUEST)
            return@post
        }
        if (!TempData.webEnabled.value) {
            call.respondText("web_access_disabled", status = HttpStatus.FORBIDDEN)
            return@post
        }
        HttpServerManager.clientIpCache[clientId] = call.remoteHost

        val bodyBytes = runCatching { call.receiveBody() }.getOrNull()
        if (bodyBytes != null && bodyBytes.isNotEmpty()) {
            val token = HttpServerManager.tokenCache[clientId]
            if (token != null) {
                val decrypted = chaCha20Decrypt(token, bodyBytes)
                if (decrypted != null) {
                    call.respondNoBody(HttpStatus.OK)
                    return@post
                }
            }
        }

        if (PasswordTypePreference.getValueAsync() == PasswordType.NONE) {
            call.respondText(HttpServerManager.resetPasswordAsync())
        } else {
            call.respondNoBody(HttpStatus.NO_CONTENT)
        }
    }
}
