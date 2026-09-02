package com.ismartcoding.plain.lib.mdns

import com.ismartcoding.plain.lib.mdns.MdnsPacketCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MdnsServiceBrowserTest {

    // ── shouldActivateQuFallback ──────────────────────────────────────────────

    @Test fun `first cycle never activates even without external multicast`() =
        assertFalse(MdnsServiceBrowser.shouldActivateQuFallback(cycle = 1, quActive = false, externalMulticastSeen = false))

    @Test fun `activates after two cycles with no external multicast`() =
        assertTrue(MdnsServiceBrowser.shouldActivateQuFallback(cycle = 2, quActive = false, externalMulticastSeen = false))

    @Test fun `external multicast keeps multicast queries active`() =
        assertFalse(MdnsServiceBrowser.shouldActivateQuFallback(cycle = 5, quActive = false, externalMulticastSeen = true))

    @Test fun `already active signals no re-activation — stickiness lives in the browser flag`() {
        assertFalse(MdnsServiceBrowser.shouldActivateQuFallback(cycle = 5, quActive = true, externalMulticastSeen = true))
        assertFalse(MdnsServiceBrowser.shouldActivateQuFallback(cycle = 5, quActive = true, externalMulticastSeen = false))
    }

    // ── QU query wire format ──────────────────────────────────────────────────

    @Test fun `dispatched QU query carries the unicast-response bit`() {
        val bytes = MdnsPacketCodec.buildQuery(PLAINAPP_SERVICE_TYPE, MdnsPacketCodec.TYPE_PTR, unicastResponse = true)
        val questions = MdnsPacketCodec.readQuestions(bytes)!!
        assertTrue(questions.single().unicastResponseRequested)
    }

    @Test fun `dispatched multicast query has no unicast-response bit`() {
        val bytes = MdnsPacketCodec.buildQuery(PLAINAPP_SERVICE_TYPE, MdnsPacketCodec.TYPE_PTR, unicastResponse = false)
        val questions = MdnsPacketCodec.readQuestions(bytes)!!
        assertFalse(questions.single().unicastResponseRequested)
    }

    // ── groupARecordsByHostname (multi-homed hosts) ───────────────────────────

    private fun aRecord(hostname: String, ip: String): MdnsRecord {
        val octets = ip.split(".").map { it.toInt().toByte() }
        return MdnsRecord(
            name = hostname,
            type = MdnsPacketCodec.TYPE_A,
            cls = 1,
            ttl = 120,
            packet = octets.toByteArray(),
            rdataStart = 0,
            rdataLength = 4,
        )
    }

    @Test fun `A records for one hostname in a single packet keep every address`() {
        val grouped = MdnsServiceBrowser.groupARecordsByHostname(
            listOf(
                aRecord("p9.local", "192.168.1.10"),
                aRecord("p9.local", "10.8.0.2"),
            ),
        )

        assertEquals(setOf("192.168.1.10", "10.8.0.2"), grouped["p9.local"])
    }

    @Test fun `A records for different hostnames stay in separate groups`() {
        val grouped = MdnsServiceBrowser.groupARecordsByHostname(
            listOf(
                aRecord("p9.local", "192.168.1.10"),
                aRecord("mac.local", "10.8.0.2"),
            ),
        )

        assertEquals(setOf("192.168.1.10"), grouped["p9.local"])
        assertEquals(setOf("10.8.0.2"), grouped["mac.local"])
    }

    @Test fun `hostnames are grouped case-insensitively`() {
        val grouped = MdnsServiceBrowser.groupARecordsByHostname(
            listOf(
                aRecord("P9.LOCAL", "192.168.1.10"),
                aRecord("p9.local", "10.8.0.2"),
            ),
        )

        assertEquals(setOf("192.168.1.10", "10.8.0.2"), grouped["p9.local"])
    }

    @Test fun `non-A records are ignored when grouping`() {
        val a = aRecord("p9.local", "192.168.1.10")
        val ptr = a.copy(type = MdnsPacketCodec.TYPE_PTR)

        val grouped = MdnsServiceBrowser.groupARecordsByHostname(listOf(ptr))

        assertTrue(grouped.isEmpty())
    }
}
