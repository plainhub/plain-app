package com.ismartcoding.plain.webserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.isDebugBuild
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.web.HttpRouteRegistry
import com.ismartcoding.plain.web.http.HttpRouter
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.coroutineScope
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
        install(CachingHeaders) {
            options { _, outgoingContent ->
                when (outgoingContent.contentType?.withoutParameters()) {
                    ContentType.Text.CSS, ContentType.Application.JavaScript ->
                        CachingOptions(
                            CacheControl.MaxAge(maxAgeSeconds = 3600 * 24 * 30),
                        )

                    else -> null
                }
            }
        }

        install(CORS) {
            if (isDebugBuild()) {
                allowHost("*")
            } else {
                allowHost("localhost:3000")
                allowHost("127.0.0.1:3000")
            }
            allowHeadersPrefixed("c-")
        }

        install(ConditionalHeaders)
        install(WebSockets)
//        install(Compression) // this will slow down the download speed
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
            if (call.request.path() == "/health") {
                return@intercept
            }
            if (!TempData.webEnabled.value) {
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
                coroutineScope {
                    proceed()
                }
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
                cacheControl {
                    arrayListOf(
                        CacheControl.NoCache(CacheControl.Visibility.Public),
                        CacheControl.NoStore(CacheControl.Visibility.Public),
                    )
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
