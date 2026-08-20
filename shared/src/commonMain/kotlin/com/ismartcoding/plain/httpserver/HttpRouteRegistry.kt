package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.http.RouteEntry
import com.ismartcoding.plain.httpserver.routes.addDlnaRoutes
import com.ismartcoding.plain.httpserver.routes.addFilesRoutes
import com.ismartcoding.plain.httpserver.routes.addGraphQLRoutes
import com.ismartcoding.plain.httpserver.routes.addNearbyRoutes
import com.ismartcoding.plain.httpserver.routes.addSystemRoutes
import com.ismartcoding.plain.httpserver.routes.addUploadRoutes
import com.ismartcoding.plain.httpserver.routes.addWebSocketRoutes
import com.ismartcoding.plain.httpserver.routes.addZipRoutes

/**
 * Peer-to-peer communication channels that remain accessible when
 * `desktopAccessEnabled=false` but `serviceEnabled=true`.
 *
 * These routes serve paired-peer traffic (PeerGraphQL, peer file download,
 * peer status heartbeat) plus the health probe. Main-UI routes
 * (MainGraphQL `/graphql`, `/init`, WS `/`, `/upload`, `/zip/dir`, `/zip/files`, `/proxyfs`)
 * are NOT listed here — they require `canDesktopAccess()`.
 *
 * Used by the Android/iOS platform intercepts as an early-reject whitelist
 * so Main-UI requests are turned away before route dispatch, while peer
 * traffic flows through to the commonMain handlers. The authoritative
 * access-control checks live inside each route handler (covers BLE RPC,
 * which has no platform intercept).
 */
private val PEER_ACCESSIBLE_PATHS: Set<Pair<HttpMethod, String>> = setOf(
    HttpMethod.POST to "/peer_graphql",
    HttpMethod.POST to "/nearby",
    HttpMethod.GET to "/fs",
    HttpMethod.GET to "/health",
    HttpMethod.GET to "/status", // WebSocket upgrade for peer status heartbeat
)

/**
 * Returns `true` when [method] + [path] is a peer-accessible route that must
 * remain available even when `desktopAccessEnabled=false` (as long as
 * `serviceEnabled=true`). See [PEER_ACCESSIBLE_PATHS].
 */
fun isPeerAccessiblePath(method: HttpMethod, path: String): Boolean {
    val cleanPath = path.substringBefore("?")
    return PEER_ACCESSIBLE_PATHS.contains(method to cleanPath)
}

/**
 * DLNA sender paths that must remain accessible when
 * `desktopAccessEnabled=false` (as long as `serviceEnabled=true`).
 *
 * `/media/{id}` serves media files to remote DLNA renderers (the TV pulls the
 * stream from here); `NOTIFY /callback/cast` receives renderer event callbacks.
 * Both are required for casting (sender mode) and have no separate feature
 * toggle — they are available whenever the service is running, independently
 * of the desktop-access gate (web UI) and the DLNA receiver toggle.
 *
 * `/media/{id}` uses a path parameter so it is matched by prefix. Used by the
 * platform HTTP intercepts to bypass the `desktopAccessEnabled` check; the
 * HTTP server itself only runs while `serviceEnabled=true`.
 */
fun isDlnaSenderPath(method: HttpMethod, path: String): Boolean {
    val cleanPath = path.substringBefore("?")
    if (method == HttpMethod.GET && cleanPath.startsWith("/media/")) return true
    if (method.name == "NOTIFY" && cleanPath == "/callback/cast") return true
    return false
}

/**
 * DLNA receiver paths that must remain accessible when
 * `desktopAccessEnabled=false` (as long as `serviceEnabled=true`).
 *
 * `/description.xml` is the device description document fetched by remote
 * senders (control points) during SSDP discovery; the `/AVTransport/...` and
 * `/RenderingControl/...` paths carry the SOAP control + GENA event traffic
 * that drives playback on this device acting as a MediaRenderer.
 *
 * The authoritative access check (`TempData.canDLNAAccess()`, which requires
 * the DLNA receiver toggle + service) lives in the receiver route handler;
 * this function only decides whether to bypass the `desktopAccessEnabled`
 * gate at the platform HTTP intercept so the request can reach that handler.
 */
fun isDlnaReceiverPath(method: HttpMethod, path: String): Boolean {
    val cleanPath = path.substringBefore("?")
    if (method == HttpMethod.GET && cleanPath == "/description.xml") return true
    if (cleanPath.startsWith("/AVTransport/") || cleanPath.startsWith("/RenderingControl/")) return true
    return false
}

/**
 * Convenience aggregate of [isDlnaSenderPath] and [isDlnaReceiverPath] for
 * the platform HTTP intercepts: any DLNA route bypasses the
 * `desktopAccessEnabled` gate (its own handler enforces the real toggle).
 */
fun isDlnaPath(method: HttpMethod, path: String): Boolean =
    isDlnaSenderPath(method, path) || isDlnaReceiverPath(method, path)

/**
 * Share-link routes that must remain accessible when
 * `desktopAccessEnabled=false` (as long as `serviceEnabled=true`). A shared
 * file link (`/s/<shared_id>` page → `/guest_graphql` + `/fs` + `/zip/dir`) is
 * meant to work as a standalone page without the desktop/web UI being enabled;
 * each handler enforces `serviceEnabled` and the share's active state itself.
 */
private val SHARE_PATHS: Set<Pair<HttpMethod, String>> = setOf(
    HttpMethod.POST to "/guest_graphql",
    HttpMethod.GET to "/fs",
    HttpMethod.GET to "/zip/dir",
)

/**
 * Returns `true` when [method] + [path] is a share-link route that must bypass
 * the `desktopAccessEnabled` gate at the platform HTTP intercept. See
 * [SHARE_PATHS].
 */
fun isSharePath(method: HttpMethod, path: String): Boolean {
    val cleanPath = path.substringBefore("?")
    return SHARE_PATHS.contains(method to cleanPath)
}

/**
 * Shared HTTP route registry built once per process and dispatch from both
 * the platform HTTP server (Ktor on Android, SwiftNIO on iOS future) and
 * the BLE [com.ismartcoding.plain.ble.server.HttpServiceHandler].
 *
 * All business-logic routes live in commonMain and are collected into
 * [router]. The GraphQL services ([mainGraphQL], [peerGraphQL]) are also
 * shared so the BLE RPC channel can dispatch `/graphql` and `/peer_graphql`
 * requests through the same code path as the HTTP server.
 */
object HttpRouteRegistry {
    val mainGraphQL: MainGraphQLService by lazy { MainGraphQLService.create() }
    val peerGraphQL: PeerGraphQLService by lazy { PeerGraphQLService.create() }
    val guestGraphQL: GuestGraphQLService by lazy { GuestGraphQLService.create() }

    val router: HttpRouter by lazy {
        HttpRouter().apply {
            addSystemRoutes()
            addNearbyRoutes()
            addUploadRoutes()
            addFilesRoutes()
            addZipRoutes()
            addDlnaRoutes()
            addGraphQLRoutes()
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
}
