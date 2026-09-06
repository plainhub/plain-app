package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.httpserver.http.HttpRouter
import com.ismartcoding.plain.httpserver.routes.addSystemRoutes
import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.NettyApplicationEngine
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import com.ismartcoding.plain.lib.ktorserver.core.routing.get
import com.ismartcoding.plain.lib.ktorserver.core.routing.routing
import com.ismartcoding.plain.platform.stopHttpServerCoreAsync
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression lock for the 2026-09 stop-latency bug: stopping used to GET
 * /shutdown and run the engine teardown inside the route's call coroutine, so
 * the engine's disposeAndJoin waited on the very handler performing the stop —
 * every stop burned the full 5s engine shutdown timeout (5.1s measured on
 * device). These tests pin the two design invariants against a real Netty
 * engine:
 *
 * 1. The in-process stop orchestration must not depend on the server it stops
 *    (no self HTTP round-trip) and must finish well under the 5s timeout.
 * 2. The /shutdown route must answer immediately and complete its teardown
 *    outside the call coroutine (OFF within the deadline, not after 5s).
 */
class HttpServerStopLifecycleTest {
    private var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var port: Int = 0

    @AfterTest
    fun tearDown() {
        httpServer?.stop(0, 500)
        httpServer = null
        engine = null
        HttpServerManager.serverState.value = HttpServerState.OFF
        HttpServerManager.httpServerError.value = ""
    }

    @Test
    fun inProcessStop_worksWithoutShutdownRoute_andBeatsEngineTimeout() {
        startServer {
            routing {
                get("/ping") { call.respondText("pong") }
            }
        }

        val start = System.currentTimeMillis()
        runBlocking { stopHttpServerCoreAsync() }
        val elapsed = System.currentTimeMillis() - start

        assertEquals(HttpServerState.OFF, HttpServerManager.serverState.value)
        assertNull(httpServer, "engine reference must be cleared after stop")
        assertTrue(elapsed < 3000, "stop took ${elapsed}ms — did the teardown move back into a call coroutine or a self-HTTP round-trip? (regression: 5.1s)")
        assertTrue(isPortClosed(), "port $port still accepts connections after stop")
    }

    @Test
    fun shutdownRoute_respondsImmediately_andTeardownCompletesOffHandler() {
        startServer { registerCommonRoutes(HttpRouter().apply { addSystemRoutes() }) }

        val client = HttpClient.newHttpClient()
        val start = System.currentTimeMillis()
        val response = client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:$port/shutdown")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        val responseMs = System.currentTimeMillis() - start

        assertEquals(410, response.statusCode(), "loopback /shutdown must be accepted (403 means the source-address guard regressed)")
        assertTrue(responseMs < 2000, "shutdown response took ${responseMs}ms — the response must not wait for the teardown")

        // The route launches teardown in an independent coroutine; it must land
        // OFF well before the 5s engine shutdown timeout (old self-join: ~5.1s).
        val deadline = System.currentTimeMillis() + 3000
        while (HttpServerManager.serverState.value != HttpServerState.OFF && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(HttpServerState.OFF, HttpServerManager.serverState.value, "server not OFF in time — teardown stalled (old bug: in-handler engine stop self-joined for 5s)")
        assertNull(httpServer)
        assertTrue(isPortClosed(), "port $port still accepts connections after /shutdown")
    }

    private fun startServer(module: com.ismartcoding.plain.lib.ktorserver.core.application.Application.() -> Unit) {
        val server = embeddedServer(
            Netty,
            applicationEnvironment { log = LoggerFactory.getLogger("stop-lifecycle-test") },
            configure = {
                connector {
                    port = 0
                    host = "127.0.0.1"
                }
            },
            module = module,
        )
        server.start(wait = false)
        engine = server
        httpServer = server
        port = runBlocking { server.engine.resolvedConnectors().first().port }
        HttpServerManager.serverState.value = HttpServerState.ON
    }

    private fun isPortClosed(): Boolean {
        val deadline = System.currentTimeMillis() + 2000
        while (System.currentTimeMillis() < deadline) {
            val closed = try {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200); false }
            } catch (_: Exception) {
                true
            }
            if (closed) return true
            Thread.sleep(50)
        }
        return false
    }
}
