package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.NettyApplicationEngine
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import com.ismartcoding.plain.lib.ktorserver.core.routing.get
import com.ismartcoding.plain.lib.ktorserver.core.routing.routing
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Regression locks for the 2026-09 port-conflict start fixes in the vendored
 * Netty engine (NettyApplicationEngine), each pinned against a real engine:
 *
 * 1. `terminate()` on a failed start used the no-arg `shutdownGracefully()` —
 *    Netty's default 2s quiet period per event-loop group (~4s total) — so
 *    every start attempt on an occupied port stalled before the failure was
 *    even reported. Must now fail fast.
 * 2. `start()` used `channels = list.map { bind }.map { sync }`: when a later
 *    connector failed, the earlier bound channels were never assigned and
 *    never closed — their sockets leaked until process death and every later
 *    start reported those ports as occupied. Connectors bound before the
 *    failing one must be released by the failure path.
 * 3. The bootstrap sets SO_REUSEADDR on the server channel: after a stop with
 *    a recently served connection (accepted socket in TIME_WAIT), an
 *    immediate restart on the same port must bind successfully.
 */
class EngineBindFailureTest {

    @Test
    fun bindFailureReturnsFast() {
        ServerSocket(0).use { held ->
            val server = newServer(held.localPort)
            val start = System.currentTimeMillis()
            val ex = assertFailsWith<Exception> { server.start(wait = false) }
            val elapsed = System.currentTimeMillis() - start
            println("bindFailureReturnsFast: ${elapsed}ms (${ex.javaClass.simpleName}: ${ex.message})")
            assertTrue(
                elapsed < 1500,
                "bind failure took ${elapsed}ms — did terminate() fall back to Netty's default 2s-per-group quiet period? (old behavior: ~4s)",
            )
        }
    }

    @Test
    fun earlierConnectorsAreReleasedWhenALaterBindFails() {
        val (httpPort) = freePorts(1)
        ServerSocket(0).use { heldHttps ->
            val server = embeddedServer(
                Netty,
                applicationEnvironment { log = LoggerFactory.getLogger("bind-failure-test") },
                configure = {
                    connector {
                        port = httpPort
                    }
                    connector {
                        port = heldHttps.localPort
                    }
                },
                module = { routing { get("/ping") { call.respondText("pong") } } },
            )
            assertFailsWith<Exception> { server.start(wait = false) }

            // The http connector bound before the https bind failed: its
            // listen socket must be closed by the failure path, otherwise the
            // port stays occupied until process death and the start fallback
            // cascades through the candidate list leaking one socket per try.
            ServerSocket(httpPort).use {
                assertTrue(it.isBound, "port $httpPort still held after a failed multi-connector start — partial-bind leak")
            }
        }
    }

    @Test
    fun immediateRestartAfterServedConnectionBindsSamePort() {
        val first = newServer(0)
        first.start(wait = false)
        val port = runBlocking { first.engine.resolvedConnectors().first().port }

        // Speak HTTP/1.0 over a raw socket: the server closes the connection
        // after responding, leaving the accepted socket in TIME_WAIT on the
        // listen port — exactly the state a quick stop→start lands in.
        Socket("127.0.0.1", port).use { socket ->
            socket.getOutputStream().write("GET /ping HTTP/1.0\r\nHost: 127.0.0.1\r\n\r\n".toByteArray())
            socket.getInputStream().readBytes()
        }

        first.stop(0, 500)

        val restartStart = System.currentTimeMillis()
        val second = newServer(port)
        second.start(wait = false)
        val elapsed = System.currentTimeMillis() - restartStart
        println("immediateRestartAfterServedConnectionBindsSamePort: restart bound port $port in ${elapsed}ms")
        second.stop(0, 500)
    }

    private fun newServer(port: Int): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
        embeddedServer(
            Netty,
            applicationEnvironment { log = LoggerFactory.getLogger("bind-failure-test") },
            configure = {
                connector {
                    // Wildcard, like the production connector: a wildcard
                    // listener held by another process must conflict. (With a
                    // specific host, SO_REUSEADDR would legally bind over a
                    // foreign wildcard listener and the test would prove
                    // nothing.)
                    this.port = port
                }
            },
            module = { routing { get("/ping") { call.respondText("pong") } } },
        )

    /** Ephemeral ports verified free right now (bind, read, close). */
    private fun freePorts(n: Int): List<Int> {
        val sockets = (1..n).map { ServerSocket(0) }
        val ports = sockets.map { it.localPort }
        sockets.forEach { it.close() }
        return ports
    }
}
