package com.ismartcoding.plain.webserver

import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.WebSocketRouteEntry
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.method
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.websocket.webSocket

/**
 * Registers every route collected by the commonMain [router] with the Ktor
 * [Route] receiver. Each handler is wrapped in a [KtorHttpCall] so the
 * shared business logic sees only the platform-agnostic [HttpCall] interface.
 *
 * Path parameters (`{name}`) are resolved from `call.parameters` and forwarded
 * to the adapter so [HttpCall.pathParam] returns the expected value.
 *
 * In Ktor 3.x the route handler receiver is [RoutingContext] and the request
 * is exposed via [RoutingContext.call] (a `RoutingCall` that extends
 * `ApplicationCall`).
 */
fun Route.registerCommonRoutes(router: HttpRouter) {
    router.entries().forEach { entry ->
        val handler: suspend RoutingContext.() -> Unit = {
            val params = extractPathParams(entry.path, call)
            val httpCall = KtorHttpCall(call, params)
            entry.handler(httpCall)
        }
        when (entry.method) {
            HttpMethod.GET -> get(entry.path, handler)
            HttpMethod.POST -> post(entry.path, handler)
            HttpMethod.PUT -> put(entry.path, handler)
            HttpMethod.DELETE -> delete(entry.path, handler)
            else -> {
                val ktorMethod = KtorHttpMethod.parse(entry.method.name)
                route(entry.path, ktorMethod) {
                    handle(handler)
                }
            }
        }
    }

    // WebSocket routes — Ktor upgrades the connection and hands the session
    // to the commonMain handler via the KtorWsSession adapter.
    router.webSocketEntries().forEach { entry ->
        webSocket(entry.path) {
            val wsCall = KtorHttpCall(this.call, emptyMap())
            val wsSession = KtorWsSession(this)
            entry.handler(wsSession, wsCall)
        }
    }
}

/**
 * Convenience overload for an [Application] that registers all routes
 * (HTTP + WebSocket) collected in [router] inside a `routing { }` block.
 */
fun Application.registerCommonRoutes(router: HttpRouter) {
    routing {
        registerCommonRoutes(router)
    }
}

/**
 * Single-pass extraction of `{name}` path parameters from [pattern]. Each
 * name is looked up in [call] parameters and collected into a map forwarded
 * to [KtorHttpCall] so [HttpCall.pathParam] returns the expected value.
 */
private fun extractPathParams(pattern: String, call: ApplicationCall): Map<String, String> {
    val params = mutableMapOf<String, String>()
    var start = -1
    for (i in pattern.indices) {
        when (val c = pattern[i]) {
            '{' -> if (start == -1) start = i + 1
            '}' -> if (start != -1) {
                val name = pattern.substring(start, i)
                call.parameters[name]?.let { params[name] = it }
                start = -1
            }
        }
    }
    return params
}
