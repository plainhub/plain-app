package com.ismartcoding.plain.tests

import com.ismartcoding.plain.discover.DDiscoverReply
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.lib.mdns.PLAINAPP_SERVICE_TYPE
import com.ismartcoding.plain.discover.buildMdnsServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class MdnsServiceInfoTest {

    private val reply = DDiscoverReply(
        id = "abc123",
        name = "Pixel 7 Pro",
        port = 8443,
        deviceType = DeviceType.PHONE,
        version = "1.2.3",
        platform = "android",
        ips = listOf("192.168.1.50"),
        awareSupported = true,
        awareRunning = false,
    )

    @Test fun `factory maps reply fields onto the service`() {
        val service = buildMdnsServiceInfo(reply, "plainapp-abc123.local")
        assertEquals("Pixel 7 Pro", service.instanceName)
        assertEquals(PLAINAPP_SERVICE_TYPE, service.serviceType)
        assertEquals("plainapp-abc123.local", service.targetHostname)
        assertEquals("Pixel 7 Pro._plainapp._tcp.local", service.instanceFqdn)
        assertEquals(8443, service.port)
        assertEquals(listOf("192.168.1.50"), service.ips)
    }

    @Test fun `TXT records mirror reply fields`() {
        val txt = buildMdnsServiceInfo(reply, "plainapp-abc123.local").txtRecords.toMap()
        assertEquals("abc123", txt["id"])
        assertEquals("PHONE", txt["dv"])
        assertEquals("1.2.3", txt["ver"])
        assertEquals("android", txt["pf"])
        assertEquals("1", txt["aw"])
        assertEquals("0", txt["ar"])
    }

    @Test fun `aware flags render as one and zero`() {
        val running = reply.copy(awareSupported = false, awareRunning = true)
        val txt = buildMdnsServiceInfo(running, "plainapp-abc.local").txtRecords.toMap()
        assertEquals("0", txt["aw"])
        assertEquals("1", txt["ar"])
    }

    private fun List<String>.toMap(): Map<String, String> =
        mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
        }.toMap()
}
