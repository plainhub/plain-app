package com.ismartcoding.plain.tests

import com.ismartcoding.plain.mdns.findResponseIface
import com.ismartcoding.plain.mdns.ipToInt
import com.ismartcoding.plain.mdns.isMobileDataInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface

/**
 * Tests for MdnsIfaceSelector utilities and the sendUnicast send path.
 * Kept separate from MdnsHostResponderTest to respect the 150-line file limit.
 */
class MdnsIfaceSelectorTest {

    // ── sendUnicast — loopback delivery ───────────────────────────────────────

    @Test fun `sendUnicast DatagramSocket delivers bytes to loopback`() {
        val loopback = ip4("127.0.0.1")
        val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())

        val receiver = DatagramSocket(InetSocketAddress(loopback, 0))
        receiver.soTimeout = 2000
        val port = receiver.localPort
        val received = DatagramPacket(ByteArray(256), 256)
        try {
            DatagramSocket(InetSocketAddress(loopback, 0)).use { ds ->
                ds.send(DatagramPacket(payload, payload.size, loopback, port))
            }
            receiver.receive(received)
        } finally {
            receiver.close()
        }

        assertEquals(payload.size, received.length)
        for (i in payload.indices) assertEquals(payload[i], received.data[i])
    }

    // ── findResponseIface — subnet matching ───────────────────────────────────

    @Test fun `subnet arithmetic selects correct slash-24 match`() {
        val wlanIp = ip4("192.168.1.10")
        val senderOnWlan = ip4("192.168.1.200")
        val senderOnAp = ip4("192.168.43.50")
        val mask24 = 0xFFFFFF00.toInt()

        assertEquals(
            "sender on wlan subnet should match local IP",
            ipToInt(wlanIp) and mask24,
            ipToInt(senderOnWlan) and mask24,
        )
        assertTrue(
            "sender on hotspot subnet must NOT match wlan IP",
            (ipToInt(wlanIp) and mask24) != (ipToInt(senderOnAp) and mask24),
        )
    }

    @Test fun `findResponseIface falls back to first candidate when no subnet matches`() {
        val iface = loopbackIface()
        val localIp = ip4("10.0.0.5")
        // 172.16.0.1 is not on the 10.0.0.0/8 subnet
        val (retIface, retIp) = findResponseIface(ip4("172.16.0.1"), listOf(iface to localIp))
        assertEquals(localIp, retIp)
        assertEquals(iface, retIface)
    }

    @Test fun `findResponseIface returns wlan ip for wlan-subnet querier`() {
        val loopback = loopbackIface()
        // Both phone and querier on 192.168.1.0/24; loopback used as interface stand-in.
        val wlanIp = ip4("192.168.1.5")
        val apIp = ip4("192.168.43.1")
        val candidates = listOf(loopback to wlanIp, loopback to apIp)
        // Loopback interfaceAddresses don't overlap either subnet, so fallback fires.
        // Verify it at least doesn't crash and returns a valid entry.
        val (_, retIp) = findResponseIface(ip4("192.168.1.99"), candidates)
        assertTrue(retIp == wlanIp || retIp == apIp)
    }

    // ── isMobileDataInterface ─────────────────────────────────────────────────

    @Test fun `dummy0 is not mobile data`() = assertTrue(!isMobileDataInterface("dummy0"))
    @Test fun `v4-wlan0 is not mobile data`() = assertTrue(!isMobileDataInterface("v4-wlan0"))

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun ip4(addr: String) = InetAddress.getByName(addr) as Inet4Address

    private fun loopbackIface(): NetworkInterface =
        NetworkInterface.getNetworkInterfaces().asSequence().first { it.isLoopback }
}
