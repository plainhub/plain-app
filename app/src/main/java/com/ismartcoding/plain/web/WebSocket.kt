package com.ismartcoding.plain.web

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.preferences.AuthTwoFactorPreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.web.websocket.WebSocketSession
import io.ktor.server.plugins.origin
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlin.text.decodeToString
import kotlin.text.isEmpty

fun Route.addWebSocket() {
    webSocket("/") {
        val q = call.request.queryParameters
        if (q["test"] == "1") {
            close(CloseReason(CloseReason.Codes.NORMAL, BuildConfig.APPLICATION_ID))
            return@webSocket
        }
        val clientId = q["cid"] ?: ""
        if (clientId.isEmpty()) {
            LogCat.e("ws: `cid` is missing")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "`cid` is missing"))
            return@webSocket
        }

        val session = WebSocketSession(System.currentTimeMillis(), clientId, this)
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        if (q["auth"] == "1") {
                            val clientIp = HttpServerManager.getClientIpForLogin(
                                clientId,
                                call.request.origin.remoteAddress
                            )
                            val rateLimitKey = clientIp.ifEmpty { "cid:$clientId" }
                            if (!HttpServerManager.tryAcquireLoginAttempt(rateLimitKey)) {
                                LogCat.e("ws: too_many_login_attempts, key=$rateLimitKey")
                                close(
                                    CloseReason(
                                        CloseReason.Codes.TRY_AGAIN_LATER,
                                        "too_many_login_attempts"
                                    )
                                )
                                return@webSocket
                            }

                            var r: AuthRequest? = null
                            val hash = CryptoHelper.sha512(
                                PasswordPreference.getAsync(MainApp.instance).toByteArray()
                            )
                            val token = HttpServerManager.hashToToken(hash)
                            val decryptedBytes =
                                CryptoHelper.chaCha20Decrypt(token, frame.readBytes())
                            if (decryptedBytes != null) {
                                r = jsonDecode<AuthRequest>(decryptedBytes.decodeToString())
                            }
                            if (r?.password == hash) {
                                val event = ConfirmToAcceptLoginEvent(this, clientId, r)
                                if (AuthTwoFactorPreference.getAsync(MainApp.instance)) {
                                    send(
                                        CryptoHelper.chaCha20Encrypt(
                                            token,
                                            JsonHelper.jsonEncode(AuthResponse(AuthStatus.PENDING))
                                        )
                                    )
                                    sendEvent(event)
                                } else {
                                    coIO {
                                        HttpServerManager.respondTokenAsync(event, clientIp)
                                    }
                                }
                            } else {
                                LogCat.e("ws: invalid_password")
                                close(
                                    CloseReason(
                                        CloseReason.Codes.TRY_AGAIN_LATER,
                                        "invalid_password"
                                    )
                                )
                            }
                        } else {
                            val token = HttpServerManager.tokenCache[clientId]
                            if (token != null) {
                                val decryptedBytes =
                                    CryptoHelper.chaCha20Decrypt(token, frame.readBytes())
                                if (decryptedBytes != null) {
                                    LogCat.d("ws: add session ${session.id}, ts: ${decryptedBytes.decodeToString()}")
                                    HttpServerManager.wsSessions.add(session)
                                    HttpServerManager.wsSessionCount.value =
                                        HttpServerManager.wsSessions.distinctBy { it.clientId }.size
                                } else {
                                    LogCat.d("ws: invalid_request")
                                    close(
                                        CloseReason(
                                            CloseReason.Codes.TRY_AGAIN_LATER,
                                            "invalid_request"
                                        )
                                    )
                                }
                            } else {
                                LogCat.d("ws: invalid_request")
                                close(
                                    CloseReason(
                                        CloseReason.Codes.TRY_AGAIN_LATER,
                                        "invalid_request"
                                    )
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }
        } catch (ex: Exception) {
            LogCat.e("ws: $ex")
        } finally {
            LogCat.d("ws: remove session ${session.id}")
            HttpServerManager.wsSessions.removeIf { it.id == session.id }
            HttpServerManager.wsSessionCount.value =
                HttpServerManager.wsSessions.distinctBy { it.clientId }.size
        }
    }
}