package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerChatParser
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.lib.JsonHelper.jsonDecode
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.dispatchScreenMirrorControl
import com.ismartcoding.plain.platform.resetScreenMirrorTouchStream
import com.ismartcoding.plain.platform.sha512
import com.ismartcoding.plain.preferences.AuthTwoFactorPreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.httpserver.AuthRequest
import com.ismartcoding.plain.httpserver.AuthResponse
import com.ismartcoding.plain.httpserver.AuthStatus
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.requiresLoginConfirmation
import com.ismartcoding.plain.httpserver.setOnlineClientIds
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.WsCloseCode
import com.ismartcoding.plain.httpserver.http.WsSession
import com.ismartcoding.plain.httpserver.WsSessionHandle
import com.ismartcoding.plain.platform.onWebSocketSessionStarted

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
        // WS `/` is the Main-UI login + event-push channel — it requires
        // `canDesktopAccess()`. WS `/status` (peer heartbeat) is separate and
        // only needs `serviceEnabled`. This check is authoritative: iOS
        // `processWebSocket` has no platform-level gate, so without this the
        // login WS would be reachable with desktop access disabled.
        if (!TempData.canDesktopAccess()) {
            ws.close(WsCloseCode.POLICY_VIOLATION, "desktop_access_disabled")
            return@webSocket
        }
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
            // LogCat.d("ws: remove session $clientId, ${sessionHandle.id}")
            HttpServerManager.wsSessions.removeAll { it.id == sessionHandle.id }
            setOnlineClientIds(HttpServerManager.wsSessions.map { it.clientId }.toSet())
            // The last client dropping mid-stroke would leave a dangling
            // injected touch — force-release the stream injector.
            if (HttpServerManager.wsSessions.isEmpty()) {
                resetScreenMirrorTouchStream()
            }
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
        val event = ConfirmToAcceptLoginEvent(sessionHandle, clientId, r, r.ecdhPublicKey)
        if (requiresLoginConfirmation(r, AuthTwoFactorPreference.getAsync())) {
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
 * WebSocket set so [com.ismartcoding.plain.httpserver.websocket.WebSocketHelper]
 * can push events to it. Returns true when the session was newly registered.
 */
private suspend fun handleSessionFrame(
    ws: WsSession,
    clientId: String,
    frame: ByteArray,
    sessionHandle: WsSessionAsHandle,
): Boolean {
    val token = HttpServerManager.tokenCache.get(clientId)
    val decryptedBytes = token?.let { chaCha20Decrypt(it, frame) }
    if (decryptedBytes != null) {
        if (HttpServerManager.wsSessions.add(sessionHandle)) {
            LogCat.d("ws: add session ${sessionHandle.id}, ts: ${decryptedBytes.decodeToString()}")
            setOnlineClientIds(HttpServerManager.wsSessions.map { it.clientId }.toSet())
            onWebSocketSessionStarted()
            return true
        }
        // Already-registered session: this is an upstream control frame.
        handleUpstreamControl(decryptedBytes)
        return true
    } else {
//        LogCat.d("ws: invalid_request: $clientId")
        ws.close(WsCloseCode.TRY_AGAIN_LATER, "invalid_request")
        return false
    }
}

// Upstream control protocol (mirrors plain-cast's screen-mirror control):
// every post-registration frame is ChaCha20-encrypted with the session token.
// The decrypted payload is either a binary touch frame (first byte 0x54) or a
// JSON-encoded ScreenMirrorControlInput for cold-path actions (BACK/HOME/SCROLL/…).
private const val TOUCH_FRAME_MAGIC = 0x54
private const val TOUCH_ACTION_DOWN = 0
private const val TOUCH_ACTION_MOVE = 1

/**
 * Dispatch one decrypted upstream control payload. Touch frames are decoded
 * sample-by-sample and injected on the dedicated touch thread (hot path —
 * no main-thread hop); failures are dropped silently, mirroring the
 * accessibility-off behavior of the GraphQL mutation.
 */
private fun handleUpstreamControl(plain: ByteArray) {
    if (plain.isEmpty()) return
    if (plain[0].toInt() and 0xff == TOUCH_FRAME_MAGIC) {
        decodeTouchFrame(plain).forEach { dispatchScreenMirrorControl(it) }
    } else {
        val input = try {
            jsonDecode<ScreenMirrorControlInput>(plain.decodeToString())
        } catch (ex: Exception) {
            LogCat.w("ws control: bad json (${ex.message})")
            return
        }
        dispatchScreenMirrorControl(input)
    }
}

/**
 * Binary touch-frame layout (little-endian), same wire format as plain-cast:
 * u8 magic=0x54 | u8 count | u16 streamId (always 0 here) |
 * count × [u8 action | u8 pointerId | u16 x | u16 y | u16 dtMs]
 * internal for TouchFrameDecodeTest (wire contract lock).
 */
internal fun decodeTouchFrame(bytes: ByteArray): List<ScreenMirrorControlInput> {
    if (bytes.size < 4 || (bytes[0].toInt() and 0xff) != TOUCH_FRAME_MAGIC) return emptyList()
    val count = bytes[1].toInt() and 0xff
    if (count == 0 || bytes.size < 4 + count * 8) return emptyList()
    val inputs = ArrayList<ScreenMirrorControlInput>(count)
    var offset = 4
    repeat(count) {
        val action = bytes[offset].toInt() and 0xff
        val pointerId = bytes[offset + 1].toInt() and 0xff
        val x = u16LE(bytes, offset + 2) / 65535f
        val y = u16LE(bytes, offset + 4) / 65535f
        // dtMs (offset + 6) is reserved for replay-accurate injection; the
        // injector paces segments by the wall clock at dispatch time.
        offset += 8
        val controlAction = when (action) {
            TOUCH_ACTION_DOWN -> ScreenMirrorControlAction.TOUCH_DOWN
            TOUCH_ACTION_MOVE -> ScreenMirrorControlAction.TOUCH_MOVE
            else -> ScreenMirrorControlAction.TOUCH_UP // 2 = UP, 3 = CANCEL (mapped to UP)
        }
        inputs.add(
            ScreenMirrorControlInput(
                action = controlAction, x = x, y = y, pointerId = pointerId,
            ),
        )
    }
    return inputs
}

private fun u16LE(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)
