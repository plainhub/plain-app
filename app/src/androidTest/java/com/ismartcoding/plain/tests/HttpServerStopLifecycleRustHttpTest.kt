package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.platform.HttpServerManager
import com.ismartcoding.plain.platform.startHttpEngineAsync
import com.ismartcoding.plain.platform.stopHttpEngineAsync
import com.ismartcoding.plain.platform.stopHttpServerCoreAsync
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Regression lock for the 2026-09 stop-latency bug: stopping used to GET
 * /shutdown and run the engine teardown inside the route's call coroutine, so
 * the engine's disposeAndJoin waited on the very handler performing the stop —
 * every stop burned the full 5s engine shutdown timeout (5.1s measured on
 * device). The invariant is that the in-process stop orchestration does not
 * depend on the server it stops (no self HTTP round-trip) and finishes well
 * under the 5s timeout, releasing the port.
 *
 * This is also the regression lock for a stop that never finished: the stop
 * hooks include an mDNS unpublish that waits on the Rust content API, and a
 * runtime whose command loop had died left that wait pending forever — so
 * `serverState` never reached OFF and the server could not be turned off.
 *
 * It runs here rather than in `androidHostTest` because it needs the real
 * Rust engine; a host JVM cannot load its native library. The state machine
 * around stop (STARTING → OFF, engine-create failure → ERROR) is covered
 * without an engine by `HttpServerStateInvariantsTest`.
 *
 * The `/shutdown` route itself lives in the Rust listener, which reaches the
 * same teardown through the `mainGraphqlShutdown` host action; its
 * loopback-only guard is covered by
 * `shutdown_only_accepts_ipv4_and_ipv6_loopback_peers` in plain-rs.
 */
@RunWith(AndroidJUnit4::class)
class HttpServerStopLifecycleRustHttpTest {
    @Test
    fun inProcessStop_reachesOff_fastAndReleasesThePort() = runBlocking {
        val oldService = UserPrefs.service.value
        val oldDesktop = UserPrefs.desktopAccess.value
        try {
            UserPrefs.service.value = true
            UserPrefs.desktopAccess.value = true
            stopHttpEngineAsync()
            startHttpEngineAsync()
            assertTrue(com.ismartcoding.plain.platform.checkHttpServerAsync())
            val port = UserPrefs.httpPort.value
            HttpServerManager.serverState.value = HttpServerState.ON

            val start = System.currentTimeMillis()
            stopHttpServerCoreAsync()
            val elapsed = System.currentTimeMillis() - start

            assertEquals(HttpServerState.OFF, HttpServerManager.serverState.value)
            assertTrue(
                "stop took ${elapsed}ms — did the teardown move back into a call coroutine or a self-HTTP round-trip? (regression: 5.1s)",
                elapsed < 3000,
            )
            assertTrue("port $port still accepts connections after stop", isPortClosed(port))
        } finally {
            stopHttpEngineAsync()
            HttpServerManager.serverState.value = HttpServerState.OFF
            UserPrefs.service.value = oldService
            UserPrefs.desktopAccess.value = oldDesktop
        }
    }

    private fun isPortClosed(port: Int): Boolean {
        val deadline = System.currentTimeMillis() + 3000
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
