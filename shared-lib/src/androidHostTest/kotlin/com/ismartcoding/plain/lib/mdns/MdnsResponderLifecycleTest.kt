package com.ismartcoding.plain.lib.mdns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks [MdnsHostResponder] bring-up / teardown behavior: socket creation,
 * incremental group membership, socket reuse across restarts, QU socket
 * lifecycle and the isRunning contract callers rely on.
 */
internal class MdnsResponderLifecycleTest : MdnsResponderTestBase() {

    private fun service(instance: String = "Pixel 7 Pro") = MdnsServiceInfo(
        instanceName = instance,
        serviceType = PLAINAPP_SERVICE_TYPE,
        targetHostname = "plainapp-abc123.local",
        port = 8443,
        txtRecords = listOf("id=abc123"),
        ips = listOf("192.168.1.10"),
    )

    @Test fun `start binds 5353 joins every LAN interface and stays running`() {
        ifaces = listOf(
            MdnsIface("wlan0", 24) to "192.168.1.10",
            MdnsIface("eth1", 24) to "192.168.1.11",
        )

        assertTrue(MdnsHostResponder.start("myhost", service()))

        assertEquals(listOf(5353), mainSocket().binds)
        assertEquals(
            listOf("224.0.0.251" to "wlan0", "224.0.0.251" to "eth1"),
            mainSocket().joins,
        )
        assertTrue(MdnsHostResponder.isRunning)
    }

    @Test fun `start creates the QU socket on an ephemeral port`() {
        assertTrue(MdnsHostResponder.start("myhost", service()))

        assertEquals(listOf(0), quSocket().binds)
        assertEquals(listOf("plain-mdns-responder", "plain-mdns-qu"), workers.map { it.first })
    }

    @Test fun `empty hostname fails without creating a socket`() {
        assertFalse(MdnsHostResponder.start("", service()))

        assertTrue(sockets.isEmpty())
        assertFalse(MdnsHostResponder.isRunning)
    }

    @Test fun `no LAN interfaces fails without creating a socket`() {
        ifaces = emptyList()

        assertFalse(MdnsHostResponder.start("myhost", service()))

        assertTrue(sockets.isEmpty())
    }

    @Test fun `bind failure closes the socket and reports not running`() {
        MdnsHostResponder.socketFactory = { FakeMdnsSocket().also { s ->
            s.bindFailure = RuntimeException("port taken")
            sockets += s
        } }

        assertFalse(MdnsHostResponder.start("myhost", service()))

        assertTrue(mainSocket().isClosed)
        assertFalse(MdnsHostResponder.isRunning)
    }

    @Test fun `restart reuses the live socket without rebinding`() {
        assertTrue(MdnsHostResponder.start("myhost", service()))
        val first = mainSocket()
        val joinCount = first.joins.size

        assertTrue(MdnsHostResponder.restartSocket())

        assertEquals(2, sockets.size) // main + QU, nothing new
        assertEquals(listOf(5353), first.binds)
        assertEquals(joinCount, first.joins.size) // no duplicate joins
        assertTrue(MdnsHostResponder.isRunning)
    }

    @Test fun `restart after a network change joins only the new interface`() {
        assertTrue(MdnsHostResponder.start("myhost", service()))
        val first = mainSocket()

        ifaces = listOf(
            MdnsIface("wlan0", 24) to "192.168.1.10",
            MdnsIface("eth1", 24) to "192.168.1.11",
        )
        assertTrue(MdnsHostResponder.restartSocket())

        assertEquals(listOf("224.0.0.251" to "wlan0", "224.0.0.251" to "eth1"), first.joins)
        assertTrue(first.isClosed.not())
    }

    @Test fun `ensureStarted is a no-op while running`() {
        assertTrue(MdnsHostResponder.start("myhost", service()))

        assertTrue(MdnsHostResponder.ensureStarted("myhost"))

        assertEquals(2, sockets.size)
    }

    @Test fun `stop closes both sockets and clears the hostname`() {
        assertTrue(MdnsHostResponder.start("myhost", service()))
        val main = mainSocket()
        val qu = quSocket()

        MdnsHostResponder.stop()

        assertTrue(main.isClosed)
        assertTrue(qu.isClosed)
        assertFalse(MdnsHostResponder.isRunning)

        // A later ensureStarted brings a fresh socket up from the empty state.
        assertTrue(MdnsHostResponder.ensureStarted("myhost"))
        assertEquals(4, sockets.size) // 2 closed + 2 fresh
        assertTrue(sockets[2].binds.contains(5353))
    }

    @Test fun `service survives a socket teardown and announces on the next start`() {
        installRealWorkers()
        assertTrue(MdnsHostResponder.start("myhost", service("Old Name")))

        // Simulate the receive path dying without a clean stop(): the socket
        // closes behind the responder's back, so serviceInfo stays published.
        mainSocket().close()
        assertTrue("socket closed → not running", waitUntil { !MdnsHostResponder.isRunning })

        // updateService stores the replacement and returns false while down.
        assertFalse(MdnsHostResponder.updateService(service("New Name")))

        assertTrue(MdnsHostResponder.ensureStarted("myhost"))

        val announced = sockets.last().sent.mapNotNull { MdnsPacketCodec.parseResponse(it.bytes) }
        val instanceNames = announced.flatMap { r -> r.allRecords }
            .mapNotNull { it.ptrTarget }
            .map { target -> target.substringBefore(".$PLAINAPP_SERVICE_TYPE") }
        assertTrue("expected the stored 'New Name' service to be announced", "New Name" in instanceNames)
    }

    @Test fun `stop clears the service so updateService is a full no-op`() {
        assertTrue(MdnsHostResponder.start("myhost", service("Old Name")))
        MdnsHostResponder.stop()

        assertFalse(MdnsHostResponder.updateService(service("New Name")))

        // Nothing is stored: the next start comes up in hostname-only mode.
        assertTrue(MdnsHostResponder.ensureStarted("myhost"))
        val announced = sockets.last().sent.mapNotNull { MdnsPacketCodec.parseResponse(it.bytes) }
        val hasServicePtr = announced.any { r -> r.allRecords.any { it.type == MdnsPacketCodec.TYPE_PTR } }
        assertFalse("no service PTR must be announced after a clean stop", hasServicePtr)
    }

    /**
     * Documents CURRENT behavior: when the receive worker dies but the socket
     * stays open, restartSocket() reuses the socket and never respawns the
     * worker — isRunning stays false and inbound packets are lost until
     * process restart. This is the hole a keep-alive fix must close; flip
     * this test when the fix lands.
     */
    @Test fun `restart with a dead worker and live socket does not respawn the receive loop`() {
        assertTrue(MdnsHostResponder.start("myhost", service()))

        workerNamed("plain-mdns-responder").isAlive = false
        assertFalse(MdnsHostResponder.isRunning)

        assertTrue(MdnsHostResponder.restartSocket())
        assertFalse(MdnsHostResponder.isRunning)
        assertEquals(1, workers.count { it.first == "plain-mdns-responder" })
    }

    @Test fun `announcement goes out once per interface with only that interface address`() {
        ifaces = listOf(
            MdnsIface("wlan0", 24) to "192.168.1.10",
            MdnsIface("eth1", 24) to "10.0.0.5",
        )

        assertTrue(MdnsHostResponder.start("myhost", service()))

        val announcements = mainSocket().sent.toList()
        assertEquals(2, announcements.size)
        assertEquals(listOf("wlan0", "eth1"), mainSocket().outgoingIfaces)
        announcements.forEach { sent ->
            val ips = MdnsPacketCodec.parseResponse(sent.bytes)!!.allRecords
                .mapNotNull { it.ip }
            assertEquals("announcement must carry only its outgoing interface address", 1, ips.size)
        }
        // wlan0 first: its announcement carries 192.168.1.10 only.
        assertEquals(
            listOf("192.168.1.10"),
            MdnsPacketCodec.parseResponse(announcements[0].bytes)!!.allRecords.mapNotNull { it.ip },
        )
        assertEquals(
            listOf("10.0.0.5"),
            MdnsPacketCodec.parseResponse(announcements[1].bytes)!!.allRecords.mapNotNull { it.ip },
        )
    }
}
