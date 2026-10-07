package com.ismartcoding.plain.httpserver.http

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
 * Collects the remaining host routes dispatched by RustHttpHost.
 * HttpRouteRegistry resolves path parameters for the loopback adapter.
 */
class HttpRouter {
    private val entries = mutableListOf<RouteEntry>()
    private val wsEntries = mutableListOf<WebSocketRouteEntry>()

    fun get(path: String, handler: HttpHandler) = add(HttpMethod.GET, path, handler)
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
