package com.ismartcoding.plain.web.routes

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerChatParser
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.sha512
import com.ismartcoding.plain.preferences.AuthTwoFactorPreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.web.AuthRequest
import com.ismartcoding.plain.web.AuthResponse
import com.ismartcoding.plain.web.AuthStatus
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.setOnlineClientIds
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.WsCloseCode
import com.ismartcoding.plain.web.http.WsSession
import com.ismartcoding.plain.web.WsSessionHandle

/**
 * Adapter that exposes a [WsSession] (platform-agnostic WebSocket session
 * used inside route handlers) as a [WsSessionHandle] (the interface business
 * code uses to push events back to the client). The two interfaces are kept
 * separate so the route handler's [WsSession] can carry platform-only state
 * (frame send/receive) while [WsSessionHandle] remains minimal.
 */
private class WsSessionAsHandle(
    override val id: Long,
    override val clientId: String,
    private val ws: WsSession,
) : WsSessionHandle {
    override suspend fun send(bytes: ByteArray) = ws.sendBinary(bytes)
    override suspend fun close(code: Int, reason: String) = ws.close(code, reason)
    override suspend fun close() = ws.close()
}

/**
 * Registers `/status` and `/` WebSocket routes.
 *
 * All authentication, decryption, session tracking, and event dispatch
 * lives here so the platform layer (Ktor `webSocket {}` / SwiftNIO upgrade
 * handler) only needs to expose a [WsSession] and dispatch incoming frames.
 */
fun HttpRouter.addWebSocketRoutes() {
    webSocket("/status") { ws, call ->
        val peerId = call.queryParam("cid") ?: ""
        if (peerId.isEmpty()) {
            ws.close(WsCloseCode.POLICY_VIOLATION, "`cid` is missing")
            return@webSocket
        }
        var authenticated = false
        try {
            while (true) {
                val frame = ws.receiveBinary() ?: break
                if (authenticated) continue

                val token = PeerCacher.getKeyBytes(peerId)
                val publicKey = PeerCacher.getPublicKeyBytes(peerId)
                if (token == null || publicKey == null) {
                    ws.close(WsCloseCode.POLICY_VIOLATION, "unknown_peer")
                    return@webSocket
                }
                val decryptResult = PeerChatParser.decrypt(token, peerId, publicKey, frame)
                if (decryptResult.content == null) {
                    ws.close(WsCloseCode.POLICY_VIOLATION, "invalid_request: $peerId")
                    return@webSocket
                }
                authenticated = true
                PeerStatusManager.setOnline(peerId, true)
                ws.sendText("ok")
            }
        } catch (ex: Exception) {
            LogCat.e("status ws: $ex")
        } finally {
            if (authenticated) {
                PeerStatusManager.disconnected(peerId)
            }
        }
    }

    webSocket("/") { ws, call ->
        val q = call.queryParamStrings()
        val clientId = q["cid"]?.firstOrNull() ?: ""
        if (clientId.isEmpty()) {
            LogCat.e("ws: `cid` is missing")
            ws.close(WsCloseCode.POLICY_VIOLATION, "`cid` is missing")
            return@webSocket
        }

        val sessionHandle = WsSessionAsHandle(
            id = TimeHelper.nowMillis(),
            clientId = clientId,
            ws = ws,
        )
        try {
            while (true) {
                val frame = ws.receiveBinary() ?: break
                if (q["auth"]?.firstOrNull() == "1") {
                    handleLoginFrame(ws, call, clientId, frame, sessionHandle)
                } else {
                    handleSessionFrame(ws, clientId, frame, sessionHandle)
                }
            }
        } catch (ex: Exception) {
            LogCat.e("ws: $ex")
        } finally {
            LogCat.d("ws: remove session $clientId, ${sessionHandle.id}")
            HttpServerManager.wsSessions.removeAll { it.id == sessionHandle.id }
            setOnlineClientIds(HttpServerManager.wsSessions.map { it.clientId }.toSet())
        }
    }
}

/**
 * Handle a login frame (`?auth=1`). Validates the password (decrypted with
 * the password-derived token) and either requests 2FA confirmation or issues
 * a session token directly.
 */
private suspend fun handleLoginFrame(
    ws: WsSession,
    call: HttpCall,
    clientId: String,
    frame: ByteArray,
    sessionHandle: WsSessionAsHandle,
) {
    val clientIp = HttpServerManager.getClientIpForLogin(
        clientId,
        call.remoteHost,
    )
    val rateLimitKey = clientIp.ifEmpty { "cid:$clientId" }
    if (!HttpServerManager.tryAcquireLoginAttempt(rateLimitKey)) {
        LogCat.e("ws: too_many_login_attempts, key=$rateLimitKey")
        ws.close(WsCloseCode.TRY_AGAIN_LATER, "too_many_login_attempts")
        return
    }

    var r: AuthRequest? = null
    val hash = sha512(
        PasswordPreference.getAsync().encodeToByteArray(),
    )
    val token = HttpServerManager.hashToToken(hash)
    val decryptedBytes = chaCha20Decrypt(token, frame)
    if (decryptedBytes != null) {
        r = jsonDecode<AuthRequest>(decryptedBytes.decodeToString())
    }
    if (r?.password == hash) {
        val event = ConfirmToAcceptLoginEvent(sessionHandle, clientId, r)
        if (AuthTwoFactorPreference.getAsync()) {
            ws.sendBinary(
                chaCha20Encrypt(
                    token,
                    jsonEncode(AuthResponse(TempData.clientId, AuthStatus.PENDING)),
                ),
            )
            sendEvent(event)
        } else {
            coIO {
                HttpServerManager.respondTokenAsync(event, clientIp)
            }
        }
    } else {
        LogCat.e("ws: invalid_password")
        ws.close(WsCloseCode.TRY_AGAIN_LATER, "invalid_password")
    }
}

/**
 * Handle a session-register frame (no `?auth=1`). The frame is encrypted
 * with the cached token; on success the session is added to the active
 * WebSocket set so [com.ismartcoding.plain.web.websocket.WebSocketHelper]
 * can push events to it.
 */
private suspend fun handleSessionFrame(
    ws: WsSession,
    clientId: String,
    frame: ByteArray,
    sessionHandle: WsSessionAsHandle,
) {
    val token = HttpServerManager.tokenCache[clientId]
    val decryptedBytes = token?.let { chaCha20Decrypt(it, frame) }
    if (decryptedBytes != null) {
        LogCat.d("ws: add session ${sessionHandle.id}, ts: ${decryptedBytes.decodeToString()}")
        HttpServerManager.wsSessions.add(sessionHandle)
        setOnlineClientIds(HttpServerManager.wsSessions.map { it.clientId }.toSet())
    } else {
        LogCat.d("ws: invalid_request: $clientId")
        ws.close(WsCloseCode.TRY_AGAIN_LATER, "invalid_request")
    }
}
