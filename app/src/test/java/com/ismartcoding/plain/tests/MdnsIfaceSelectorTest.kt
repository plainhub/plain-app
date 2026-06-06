package com.ismartcoding.plain.tests

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
 * Tests for MdnsIfaceSelector utilities (candidateInterfaces helpers, subnet arithmetic).
 * Kept separate from MdnsHostResponderTest to respect the 150-line file limit.
 */
class MdnsIfaceSelectorTest {

    // ── DatagramSocket loopback delivery (sanity: kernel UDP works) ───────────

    @Test fun `DatagramSocket delivers bytes to loopback`() {
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

    // ── isMobileDataInterface ─────────────────────────────────────────────────

    @Test fun `dummy0 is not mobile data`() = assertTrue(!isMobileDataInterface("dummy0"))
    @Test fun `v4-wlan0 is not mobile data`() = assertTrue(!isMobileDataInterface("v4-wlan0"))

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun ip4(addr: String) = InetAddress.getByName(addr) as Inet4Address

    private fun loopbackIface(): NetworkInterface =
        NetworkInterface.getNetworkInterfaces().asSequence().first { it.isLoopback }
}
