package com.ismartcoding.plain.tests

import com.ismartcoding.plain.mdns.MdnsHostResponder
import com.ismartcoding.plain.mdns.MdnsPacketCodec
import com.ismartcoding.plain.mdns.ipToInt
import com.ismartcoding.plain.mdns.isMobileDataInterface
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

    @Test fun `plain multicast query does not request unicast response`() {
        val query = mdnsQuery("plainapp.local", type = 1)
        val result = MdnsPacketCodec.buildResponseIfMatchDetails(query, "plainapp.local", listOf(ip4("192.168.1.55")))
        assertNotNull(result)
        assertFalse(result!!.unicastResponseRequested)
        assertEquals(1, result.matchedQuestions.single().qtype)
        assertEquals(1, result.matchedQuestions.single().qclass)
    }

    @Test fun `query with QU bit requests unicast response`() {
        val query = mdnsQuery("plainapp.local", type = 1, qu = true)
        val result = MdnsPacketCodec.buildResponseIfMatchDetails(query, "plainapp.local", listOf(ip4("192.168.1.55")))
        assertNotNull(result)
        assertTrue(result!!.unicastResponseRequested)
        assertEquals(1, result.matchedQuestions.single().qtype)
        assertEquals(1, result.matchedQuestions.single().qclass)
    }

    @Test fun `response packet with QR bit set yields null — no reply loop`() {
        val q = mdnsQuery("plainapp.local", 1).copyOf()
        q[2] = 0x84.toByte() // set QR=1 (response), AA=1 — simulate an incoming response
        assertNull(MdnsPacketCodec.buildResponseIfMatch(q, "plainapp.local", listOf(ip4("192.168.1.1"))))
    }

    // ── extractInet4Address ───────────────────────────────────────────────────

    @Test fun `extractInet4Address — plain IPv4 returned as-is`() {
        val ip = ip4("192.168.1.5")
        assertEquals(ip, MdnsHostResponder.extractInet4Address(ip))
    }

    @Test fun `extractInet4Address — IPv4-mapped IPv6 unwrapped`() {
        val mapped = InetAddress.getByName("::ffff:192.168.1.5")
        val result = MdnsHostResponder.extractInet4Address(mapped)
        assertNotNull("Expected unwrapped Inet4Address", result)
        assertEquals("192.168.1.5", result!!.hostAddress)
    }

    @Test fun `extractInet4Address — pure IPv6 returns null`() {
        assertNull(MdnsHostResponder.extractInet4Address(InetAddress.getByName("2001:db8::1")))
    }

    private fun ip4(addr: String) = InetAddress.getByName(addr) as Inet4Address
    private fun mdnsQuery(name: String, type: Int, qu: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        name.split('.').filter { it.isNotEmpty() }.forEach { label ->
            val b = label.toByteArray(Charsets.UTF_8)
            out.write(b.size)
            out.write(b)
        }
        out.write(0)
        val qclass = if (qu) 0x8001 else 0x0001
        out.write(byteArrayOf((type ushr 8).toByte(), type.toByte(), (qclass ushr 8).toByte(), qclass.toByte()))
        return out.toByteArray()
    }
}
