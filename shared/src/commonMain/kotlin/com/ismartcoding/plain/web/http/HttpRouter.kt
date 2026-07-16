package com.ismartcoding.plain.web.http

/**
 * Route registration entry. The path may include `{param}` segments which
 * the platform router resolves into [HttpCall.pathParam] at dispatch time.
 */
data class RouteEntry(
    val method: HttpMethod,
    val path: String,
    val handler: suspend (HttpCall) -> Unit,
)

typealias HttpHandler = suspend (HttpCall) -> Unit

/**
 * Collects route handlers in commonMain. The platform layer (Ktor, SwiftNIO)
 * iterates [entries] and registers each one with the native router so the
 * business logic can stay in shared code.
 *
 * Path parameters use the `{name}` convention — Ktor and SwiftNIO both
 * support it natively, so no custom matcher is required.
 */
class HttpRouter {
    private val entries = mutableListOf<RouteEntry>()
    private val wsEntries = mutableListOf<WebSocketRouteEntry>()

    fun get(path: String, handler: HttpHandler) = add(HttpMethod.GET, path, handler)
    fun post(path: String, handler: HttpHandler) = add(HttpMethod.POST, path, handler)
    fun put(path: String, handler: HttpHandler) = add(HttpMethod.PUT, path, handler)
    fun delete(path: String, handler: HttpHandler) = add(HttpMethod.DELETE, path, handler)
    fun method(method: HttpMethod, path: String, handler: HttpHandler) = add(method, path, handler)

    /**
     * Register a WebSocket handler at [path]. The platform layer is
     * responsible for upgrading the HTTP request to a WebSocket session
     * and invoking [handler] with a [WsSession] and a lightweight [HttpCall]
     * (mostly populated with request headers and query parameters).
     */
    fun webSocket(
        path: String,
        handler: suspend (WsSession, HttpCall) -> Unit,
    ) {
        wsEntries.add(WebSocketRouteEntry(path, handler))
    }

    private fun add(method: HttpMethod, path: String, handler: HttpHandler) {
        entries.add(RouteEntry(method, path, handler))
    }

    fun entries(): List<RouteEntry> = entries.toList()

    /** WebSocket routes collected via [webSocket]. */
    fun webSocketEntries(): List<WebSocketRouteEntry> = wsEntries.toList()
}
