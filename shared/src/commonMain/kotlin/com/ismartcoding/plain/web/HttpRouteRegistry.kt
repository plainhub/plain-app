package com.ismartcoding.plain.web

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.web.graphql.MainGraphQLService
import com.ismartcoding.plain.web.graphql.PeerGraphQLService
import com.ismartcoding.plain.web.http.HttpCall
import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpRouter
import com.ismartcoding.plain.web.http.HttpStatus
import com.ismartcoding.plain.web.http.RouteEntry
import com.ismartcoding.plain.web.routes.addDlnaRoutes
import com.ismartcoding.plain.web.routes.addFilesRoutes
import com.ismartcoding.plain.web.routes.addGraphQLRoutes
import com.ismartcoding.plain.web.routes.addSystemRoutes
import com.ismartcoding.plain.web.routes.addUploadRoutes
import com.ismartcoding.plain.web.routes.addWebSocketRoutes
import com.ismartcoding.plain.web.routes.addZipRoutes

/**
 * Shared HTTP route registry built once per process and dispatch from both
 * the platform HTTP server (Ktor on Android, SwiftNIO on iOS future) and
 * the BLE [com.ismartcoding.plain.ble.server.HTTPServiceHandler].
 *
 * All business-logic routes live in commonMain and are collected into
 * [router]. The GraphQL services ([mainGraphQL], [peerGraphQL]) are also
 * shared so the BLE RPC channel can dispatch `/graphql` and `/peer_graphql`
 * requests through the same code path as the HTTP server.
 */
object HttpRouteRegistry {
    val mainGraphQL: MainGraphQLService by lazy { MainGraphQLService.create() }
    val peerGraphQL: PeerGraphQLService by lazy { PeerGraphQLService.create() }

    val router: HttpRouter by lazy {
        HttpRouter().apply {
            addSystemRoutes()
            addUploadRoutes()
            addFilesRoutes()
            addZipRoutes()
            addDlnaRoutes()
            addGraphQLRoutes(mainGraphQL, peerGraphQL)
            addWebSocketRoutes()
        }
    }

    /**
     * Find the [RouteEntry] matching [method] + [path] and invoke its handler
     * against [call]. Path parameters (`{name}`) are resolved into
     * [call.pathParam] via the [BleHttpCall] adapter — for platform
     * [HttpCall] implementations the params are already populated by the
     * platform router, so this function only does the lookup.
     *
     * Returns `true` when a route matched, `false` when no match was found
     * (in which case the caller is responsible for sending a 404).
     */
    suspend fun dispatch(method: HttpMethod, path: String, call: HttpCall): Boolean {
        val entry = matchRoute(method, path) ?: run {
            LogCat.d("HttpRouteRegistry: no route for $method $path")
            return false
        }
        entry.handler(call)
        return true
    }

    /**
     * Locate the [RouteEntry] for [method] + [path], returning the matched
     * path parameters (possibly empty) or `null` when no route matches.
     * Exposed so callers that need to populate path params on the [HttpCall]
     * adapter (e.g. [BleHttpCall]) can do so before dispatch.
     */
    fun matchRoute(method: HttpMethod, path: String): RouteEntry? {
        return router.entries().firstOrNull { entry ->
            entry.method == method && matchPath(entry.path, path) != null
        }
    }

    /**
     * Match a route pattern (`/media/{id}`) against a concrete [path]
     * (`/media/abc`). Returns the resolved path parameters or `null` when
     * the segments don't line up.
     */
    fun matchPath(pattern: String, path: String): Map<String, String>? {
        val patternParts = pattern.split("/").filter { it.isNotEmpty() }
        val pathParts = path.split("/").filter { it.isNotEmpty() }
        if (patternParts.size != pathParts.size) return null
        val params = mutableMapOf<String, String>()
        for (i in patternParts.indices) {
            val p = patternParts[i]
            val v = pathParts[i]
            if (p.startsWith("{") && p.endsWith("}")) {
                params[p.substring(1, p.length - 1)] = v
            } else if (p != v) {
                return null
            }
        }
        return params
    }

    /** HTTP status used by [dispatch] callers when no route matches. */
    const val NOT_FOUND_STATUS = HttpStatus.NOT_FOUND
}
