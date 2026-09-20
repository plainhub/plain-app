package com.ismartcoding.plain.lib.mdns

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Locks the [MdnsServiceBrowser.handlePacket] state machine — the code that
 * decides whether a discovered peer is complete enough to reach NearbyPage.
 * An instance must accumulate id (TXT) + port (SRV) + addresses (A) before
 * [MdnsServiceBrowser.onDevice] fires; the debug page's snapshot() shows
 * incomplete instances too, which is why the two pages can disagree.
 */
class MdnsBrowserStateTest {

    private val localIp = "192.168.1.10"
    private val peerIp = "192.168.1.50"
    private val hostname = "plainapp-abc123.local"
    private val instanceFqdn = "Pixel 7 Pro.$PLAINAPP_SERVICE_TYPE"

    private val found = mutableListOf<MdnsFoundDevice>()

    @Before fun setUp() {
        MdnsHostResponder.resetForTest()
        MdnsServiceBrowser.resetForTest()
        MdnsHostResponder.logSink = {}
        interfacesProvider = { listOf(MdnsIface("wlan0", 24) to localIp) }
        MdnsServiceBrowser.onDevice = { found += it }
    }

    @After fun tearDown() {
        MdnsHostResponder.resetForTest()
        MdnsServiceBrowser.resetForTest()
        MdnsHostResponder.logSink = { println("mDNS: $it") }
        interfacesProvider = null
    }

    /** A full peer response: PTR answer + SRV/TXT/A additional (RFC 6763 §12). */
    private fun fullPeerResponse() = MdnsServiceResponseBuilder.buildResponseIfMatch(
        MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE),
        MdnsServiceInfo(
            instanceName = "Pixel 7 Pro",
            serviceType = PLAINAPP_SERVICE_TYPE,
            targetHostname = hostname,
            port = 8443,
            txtRecords = listOf("id=abc123", "dv=PHONE", "ver=1.2.3", "pf=android"),
            ips = listOf(peerIp),
        ),
    )!!.bytes

    @Test fun `complete response emits the device with every field`() {
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)

        val device = found.single()
        assertEquals("abc123", device.id)
        assertEquals("Pixel 7 Pro", device.name)
        assertEquals(8443, device.port)
        assertEquals(listOf(peerIp), device.ips)
        assertEquals("PHONE", device.deviceType)
        assertEquals("1.2.3", device.version)
        assertEquals("android", device.platform)
        assertTrue(MdnsServiceBrowser.snapshot().single().complete)
    }

    @Test fun `query packets are not responses and are ignored`() {
        MdnsServiceBrowser.handlePacket(MdnsPacketCodec.buildPtrQuery(PLAINAPP_SERVICE_TYPE), peerIp)

        assertTrue(found.isEmpty())
        assertTrue(MdnsServiceBrowser.snapshot().isEmpty())
    }

    @Test fun `own looped-back announcements are ignored`() {
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), localIp)

        assertTrue(found.isEmpty())
        assertTrue(MdnsServiceBrowser.snapshot().isEmpty())
    }

    @Test fun `PTR-only packet stays incomplete and silent`() {
        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf { ptr(PLAINAPP_SERVICE_TYPE, instanceFqdn) },
            peerIp,
        )

        val snapshot = MdnsServiceBrowser.snapshot().single()
        assertEquals("Pixel 7 Pro", snapshot.instanceName)
        assertFalse(snapshot.complete)
        assertTrue(found.isEmpty())
    }

    @Test fun `SRV TXT A follow-up packet completes the instance`() {
        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf { ptr(PLAINAPP_SERVICE_TYPE, instanceFqdn) },
            peerIp,
        )
        assertTrue(found.isEmpty())

        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf {
                srv(instanceFqdn, 8443, hostname)
                txt(instanceFqdn, listOf("id=abc123", "dv=PHONE", "ver=1.2.3", "pf=android"))
                a(hostname, listOf(peerIp))
            },
            peerIp,
        )

        assertEquals(1, found.size)
        assertEquals("abc123", found.single().id)
        assertTrue(MdnsServiceBrowser.snapshot().single().complete)
    }

    @Test fun `refresh announcements re-emit the device`() {
        // The 60s nearby-list cleanup keeps a device only while announcements
        // keep refreshing it — every complete packet must re-fire onDevice.
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)

        assertEquals(2, found.size)
        assertEquals(found[0].id, found[1].id)
    }

    @Test fun `A records replace the previous address set`() {
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)
        val newIp = "192.168.1.99"

        // A host that moved networks: the packet's A set is authoritative and
        // the stale address must be dropped, not merged.
        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf { a(hostname, listOf(newIp)) },
            peerIp,
        )

        assertEquals(listOf(newIp), MdnsServiceBrowser.snapshot().single().ips)
        assertEquals(newIp, found.last().ips.single())
    }

    @Test fun `goodbye withdraws the instance`() {
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)
        assertTrue(MdnsServiceBrowser.snapshot().isNotEmpty())

        MdnsServiceBrowser.handlePacket(
            MdnsServiceResponseBuilder.buildGoodbye(
                MdnsServiceInfo(
                    instanceName = "Pixel 7 Pro",
                    serviceType = PLAINAPP_SERVICE_TYPE,
                    targetHostname = hostname,
                    port = 8443,
                    txtRecords = listOf("id=abc123"),
                    ips = listOf(peerIp),
                ),
            ),
            peerIp,
        )

        assertTrue(MdnsServiceBrowser.snapshot().isEmpty())
    }

    @Test fun `orphan A records after goodbye do not resurrect the instance`() {
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)
        assertTrue(MdnsServiceBrowser.snapshot().isNotEmpty())

        // Goodbye removes the instance AND its hostname mapping, so a later
        // A packet for the old hostname finds no instance to attach to.
        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf {
                ptr(PLAINAPP_SERVICE_TYPE, instanceFqdn, ttl = 0)
                srv(instanceFqdn, 8443, hostname, ttl = 0)
                txt(instanceFqdn, listOf("id=abc123"), ttl = 0)
            },
            peerIp,
        )
        assertTrue(MdnsServiceBrowser.snapshot().isEmpty())

        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf { a(hostname, listOf(peerIp)) },
            peerIp,
        )

        assertTrue(MdnsServiceBrowser.snapshot().isEmpty())
    }

    @Test fun `goodbye and live announcement in one packet drop only the withdrawn instance`() {
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)

        val oldFqdn = instanceFqdn
        val newName = "Pixel 8"
        val newFqdn = "$newName.$PLAINAPP_SERVICE_TYPE"
        val newHost = "plainapp-def456.local"

        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf {
                ptr(PLAINAPP_SERVICE_TYPE, oldFqdn, ttl = 0)
                srv(oldFqdn, 8443, hostname, ttl = 0)
                txt(oldFqdn, listOf("id=abc123"), ttl = 0)
                ptr(PLAINAPP_SERVICE_TYPE, newFqdn)
                srv(newFqdn, 8443, newHost)
                txt(newFqdn, listOf("id=def456", "dv=PHONE"))
                a(newHost, listOf(peerIp))
            },
            peerIp,
        )

        val snapshot = MdnsServiceBrowser.snapshot().single()
        // Instance keys are stored lowercased (instanceKeyOf normalizes case).
        assertEquals("pixel 8.$PLAINAPP_SERVICE_TYPE", snapshot.instanceFqdn)
        assertTrue(snapshot.complete)
        assertEquals("def456", found.last().id)
    }

    @Test fun `foreign service type records are ignored`() {
        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf { ptr("_airplay._tcp.local", "Speaker._airplay._tcp.local") },
            peerIp,
        )

        assertTrue(MdnsServiceBrowser.snapshot().isEmpty())
        assertTrue(found.isEmpty())
    }

    @Test fun `snapshots sort by instance fqdn`() {
        MdnsServiceBrowser.handlePacket(fullPeerResponse(), peerIp)
        MdnsServiceBrowser.handlePacket(
            mdnsResponseOf {
                ptr(PLAINAPP_SERVICE_TYPE, "alpha.$PLAINAPP_SERVICE_TYPE")
                srv("alpha.$PLAINAPP_SERVICE_TYPE", 9000, "plainapp-a.local")
                txt("alpha.$PLAINAPP_SERVICE_TYPE", listOf("id=aaa"))
                a("plainapp-a.local", listOf("192.168.1.61"))
            },
            peerIp,
        )

        val names = MdnsServiceBrowser.snapshot().map { it.instanceFqdn }
        // Sorted by the lowercased instance key.
        assertEquals(
            listOf("alpha.$PLAINAPP_SERVICE_TYPE", "pixel 7 pro.$PLAINAPP_SERVICE_TYPE"),
            names,
        )
    }
}
