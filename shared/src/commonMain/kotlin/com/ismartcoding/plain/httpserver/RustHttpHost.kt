package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.api.ContentApiSession
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpStatus
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

internal object RustHttpHost {
    private val client by lazy { createPlainHttpClient(PlainHttpClientSpec.HttpHost) }
    fun start(scope: CoroutineScope, session: ContentApiSession, id: String) {
        scope.launch {
            try {
                client.webSocket(session.baseUrl.replace("http://", "ws://") + "/http_host/$id", session.headers()) { socket ->
                    val frame = socket.incoming.receiveCatching().getOrNull() ?: return@webSocket
                    val metadata = Json.parseToJsonElement(checkNotNull(frame.text)).jsonObject
                    check(metadata["kind"]?.jsonPrimitive?.content == "request")
                    val call = RustHttpCall(socket, metadata)
                    try { dispatch(socket, call) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Throwable) {
                        if (call.responded) throw error
                        if (error is GraphQLError) {
                            if (!HttpRouteRegistry.mainGraphQL.handleError(error, call)) call.respondNoBody(HttpStatus.UNAUTHORIZED)
                        } else {
                            LogCat.e("Rust HTTP handler failed", error)
                            call.respondNoBody(HttpStatus.INTERNAL_SERVER_ERROR)
                        }
                    }
                    if (!call.responded) call.respondNoBody(HttpStatus.NO_CONTENT)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("Rust HTTP host stream ended: ${error.message}") }
        }
    }
    private suspend fun dispatch(socket: PlainWebSocketSession, call: RustHttpCall) {
        if (!UserPrefs.desktopAccess.value && !isPeerAccessiblePath(call.method, call.path) && !isDlnaPath(call.method, call.path) && !isSharePath(call.method, call.path)) {
            call.respondNoBody(HttpStatus.NOT_FOUND)
            return
        }
        call.responseHeader("X-Server-Time", TimeHelper.nowMillis().toString())
        call.responseHeader("Cross-Origin-Opener-Policy", CorsPolicy.crossOriginOpenerPolicy)
        call.responseHeader("Cross-Origin-Embedder-Policy", CorsPolicy.crossOriginEmbedderPolicy)
        val origin = call.header("Origin")
        if (origin != null) {
            val host = call.header("Host") ?: ""
            val sameOrigin = runCatching { val url = io.ktor.http.Url(origin); val authority = io.ktor.http.Url("${call.scheme}://$host"); url.protocol.name == call.scheme && url.host.equals(authority.host, true) && url.port == authority.port }.getOrDefault(false)
            if (!sameOrigin && !UserPrefs.allowAnyHost.value && !isDebugBuild()) {
                call.respondNoBody(HttpStatus.FORBIDDEN)
                return
            }
            call.responseHeader("Access-Control-Allow-Origin", origin)
            call.responseHeader("Vary", "Origin")
            if (call.method == HttpMethod.OPTIONS) {
                call.responseHeader("Access-Control-Allow-Methods", CorsPolicy.allowedMethods)
                call.responseHeader("Access-Control-Max-Age", CorsPolicy.maxAgeSeconds)
                CorsPolicy.filterAllowedRequestHeaders(call.header("Access-Control-Request-Headers"))?.let { call.responseHeader("Access-Control-Allow-Headers", it) }
                call.respondNoBody(HttpStatus.OK)
                return
            }
        }
        if (call.webSocket) {
            val entry = HttpRouteRegistry.router.webSocketEntries().firstOrNull { it.path == call.path }
            if (entry == null) { call.respondNoBody(HttpStatus.NOT_FOUND); return }
            call.upgrade()
            val session = RustWsSession(socket, call.remoteHost)
            try { entry.handler(session, call) } finally { session.close() }
        } else if (!HttpRouteRegistry.dispatch(call.method, call.path, call)) {
            if (call.method != HttpMethod.GET || !serveRustWebAsset(call)) call.respondNoBody(HttpStatus.NOT_FOUND)
        }
    }
}
