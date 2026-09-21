package com.ismartcoding.plain.lib.mdns

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks the debug-page ring buffer: enable gating, newest-first order, capacity. */
class MdnsPacketCaptureTest {

    @After fun tearDown() {
        MdnsPacketCapture.setEnabled(false)
    }

    @Test fun `disabled capture records nothing`() {
        // Default state is disabled; recordIn must be a no-op.
        MdnsPacketCapture.recordIn("10.0.0.1", 5353, MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))

        assertTrue(MdnsPacketCapture.snapshotIn().isEmpty())
        assertTrue(MdnsPacketCapture.snapshotOut().isEmpty())
    }

    @Test fun `records inbound and outbound packets newest first`() {
        MdnsPacketCapture.setEnabled(true)

        MdnsPacketCapture.recordIn("10.0.0.1", 5353, MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))
        MdnsPacketCapture.recordOut("192.168.1.2", 5353, "224.0.0.251", 5353, MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))
        MdnsPacketCapture.recordIn("10.0.0.9", 5353, MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))

        assertEquals(listOf("10.0.0.9", "10.0.0.1"), MdnsPacketCapture.snapshotIn().map { it.srcIp })
        assertEquals(1, MdnsPacketCapture.snapshotOut().size)
        assertEquals(MdnsPacketDirection.OUT, MdnsPacketCapture.snapshotOut().single().direction)
    }

    @Test fun `buffer keeps only the last 50 packets`() {
        MdnsPacketCapture.setEnabled(true)

        repeat(60) { i ->
            MdnsPacketCapture.recordIn("10.0.0.${i % 250}", 5353, MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))
        }

        val snapshot = MdnsPacketCapture.snapshotIn()
        assertEquals(50, snapshot.size)
        // Newest first: the last recorded packet (i = 59) is at the head.
        assertEquals("10.0.0.59", snapshot.first().srcIp)
        assertEquals("10.0.0.10", snapshot.last().srcIp)
    }

    @Test fun `disabling clears the buffers and re-enabling starts fresh`() {
        MdnsPacketCapture.setEnabled(true)
        MdnsPacketCapture.recordIn("10.0.0.1", 5353, MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))
        assertFalse(MdnsPacketCapture.snapshotIn().isEmpty())

        MdnsPacketCapture.setEnabled(false)
        assertTrue(MdnsPacketCapture.snapshotIn().isEmpty())

        MdnsPacketCapture.setEnabled(true)
        assertTrue(MdnsPacketCapture.snapshotIn().isEmpty())
    }

    @Test fun `summary distinguishes queries from responses`() {
        MdnsPacketCapture.setEnabled(true)

        MdnsPacketCapture.recordIn("10.0.0.1", 5353, MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE))
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE),
            MdnsServiceInfo(
                instanceName = "Pixel 7",
                serviceType = PLAINAPP_SERVICE_TYPE,
                targetHostname = "plainapp-a.local",
                port = 8443,
                txtRecords = listOf("id=a"),
                ips = listOf("192.168.1.50"),
            ),
        )!!
        MdnsPacketCapture.recordIn("10.0.0.2", 5353, response.bytes)

        val summaries = MdnsPacketCapture.snapshotIn().map { it.summary }
        assertTrue(summaries[1].startsWith("query"))
        assertTrue(summaries[0].startsWith("response"))
    }
}
