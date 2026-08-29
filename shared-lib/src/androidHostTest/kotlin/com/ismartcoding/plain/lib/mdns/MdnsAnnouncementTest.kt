package com.ismartcoding.plain.lib.mdns

import org.junit.Assert.assertEquals
import org.junit.Test

class MdnsAnnouncementTest {

    @Test fun `service announcement contains only its outgoing interface address`() {
        val wifiIp = "192.168.1.55"
        val service = MdnsServiceInfo(
            instanceName = "PlainApp",
            serviceType = PLAINAPP_SERVICE_TYPE,
            targetHostname = "plainapp.local",
            port = 8080,
            txtRecords = emptyList(),
            ips = listOf(wifiIp, "10.8.0.2"),
        )

        val bytes = MdnsHostResponder.buildAnnouncement(service, "plainapp.local", wifiIp)

        assertEquals(listOf(wifiIp), advertisedAddresses(bytes!!))
    }

    @Test fun `hostname announcement contains only its outgoing interface address`() {
        val wifiIp = "192.168.1.55"

        val bytes = MdnsHostResponder.buildAnnouncement(null, "plainapp.local", wifiIp)

        assertEquals(listOf(wifiIp), advertisedAddresses(bytes!!))
    }

    private fun advertisedAddresses(bytes: ByteArray): List<String> =
        MdnsPacketCodec.parseResponse(bytes)?.allRecords?.mapNotNull { it.ip }.orEmpty()
}
