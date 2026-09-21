package com.ismartcoding.plain.lib.mdns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the event-driven self-healing: a receive loop killed by an uncaught
 * Error (the loop body only catches Exception) announces its own death from
 * its finally block — the socket is closed and a rebuild is scheduled on the
 * retry backoff. No watchdog, no polling; this is the recovery path for the
 * "NearbyPage empty until the app is killed" failure mode.
 */
internal class MdnsReceiveLoopDeathTest : MdnsResponderTestBase() {

    private fun service() = MdnsServiceInfo(
        instanceName = "Pixel 7 Pro",
        serviceType = PLAINAPP_SERVICE_TYPE,
        targetHostname = "plainapp-abc123.local",
        port = 8443,
        txtRecords = listOf("id=abc123"),
        ips = listOf("192.168.1.10"),
    )

    @Test fun `main loop killed by an Error revives itself via the death notification`() {
        installRealWorkers()
        MdnsHostResponder.retryInitialDelayMs = 100
        assertTrue(MdnsHostResponder.start("myhost", service()))
        val dead = mainSocket()

        // OOM unwinds the receive thread; only its finally can report the death.
        dead.failReceive = OutOfMemoryError("simulated allocation failure")

        // Wait for the death first: isRunning is true until the thread dies,
        // so waiting for the revival alone could pass before the death lands.
        assertTrue("worker should die from the simulated OOM", waitUntil { !MdnsHostResponder.isRunning })
        assertTrue(
            "death notification should rebuild the receive pair",
            waitUntil { MdnsHostResponder.isRunning },
        )

        assertTrue(dead.isClosed)
        assertEquals(3, sockets.size) // old main + QU + rebuilt main
        assertEquals(listOf(5353), sockets[2].binds)
        assertTrue(sockets[2].joins.isNotEmpty())
    }

    @Test fun `QU loop killed by an Error revives without touching the main pair`() {
        installRealWorkers()
        MdnsHostResponder.retryInitialDelayMs = 100
        assertTrue(MdnsHostResponder.start("myhost", service()))
        val main = mainSocket()
        val deadQu = quSocket()

        deadQu.failReceive = OutOfMemoryError("simulated allocation failure")

        assertTrue(
            "QU death notification should rebuild the QU socket",
            waitUntil { sockets.size == 3 },
        )

        assertTrue(deadQu.isClosed)
        assertFalse("main pair must stay untouched", main.isClosed)
        assertTrue(MdnsHostResponder.isRunning)
        assertEquals(listOf(0), sockets[2].binds) // rebuilt QU socket, ephemeral port
    }

    @Test fun `clean stop schedules no revival`() {
        installRealWorkers()
        MdnsHostResponder.retryInitialDelayMs = 100
        assertTrue(MdnsHostResponder.start("myhost", service()))
        val count = sockets.size

        MdnsHostResponder.stop()
        Thread.sleep(400)

        assertEquals("no revival after a clean stop", count, sockets.size)
        assertFalse(MdnsHostResponder.isRunning)
    }

    @Test fun `death during a stopped responder does not revive`() {
        installRealWorkers()
        MdnsHostResponder.retryInitialDelayMs = 100
        assertTrue(MdnsHostResponder.start("myhost", service()))
        val dead = mainSocket()
        MdnsHostResponder.stop()

        // The loop thread may still be unwinding when stop() finishes; its
        // finally must not resurrect a responder the app explicitly stopped.
        dead.failReceive = OutOfMemoryError("late death")
        Thread.sleep(400)

        assertFalse(MdnsHostResponder.isRunning)
    }
}
