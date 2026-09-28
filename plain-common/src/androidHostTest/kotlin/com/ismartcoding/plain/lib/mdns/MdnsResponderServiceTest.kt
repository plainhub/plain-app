package com.ismartcoding.plain.lib.mdns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks service-publication behavior: rename sends an RFC 6762 §8.4 goodbye
 * before the new announcement, same-FQDN updates re-announce only, and
 * unpublished responders decline no-ops.
 */
internal class MdnsResponderServiceTest : MdnsResponderTestBase() {

    private fun service(instance: String) = MdnsServiceInfo(
        instanceName = instance,
        serviceType = PLAINAPP_SERVICE_TYPE,
        targetHostname = "plainapp-abc123.local",
        port = 8443,
        txtRecords = listOf("id=abc123"),
        ips = listOf("192.168.1.10"),
    )

    @Test fun `renaming the instance sends goodbye before the new announcement`() {
        assertTrue(MdnsHostResponder.start("myhost", service("Pixel 7")))
        mainSocket().sent.clear()

        assertTrue(MdnsHostResponder.updateService(service("Pixel 8")))

        val sent = mainSocket().sent.toList()
        assertTrue("expected goodbye + announcement", sent.size >= 2)
        val goodbye = MdnsPacketCodec.parseResponse(sent[0].bytes)!!
        assertTrue(goodbye.allRecords.all { it.ttl == 0L })
        assertEquals("Pixel 7.$PLAINAPP_SERVICE_TYPE", goodbye.allRecords.first().ptrTarget)

        val announcement = MdnsPacketCodec.parseResponse(sent[1].bytes)!!
        assertTrue(announcement.allRecords.all { it.ttl != 0L })
        assertEquals("Pixel 8.$PLAINAPP_SERVICE_TYPE", announcement.allRecords.first().ptrTarget)
    }

    @Test fun `same-fqdn update re-announces without a goodbye`() {
        assertTrue(MdnsHostResponder.start("myhost", service("Pixel 7")))
        mainSocket().sent.clear()

        assertTrue(MdnsHostResponder.updateService(service("Pixel 7").copy(port = 9000)))

        val sent = mainSocket().sent.toList()
        assertEquals(1, sent.size)
        val parsed = MdnsPacketCodec.parseResponse(sent[0].bytes)!!
        assertTrue(parsed.allRecords.all { it.ttl != 0L })
        assertEquals(9000, parsed.allRecords.single { it.type == MdnsPacketCodec.TYPE_SRV }.srv!!.port)
    }

    @Test fun `updateService without a published service is a no-op`() {
        assertTrue(MdnsHostResponder.start("myhost", null))
        mainSocket().sent.clear()

        assertFalse(MdnsHostResponder.updateService(service("Pixel 7")))

        assertTrue(mainSocket().sent.isEmpty())
    }
}
