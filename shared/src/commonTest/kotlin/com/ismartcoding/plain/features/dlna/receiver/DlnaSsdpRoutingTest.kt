package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.features.dlna.DlnaRendererState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the M-SEARCH response routing bug.
 *
 * Bug: the receiver used to parse the M-SEARCH `HOST` header (239.255.255.250:1900,
 * the multicast group) and send search responses there. But the sender's socket
 * is bound to a random ephemeral port (not 1900), so it could never receive
 * multicast packets sent to port 1900 — discovery silently failed.
 *
 * Fix: responses must be **unicasted** to the source address:port of the M-SEARCH
 * datagram (per UPnP spec), which [DlnaSsdpPacket.sourceAddress]/[sourcePort]
 * captures from the incoming UDP packet.
 */
class DlnaSsdpRoutingTest {

    private val testUuid = "12345678-1234-4123-8123-123456789abc"

    @BeforeTest
    fun setup() {
        DlnaRendererState.port.value = 7878
    }

    @AfterTest
    fun teardown() {
        DlnaRendererState.reset()
    }

    @Test
    fun `M-SEARCH response is unicasted to sender source address and port`() {
        val socket = FakeSsdpSocket()
        val packet = DlnaSsdpPacket(
            message = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: ssdp:all\r\n\r\n",
            sourceAddress = "192.168.1.100",
            sourcePort = 54321,
        )

        DlnaReceiverEngine.handleSsdpPacket(packet, socket, testUuid)

        assertEquals(3, socket.unicastSends.size, "should send 3 search responses")
        socket.unicastSends.forEach { (message, address, port) ->
            assertEquals("192.168.1.100", address, "response must go to sender's source IP, not the multicast group")
            assertEquals(54321, port, "response must go to sender's source port, not port 1900")
            assertTrue(message.startsWith("HTTP/1.1 200 OK"), "response must be an HTTP 200 OK")
        }
    }

    @Test
    fun `M-SEARCH response is NOT sent to multicast group`() {
        val socket = FakeSsdpSocket()
        val packet = DlnaSsdpPacket(
            message = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: ssdp:all\r\n\r\n",
            sourceAddress = "10.0.0.5",
            sourcePort = 38111,
        )

        DlnaReceiverEngine.handleSsdpPacket(packet, socket, testUuid)

        // No multicast sends should happen in response to an M-SEARCH
        assertEquals(0, socket.multicastSends.size, "M-SEARCH must not trigger any multicast sends")
        // And specifically, nothing should be sent to the SSDP multicast address:port
        socket.unicastSends.forEach { (_, address, port) ->
            assertTrue(address != "239.255.255.250", "must not unicast to the multicast group IP")
            assertTrue(port != 1900, "must not send to port 1900 (the multicast port)")
        }
    }

    @Test
    fun `M-SEARCH responses contain correct UUID in USN header`() {
        val socket = FakeSsdpSocket()
        val packet = DlnaSsdpPacket(
            message = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nST: ssdp:all\r\n\r\n",
            sourceAddress = "192.168.1.50",
            sourcePort = 9999,
        )

        DlnaReceiverEngine.handleSsdpPacket(packet, socket, testUuid)

        socket.unicastSends.forEach { (message, _, _) ->
            assertTrue(message.contains("USN: $testUuid"), "USN header must contain the device UUID")
        }
    }

    @Test
    fun `non-M-SEARCH packet does not trigger any response`() {
        val socket = FakeSsdpSocket()
        val packet = DlnaSsdpPacket(
            message = "NOTIFY * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nNT: ssdp:alive\r\n\r\n",
            sourceAddress = "192.168.1.200",
            sourcePort = 1900,
        )

        DlnaReceiverEngine.handleSsdpPacket(packet, socket, testUuid)

        assertEquals(0, socket.unicastSends.size, "NOTIFY must not trigger unicast responses")
        assertEquals(0, socket.multicastSends.size, "NOTIFY must not trigger multicast sends")
    }

    @Test
    fun `M-SEARCH from ephemeral port reaches correct destination`() {
        val socket = FakeSsdpSocket()
        // Simulate a real sender: socket on random port 49152, M-SEARCH to multicast group
        val senderPort = 49152
        val packet = DlnaSsdpPacket(
            message = "M-SEARCH * HTTP/1.1\r\nST: ssdp:all\r\nHOST: 239.255.255.250:1900\r\nMX: 3\r\nMAN: \"ssdp:discover\"\r\n\r\n",
            sourceAddress = "192.168.123.23",
            sourcePort = senderPort,
        )

        DlnaReceiverEngine.handleSsdpPacket(packet, socket, testUuid)

        assertEquals(3, socket.unicastSends.size)
        // The critical assertion: response goes to the sender's ephemeral port,
        // NOT to port 1900 (which the sender cannot receive on).
        socket.unicastSends.forEach { (_, address, port) ->
            assertEquals("192.168.123.23", address)
            assertEquals(senderPort, port, "must unicast to sender's ephemeral port, not 1900")
        }
    }
}

/** Records all send calls for verification; never actually sends anything. */
private class FakeSsdpSocket : DlnaSsdpSocket {
    val multicastSends = mutableListOf<String>()
    val unicastSends = mutableListOf<Triple<String, String, Int>>()

    override suspend fun receive(timeoutMs: Int): DlnaSsdpPacket? = null
    override fun sendMulticast(message: String) { multicastSends.add(message) }
    override fun sendUnicast(message: String, address: String, port: Int) {
        unicastSends.add(Triple(message, address, port))
    }
    override fun close() {}
}
