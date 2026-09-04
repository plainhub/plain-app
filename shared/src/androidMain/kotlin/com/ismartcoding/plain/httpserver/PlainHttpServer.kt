package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.httpserver.CorsPolicy
import com.ismartcoding.plain.httpserver.HttpRouteRegistry
import com.ismartcoding.plain.httpserver.http.HttpMethod
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.isPeerAccessiblePath
import com.ismartcoding.plain.httpserver.isDlnaPath
import com.ismartcoding.plain.httpserver.isSharePath
import com.ismartcoding.plain.lib.ktorserver.DefaultWebSocketServerSession
import com.ismartcoding.plain.lib.ktorserver.matchPathTemplate
import com.ismartcoding.plain.lib.ktorserver.websocket.DefaultWebSocketSession
import com.ismartcoding.plain.lib.ktorserver.websocket.PINGER_DISABLED
import com.ismartcoding.plain.lib.ktorserver.websocket.WebSocketSession
import com.ismartcoding.plain.lib.ktorserver.websocket.close
import com.ismartcoding.plain.lib.ktorserver.WebSockets
import com.ismartcoding.plain.lib.ktorserver.WebSocketUpgrade
import com.ismartcoding.plain.lib.ktorserver.core.application.ApplicationCall
import com.ismartcoding.plain.lib.ktorserver.core.application.PipelineCall
import com.ismartcoding.plain.lib.ktorserver.core.plugins.mutableOriginConnectionPoint
import com.ismartcoding.plain.lib.ktorserver.core.plugins.origin
import com.ismartcoding.plain.lib.ktorserver.core.request.path
import com.ismartcoding.plain.lib.ktorserver.core.response.header
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.response.respondBytes
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import com.ismartcoding.plain.lib.ktorserver.toServerSession
import io.ktor.util.cio.ChannelIOException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Job as CoroutineJob

/**
 * Direct request dispatch — replaces the former Ktor call pipeline + routing
 * tree. Per request, in order:
 *
 * 1. access gate (HEAD→GET rewrite, desktop-access check, X-Server-Time, CORS)
 * 2. route dispatch (flat commonMain route table → web assets → SPA fallback)
 * 3. GraphQL error fallback
 *
 * Unhandled errors fall through to the engine's 500 handler.
 */
object PlainHttpServer {

    private val mainGraphQL get() = HttpRouteRegistry.mainGraphQL
    private val commonRouter: HttpRouter get() = HttpRouteRegistry.router

    /**
     * Default WebSocket settings — identical to the former `install(WebSockets)`
     * configuration defaults.
     */
    private val webSockets = WebSockets(
        pingIntervalMillis = PINGER_DISABLED,
        timeoutMillis = 15_000L,
        maxFrameSize = Long.MAX_VALUE,
        masking = false,
    )

    /**
     * index.html is baked into the APK and never changes at runtime; read it
     * once instead of re-reading the resource on every SPA fallback hit.
     */
    @Volatile
    private var cachedIndexHtml: String? = null

    val requestHandler: suspend (PipelineCall) -> Unit = { call ->
        handleRequest(call)
    }

    private suspend fun handleRequest(call: PipelineCall) {
        // Serve HEAD requests through the matching GET route; the engine
        // suppresses the body while keeping Content-Length.
        if (call.request.local.method == io.ktor.http.HttpMethod.Head) {
            call.mutableOriginConnectionPoint.method = io.ktor.http.HttpMethod.Get
        }
        val method = HttpMethod(call.request.origin.method.value.uppercase())
        val path = call.request.path()

        // Peer-accessible routes (PeerGraphQL, /fs, /health, WS /status)
        // and DLNA routes (sender /media/{id}, NOTIFY /callback/cast;
        // receiver /description.xml, /AVTransport/*, /RenderingControl/*)
        // remain available when desktopAccessEnabled=false but
        // serviceEnabled=true. Main-UI routes are rejected here; the
        // authoritative check still lives in each route handler so BLE
        // RPC (which bypasses this intercept) is also covered.
        if (!TempData.desktopAccessEnabled.value && !isPeerAccessiblePath(method, path) && !isDlnaPath(method, path) && !isSharePath(method, path)) {
            call.respond(HttpStatusCode.NotFound)
            return
        }
        call.response.headers.append("X-Server-Time", System.currentTimeMillis().toString())

        try {
            if (!dispatch(call, method, path)) {
                call.respond(HttpStatusCode.NotFound)
            }
        } catch (e: GraphQLError) {
            // Catch GraphQL errors thrown during request execution and deliver
            // them to the client through the same encryption/bearer channel used
            // by the /graphql and /peer_graphql handlers. Non-GraphQL exceptions
            // are re-thrown so the engine returns a 500.
            val httpCall = KtorHttpCall(call, emptyMap())
            val sent = mainGraphQL.handleError(e, httpCall)
            if (!sent) {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }

    /**
     * Dispatches to business routes, WebSocket routes, web assets or the SPA
     * fallback. Returns false when nothing matched.
     */
    private suspend fun dispatch(call: PipelineCall, method: HttpMethod, path: String): Boolean {
        // Business routes (HTTP), matched against the flat commonMain table.
        val effectiveMethod = if (method == HttpMethod.HEAD) HttpMethod.GET else method
        for (entry in commonRouter.entries()) {
            if (entry.method != effectiveMethod) continue
            val params = matchPathTemplate(entry.path, path) ?: continue
            entry.handler(KtorHttpCall(call, params))
            return true
        }

        // WebSocket routes.
        if (method == HttpMethod.GET && call.request.headers[HttpHeaders.Connection]?.contains("Upgrade", ignoreCase = true) == true &&
            call.request.headers[HttpHeaders.Upgrade]?.equals("websocket", ignoreCase = true) == true
        ) {
            for (entry in commonRouter.webSocketEntries()) {
                val params = matchPathTemplate(entry.path, path) ?: continue
                call.upgradeWebSocket { entry.handler(KtorWsSession(this), KtorHttpCall(call, params)) }
                return true
            }
        }

        // Static web assets and the SPA fallback.
        return respondWeb(call, path)
    }

    /**
     * Serves classpath resources from "web/" with the same cache policy as the
     * former staticResources block: hashed build outputs under /assets/ are
     * immutable, everything else is not cacheable. Paths without an extension
     * fall back to index.html with the server-time boot script injected.
     */
    private suspend fun respondWeb(call: PipelineCall, path: String): Boolean {
        if (path.contains("..")) return false

        val resourcePath = "web" + path.trimEnd('/')
        if (path.contains('.') && resourcePath != "web") {
            val bytes = classLoader(call).getResourceAsStream(resourcePath)?.readBytes() ?: return false
            call.response.headers.append(
                "Cache-Control",
                if (path.contains("/assets/")) {
                    "public, max-age=31536000, immutable"
                } else {
                    "no-cache, no-store, public"
                }
            )
            call.respondBytes(bytes, ContentType.defaultForFilePath(path))
            return true
        }

        // SPA route (no extension) → serve index.html with injected server time.
        val html = cachedIndexHtml ?: classLoader(call).getResourceAsStream("web/index.html")
            ?.bufferedReader()?.readText()?.also { cachedIndexHtml = it } ?: return false
        val injected = html.replace(
            "<head>",
            "<head><script>window.__SERVER_TIME__=${System.currentTimeMillis()}</script>"
        )
        call.respondText(injected, ContentType.Text.Html)
        return true
    }

    private fun classLoader(call: ApplicationCall): ClassLoader = call.application.environment.classLoader

    /**
     * Performs the WebSocket upgrade and hands the default (ping/pong aware)
     * session to [handler] — replaces the former `webSocket {}` route builder.
     */
    @OptIn(io.ktor.utils.io.InternalAPI::class)
    private suspend fun ApplicationCall.upgradeWebSocket(handler: suspend DefaultWebSocketServerSession.() -> Unit) {
        val block: suspend WebSocketSession.() -> Unit = {
            val session = DefaultWebSocketSession(
                this,
                webSockets.pingIntervalMillis,
                webSockets.timeoutMillis,
                webSockets.channelsConfig,
            )
            session.start(this@upgradeWebSocket.attributes[WebSockets.EXTENSIONS_KEY])
            try {
                val serverSession = session.toServerSession(this@upgradeWebSocket)
                handler(serverSession)
                session.close()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (io: ChannelIOException) {
                throw io
            }
            session.coroutineContext[CoroutineJob]!!.join()
        }
        respond(WebSocketUpgrade(this, webSockets, protocol = null, installExtensions = true, handle = block))
    }
}
