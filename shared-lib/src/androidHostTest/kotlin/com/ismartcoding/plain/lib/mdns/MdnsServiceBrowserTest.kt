package com.ismartcoding.plain.lib.mdns

import com.ismartcoding.plain.lib.mdns.MdnsPacketCodec
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
}
