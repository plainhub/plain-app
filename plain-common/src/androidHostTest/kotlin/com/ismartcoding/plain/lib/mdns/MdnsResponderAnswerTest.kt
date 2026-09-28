package com.ismartcoding.plain.lib.mdns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the inbound answering path: datagram → receive thread → single
 * consumer → response on the wire. Covers RFC 6762 §6.7 (responses come from
 * port 5353), QU/legacy-unicast routing, the multicast-loopback guard and the
 * external-multicast liveness signal the browser's QU fallback polls.
 */
internal class MdnsResponderAnswerTest : MdnsResponderTestBase() {

    private val peerIp = "192.168.1.50"

    private fun service() = MdnsServiceInfo(
        instanceName = "Pixel 7 Pro",
        serviceType = PLAINAPP_SERVICE_TYPE,
        targetHostname = "plainapp-abc123.local",
        port = 8443,
        txtRecords = listOf("id=abc123"),
        ips = listOf("192.168.1.10"),
    )

    private fun startWithService() {
        installRealWorkers()
        assertTrue(MdnsHostResponder.start("myhost", service()))
        mainSocket().sent.clear() // drop the startup announcements
    }

    private fun awaitSent(count: Int = 1): List<SentDatagram> {
        assertTrue("expected $count response(s) on the wire", waitUntil { mainSocket().sent.size >= count })
        return mainSocket().sent.toList().take(count)
    }

    @Test fun `multicast PTR query from port 5353 is answered to the group`() {
        startWithService()

        mainSocket().deliver(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE), peerIp, 5353)

        val response = awaitSent().single()
        assertEquals("224.0.0.251", response.destIp)
        assertEquals(5353, response.destPort)
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)!!
        assertTrue(parsed.answers.any { it.type == MdnsPacketCodec.TYPE_PTR })
    }

    @Test fun `QU query is answered unicast to the sender`() {
        startWithService()

        mainSocket().deliver(
            MdnsPacketCodec.buildQuery(PLAINAPP_SERVICE_TYPE, MdnsPacketCodec.TYPE_PTR, unicastResponse = true),
            peerIp, 5353,
        )

        val response = awaitSent().single()
        assertEquals(peerIp, response.destIp)
        assertEquals(5353, response.destPort)
    }

    @Test fun `legacy query from a non-5353 port is answered unicast to that port`() {
        startWithService()

        mainSocket().deliver(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE), peerIp, 53211)

        val response = awaitSent().single()
        assertEquals(peerIp, response.destIp)
        assertEquals(53211, response.destPort)
    }

    @Test fun `own multicast loopback is not answered`() {
        startWithService()

        mainSocket().deliver(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE), "192.168.1.10", 5353)

        assertFalse("own looped-back query must not be answered", waitUntil(400) { mainSocket().sent.isNotEmpty() })
    }

    @Test fun `external packet marks the multicast path alive and the flag resets on read`() {
        startWithService()

        mainSocket().deliver(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE), peerIp, 5353)
        awaitSent()
        assertTrue(MdnsHostResponder.takeExternalMulticastSeen())
        assertFalse("flag must reset after being taken", MdnsHostResponder.takeExternalMulticastSeen())
    }

    @Test fun `service answer carries the interface address of the sender subnet`() {
        ifaces = listOf(
            MdnsIface("wlan0", 24) to "192.168.1.10",
            MdnsIface("eth1", 24) to "10.0.0.5",
        )
        startWithService()

        mainSocket().deliver(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE), "10.0.0.77", 5353)

        val response = awaitSent().single()
        val aRecords = MdnsPacketCodec.parseResponse(response.bytes)!!.allRecords
            .filter { it.type == MdnsPacketCodec.TYPE_A }
            .mapNotNull { it.ip }
        assertEquals(listOf("10.0.0.5"), aRecords)
    }

    @Test fun `hostname A query is answered when no service is published`() {
        installRealWorkers()
        assertTrue(MdnsHostResponder.start("myhost", null))
        mainSocket().sent.clear()

        mainSocket().deliver(MdnsPacketCodec.buildQuery("myhost.local", MdnsPacketCodec.TYPE_A), peerIp, 5353)

        val response = awaitSent().single()
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)!!
        val a = parsed.answers.single()
        assertEquals("myhost.local", a.name)
        assertEquals("192.168.1.10", a.ip)
    }

    @Test fun `cleared service stops PTR answers but keeps hostname answers`() {
        startWithService()
        MdnsHostResponder.clearService()
        mainSocket().sent.clear()

        mainSocket().deliver(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE), peerIp, 5353)
        assertFalse("PTR query must not be answered after clearService", waitUntil(400) { mainSocket().sent.isNotEmpty() })

        mainSocket().deliver(MdnsPacketCodec.buildQuery("myhost.local", MdnsPacketCodec.TYPE_A), peerIp, 5353)
        val response = awaitSent().single()
        assertNotNull(MdnsPacketCodec.parseResponse(response.bytes)!!.answers.single { it.type == MdnsPacketCodec.TYPE_A })
    }
}
