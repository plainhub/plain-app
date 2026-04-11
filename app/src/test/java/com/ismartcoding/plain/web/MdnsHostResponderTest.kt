package com.ismartcoding.plain.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.InetAddress

class MdnsHostResponderTest {

    // ── normalizeHostname ─────────────────────────────────────────────────────

    @Test fun `already has dot local suffix — unchanged`() =
        assertEquals("plainapp.local", MdnsHostResponder.normalizeHostname("plainapp.local"))

    @Test fun `no suffix — dot local appended`() =
        assertEquals("haixin.local", MdnsHostResponder.normalizeHostname("haixin"))

    @Test fun `uppercase — lowercased`() =
        assertEquals("haixin.local", MdnsHostResponder.normalizeHostname("HaiXin.local"))

    @Test fun `leading and trailing dots stripped`() =
        assertEquals("haixin.local", MdnsHostResponder.normalizeHostname(".haixin.local."))

    @Test fun `surrounding whitespace stripped`() =
        assertEquals("haixin.local", MdnsHostResponder.normalizeHostname("  haixin  "))

    @Test fun `empty string returns empty`() =
        assertEquals("", MdnsHostResponder.normalizeHostname(""))

    @Test fun `blank string returns empty`() =
        assertEquals("", MdnsHostResponder.normalizeHostname("   "))

    // ── isMobileDataInterface ─────────────────────────────────────────────────

    @Test fun `rmnet_data0 is mobile data`() = assertTrue(isMobileDataInterface("rmnet_data0"))
    @Test fun `rmnet0 is mobile data`() = assertTrue(isMobileDataInterface("rmnet0"))
    @Test fun `ccmni0 is mobile data`() = assertTrue(isMobileDataInterface("ccmni0"))
    @Test fun `wlan0 is not mobile data`() = assertFalse(isMobileDataInterface("wlan0"))
    @Test fun `ap0 is not mobile data`() = assertFalse(isMobileDataInterface("ap0"))
    @Test fun `eth0 is not mobile data`() = assertFalse(isMobileDataInterface("eth0"))
    @Test fun `wlan1 is not mobile data`() = assertFalse(isMobileDataInterface("wlan1"))

    // ── ipToInt ───────────────────────────────────────────────────────────────

    @Test fun `192 168 1 1 converts correctly`() {
        val ip = ip4("192.168.1.1")
        assertEquals((192 shl 24) or (168 shl 16) or (1 shl 8) or 1, ipToInt(ip))
    }

    @Test fun `10 0 0 1 converts correctly`() {
        val ip = ip4("10.0.0.1")
        assertEquals((10 shl 24) or 1, ipToInt(ip))
    }

    @Test fun `255 255 255 255 is minus one`() {
        assertEquals(-1, ipToInt(ip4("255.255.255.255")))
    }

    @Test fun `0 0 0 0 is zero`() {
        assertEquals(0, ipToInt(ip4("0.0.0.0")))
    }

    // ── MdnsPacketCodec round-trip ────────────────────────────────────────────

    @Test fun `type A query for matching hostname yields non-null response`() {
        val query = mdnsQuery("plainapp.local", type = 1)
        val result = MdnsPacketCodec.buildResponseIfMatch(query, "plainapp.local", listOf(ip4("192.168.1.100")))
        assertNotNull("Expected a response packet", result)
    }

    @Test fun `type ANY query for matching hostname yields non-null response`() {
        val query = mdnsQuery("haixin.local", type = 0xFF)
        val result = MdnsPacketCodec.buildResponseIfMatch(query, "haixin.local", listOf(ip4("192.168.43.1")))
        assertNotNull("Expected a response packet for type ANY", result)
    }

    @Test fun `query for different hostname yields null`() {
        val query = mdnsQuery("other.local", type = 1)
        val result = MdnsPacketCodec.buildResponseIfMatch(query, "plainapp.local", listOf(ip4("192.168.1.100")))
        assertNull("Expected null for non-matching hostname", result)
    }

    @Test fun `empty IP list yields null`() {
        val query = mdnsQuery("plainapp.local", type = 1)
        val result = MdnsPacketCodec.buildResponseIfMatch(query, "plainapp.local", emptyList())
        assertNull("Expected null when no IPs are supplied", result)
    }

    @Test fun `response contains the advertised IP bytes`() {
        val query = mdnsQuery("plainapp.local", type = 1)
        val ip = ip4("192.168.1.55")
        val result = MdnsPacketCodec.buildResponseIfMatch(query, "plainapp.local", listOf(ip))
        assertNotNull(result)
        val bytes = result!!
        val addrBytes = ip.address
        val found = (0..bytes.size - 4).any { i ->
            bytes[i] == addrBytes[0] && bytes[i + 1] == addrBytes[1] &&
                bytes[i + 2] == addrBytes[2] && bytes[i + 3] == addrBytes[3]
        }
        assertTrue("IP bytes should appear in the DNS response payload", found)
    }

    private fun ip4(addr: String) = InetAddress.getByName(addr) as Inet4Address

    private fun mdnsQuery(name: String, type: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        name.split('.').filter { it.isNotEmpty() }.forEach { label ->
            val b = label.toByteArray(Charsets.UTF_8)
            out.write(b.size)
            out.write(b)
        }
        out.write(0)
        out.write(byteArrayOf((type ushr 8).toByte(), type.toByte(), 0, 1))
        return out.toByteArray()
    }
}

