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
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import com.ismartcoding.plain.lib.ktorserver.AutoHeadResponse
import com.ismartcoding.plain.lib.ktorserver.ConditionalHeaders
import com.ismartcoding.plain.lib.ktorserver.ContentNegotiation
import com.ismartcoding.plain.lib.ktorserver.PartialContent
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

object HttpModule {

    /**
     * Shared GraphQL services and the commonMain router live in
     * [HttpRouteRegistry] so the BLE RPC channel can dispatch to the same
     * route handlers without re-building the schema or duplicating the route
     * table.
     */
    private val mainGraphQL get() = HttpRouteRegistry.mainGraphQL
    private val commonRouter: HttpRouter get() = HttpRouteRegistry.router

    val module: Application.() -> Unit = {
        install(CORS) {
            if (TempData.allowAnyHost.value) {
                anyHost()
            }
            CorsPolicy.allowedHeaderPrefixes.forEach { allowHeadersPrefixed(it) }
        }

        install(ConditionalHeaders)
        install(WebSockets)
        install(ForwardedHeaders)
        install(PartialContent)
        install(AutoHeadResponse)
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                },
            )
        }

        intercept(ApplicationCallPipeline.Plugins) {
            val method = HttpMethod(call.request.local.method.value.uppercase())
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
                return@intercept finish()
            }
            call.response.headers.append("X-Server-Time", System.currentTimeMillis().toString())
        }

        // Catch GraphQL errors thrown during request execution and deliver
        // them to the client through the same encryption/bearer channel used
        // by the /graphql and /peer_graphql handlers. Non-GraphQL exceptions
        // are re-thrown so Ktor returns a 500.
        intercept(ApplicationCallPipeline.Monitoring) {
            try {
                proceed()
            } catch (e: Throwable) {
                if (e is GraphQLError) {
                    val httpCall = KtorHttpCall(call, emptyMap())
                    val sent = mainGraphQL.handleError(e, httpCall)
                    if (!sent) {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                } else {
                    throw e
                }
            }
        }

        routing {
            // SPA: serve all resources from classpath "web/", inject __SERVER_TIME__ into index.html
            // for every non-file path (no extension) so the Vue SPA can boot with a clock-sync value.
            staticResources("/", "web", index = null) {
                cacheControl { url ->
                    if (url.path.contains("/assets/")) {
                        // Hashed build outputs are immutable
                        arrayListOf(
                            CacheControl.MaxAge(
                                maxAgeSeconds = 3600 * 24 * 365,
                                visibility = CacheControl.Visibility.Public,
                            ),
                        )
                    } else {
                        arrayListOf(
                            CacheControl.NoCache(CacheControl.Visibility.Public),
                            CacheControl.NoStore(CacheControl.Visibility.Public),
                        )
                    }
                }
                fallback { requestedPath, call ->
                    if (requestedPath.contains('.')) {
                        // Real static asset that doesn't exist → 404
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        // SPA route (no extension) → serve index.html with injected server time
                        val classLoader = call.application.environment.classLoader
                        val html = classLoader.getResourceAsStream("web/index.html")
                            ?.bufferedReader()?.readText() ?: ""
                        val injected = html.replace(
                            "<head>",
                            "<head><script>window.__SERVER_TIME__=${System.currentTimeMillis()}</script>"
                        )
                        call.respondText(injected, ContentType.Text.Html)
                    }
                }
            }

            // All business-logic routes (HTTP + WebSocket) live in commonMain
            // and are dispatched through KtorHttpCall/KtorWsSession so shared
            // code never touches Ktor APIs directly.
            registerCommonRoutes(commonRouter)
        }
    }

}
