package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.web.HttpRouteRegistry
import com.ismartcoding.plain.web.http.HttpMethod
import com.ismartcoding.plain.web.http.HttpStatus
import com.ismartcoding.plain.web.http.WsSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Singleton entry point that Swift calls to process each HTTP request and
 * WebSocket upgrade. Exposed to Swift via the PlainShared framework header.
 *
 * Swift creates an [IosRequestContext], populates it with request data,
 * then calls [processHttpRequest]. When the suspend function returns, Swift
 * reads the response fields back from the context and writes them to the
 * SwiftNIO channel.
 *
 * For WebSocket upgrades, Swift creates a [NioWsSession] plus a lightweight
 * [IosRequestContext] carrying the request headers/query params, then calls
 * [processWebSocket]. The route handler runs in a coroutine launched here;
 * Swift drives the session by calling [NioWsSession.onBinaryFrame] /
 * [NioWsSession.onClose] as frames arrive.
 */
object IosRequestProcessor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Process a single HTTP request through the shared [HttpRouteRegistry].
     *
     * This is a `suspend fun` so Kotlin/Native exposes it to Swift as a
     * function with a completion handler:
     *
     * ```swift
     * IosRequestProcessor.shared.processHttpRequest(data: ctx) {
     *     // response is ready in ctx
     * }
     * ```
     */
    suspend fun processHttpRequest(ctx: IosRequestContext) {
        try {
            // Short-circuit /health and /health_check so they work even when web is disabled.
            if (ctx.path == "/health" || ctx.path == "/health_check") {
                ctx.responseStatus = HttpStatus.OK
                ctx.setResponseBody(getOwnPackageName().encodeToByteArray())
                ctx.setResponseHeader("Content-Type", "text/plain")
                return
            }

            // Match the web-enabled gate used by Ktor's intercept pipeline.
            if (!TempData.webEnabled.value) {
                ctx.responseStatus = HttpStatus.NOT_FOUND
                return
            }

            val method = HttpMethod(ctx.method.uppercase())
            val matched = HttpRouteRegistry.matchRoute(method, ctx.path)
            if (matched != null) {
                val params = HttpRouteRegistry.matchPath(matched.path, ctx.path) ?: emptyMap()
                val call = NioHttpCall(ctx)
                call.setPathParams(params)
                try {
                    matched.handler(call)
                } catch (e: GraphQLError) {
                    val sent = HttpRouteRegistry.mainGraphQL.handleError(e, call)
                    if (!sent) {
                        ctx.responseStatus = HttpStatus.UNAUTHORIZED
                    }
                } catch (e: Throwable) {
                    LogCat.e("IosRequestProcessor: route handler threw: ${e.message}")
                    ctx.responseStatus = HttpStatus.INTERNAL_SERVER_ERROR
                }
                return
            }

            // No API route matched → try static web assets (SPA fallback).
            if (tryServeStaticFile(ctx)) return

            ctx.responseStatus = HttpStatus.NOT_FOUND
            ctx.setResponseBody("Not Found".encodeToByteArray())
            ctx.setResponseHeader("Content-Type", "text/plain")
        } catch (e: Throwable) {
            LogCat.e("IosRequestProcessor: unhandled exception: ${e.message}")
            ctx.responseStatus = HttpStatus.INTERNAL_SERVER_ERROR
            ctx.setResponseBody("Internal Server Error".encodeToByteArray())
            ctx.setResponseHeader("Content-Type", "text/plain")
        }
    }

    /**
     * Process a WebSocket upgrade. Swift has already completed the HTTP 101
     * switching-protocols handshake and created a [NioWsSession] wired to the
     * SwiftNIO channel. This function launches a coroutine that runs the
     * commonMain WebSocket route handler; Swift feeds inbound frames by
     * calling [NioWsSession.onBinaryFrame].
     */
    fun processWebSocket(
        path: String,
        session: NioWsSession,
        ctx: IosRequestContext,
    ) {
        scope.launch {
            try {
                val cleanPath = path.substringBefore("?")
                val wsEntries = HttpRouteRegistry.router.webSocketEntries()
                val entry = wsEntries.firstOrNull { it.path == cleanPath }
                if (entry == null) {
                    session.close(1008, "no route")
                    return@launch
                }
                // Build a lightweight HttpCall from the upgrade request context
                // so the route handler can read query params and headers.
                val call = NioHttpCall(ctx)
                entry.handler(session, call)
            } catch (e: Throwable) {
                LogCat.e("IosRequestProcessor: WebSocket handler threw: ${e.message}")
            } finally {
                // Ensure the session is marked closed on the Kotlin side even
                // if the route handler exits without calling close().
                session.onClose()
            }
        }
    }

    /**
     * Attempt to serve a static web asset from the iOS app bundle.
     *
     * Looks up [path] under the `web/` bundle resource directory. For paths
     * without an extension (SPA routes) the function falls back to
     * `index.html` with the server-time injection used on Android.
     *
     * Returns `true` when a file was served (response fields on [ctx] are
     * populated); `false` when no matching resource exists.
     */
    private fun tryServeStaticFile(ctx: IosRequestContext): Boolean {
        val resourcePath = ctx.path.removePrefix("/").removeSuffix("/")
        if (resourcePath.isEmpty()) {
            serveIndexHtml(ctx)
            return true
        }
        // Only serve files with an extension directly; extensionless paths are SPA routes.
        if (!resourcePath.contains('.')) {
            serveIndexHtml(ctx)
            return true
        }
        val filePath = IosWebAssets.resolve(resourcePath)
        if (filePath != null) {
            ctx.responseStatus = HttpStatus.OK
            ctx.setResponseFilePath(filePath, IosWebAssets.contentTypeFor(resourcePath), null)
            return true
        }
        return false
    }

    private fun serveIndexHtml(ctx: IosRequestContext) {
        val indexPath = IosWebAssets.resolve("index.html")
        if (indexPath != null) {
            ctx.responseStatus = HttpStatus.OK
            ctx.setResponseHeader("Content-Type", "text/html")
            ctx.setResponseHeader("Cache-Control", "no-cache, no-store")
            ctx.setResponseFilePath(indexPath, "text/html", null)
        } else {
            ctx.responseStatus = HttpStatus.NOT_FOUND
            ctx.setResponseBody("Web UI not bundled".encodeToByteArray())
        }
    }
}
