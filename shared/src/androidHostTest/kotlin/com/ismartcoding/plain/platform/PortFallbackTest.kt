package com.ismartcoding.plain.platform

import com.ismartcoding.plain.httpserver.httpPorts
import com.ismartcoding.plain.httpserver.httpsPorts
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression lock for the port-fallback start policy (2026-09): when the
 * configured port is occupied by a foreign process, the start orchestrator
 * moves to the next free candidate ([nextFreePort]) instead of waiting for
 * the port or failing the start outright; only a fully busy candidate list
 * yields null (→ ERROR). The predicate-injection cases pin the selection
 * policy; the real-socket cases pin that the default [isPortInUse] predicate
 * agrees with an actually bound socket.
 */
class PortFallbackTest {

    @Test
    fun configuredPortIsKeptWhenFree() {
        assertEquals(8080, nextFreePort(8080, httpPorts) { false })
        assertEquals(8443, nextFreePort(8443, httpsPorts) { false })
    }

    @Test
    fun fallsBackToFirstFreeCandidateInListOrder() {
        // 8080 and 80 held → the next candidate in insertion order is 8180;
        // 8443 and 443 held → 8043. The choice must stay deterministic.
        assertEquals(8180, nextFreePort(8080, httpPorts) { it == 8080 || it == 80 })
        assertEquals(8043, nextFreePort(8443, httpsPorts) { it == 8443 || it == 443 })
    }

    @Test
    fun allCandidatesBusyReturnsNull() {
        assertNull(nextFreePort(8080, httpPorts) { true })
        assertNull(nextFreePort(8443, httpsPorts) { true })
    }

    @Test
    fun busyCurrentOutsideCandidateListFallsBackToList() {
        // The configured port need not be a member of the candidate list.
        assertEquals(80, nextFreePort(9999, httpPorts) { it == 9999 })
    }

    @Test
    fun realHeldPortIsSkippedWithDefaultPredicate() {
        val (free1, free2) = freePorts(2)
        ServerSocket(0).use { held ->
            assertEquals(free1, nextFreePort(held.localPort, setOf(held.localPort, free1, free2)))
        }
    }

    @Test
    fun pickedCandidateBindsImmediately() {
        val (free1) = freePorts(1)
        ServerSocket(0).use { held ->
            val picked = nextFreePort(held.localPort, setOf(held.localPort, free1))
            assertEquals(free1, picked)
            ServerSocket(free1).use { assertTrue(it.isBound, "picked port $picked must be bindable") }
        }
    }

    /** Ephemeral ports verified free right now (bind, read, close). */
    private fun freePorts(n: Int): List<Int> {
        val sockets = (1..n).map { ServerSocket(0) }
        val ports = sockets.map { it.localPort }
        sockets.forEach { it.close() }
        return ports
    }
}
