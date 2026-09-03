package com.ismartcoding.plain.lib.mdns

import com.ismartcoding.plain.lib.mdns.MdnsPacketCodec
import com.ismartcoding.plain.lib.mdns.MdnsServiceInfo
import com.ismartcoding.plain.lib.mdns.MdnsServiceResponseBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MdnsServiceCodecTest {

    private val service = MdnsServiceInfo(
        instanceName = "Pixel 7 Pro",
        serviceType = "_plainapp._tcp.local",
        targetHostname = "plainapp-abc123.local",
        port = 8443,
        txtRecords = listOf("id=abc123", "dv=PHONE", "ver=1.2.3", "pf=android"),
        ips = listOf("192.168.1.50"),
    )

    private fun assertQuestion(query: ByteArray, name: String, qtype: Int) {
        val questions = MdnsPacketCodec.readQuestions(query)
        assertNotNull(questions)
        val q = questions!!.single()
        assertEquals(name, q.name)
        assertEquals(qtype, q.qtype)
        assertEquals(MdnsPacketCodec.DNS_CLASS_IN, q.qclass)
    }

    // ── Query builders ────────────────────────────────────────────────────────

    @Test fun `PTR query asks for the service type`() =
        assertQuestion(MdnsPacketCodec.buildPtrQuery(service.serviceType), service.serviceType, MdnsPacketCodec.TYPE_PTR)

    @Test fun `SRV query asks for the instance fqdn`() =
        assertQuestion(MdnsPacketCodec.buildSrvQuery(service.instanceName, service.serviceType), service.instanceFqdn, MdnsPacketCodec.TYPE_SRV)

    @Test fun `TXT query asks for the instance fqdn`() =
        assertQuestion(MdnsPacketCodec.buildTxtQuery(service.instanceName, service.serviceType), service.instanceFqdn, MdnsPacketCodec.TYPE_TXT)

    // ── Browser follow-up round-trip (regression: double-suffixed names) ─────
    // The browser sends SRV/TXT follow-ups after learning an instance from a
    // PTR answer. It must pass the SHORT instance name to the query builders
    // (they append the service type); passing the full FQDN produces a
    // double-suffixed name the responder never matches, so instances stay
    // incomplete forever and NearbyPage shows nothing.

    @Test fun `browser SRV follow-up query is answered`() {
        val query = MdnsPacketCodec.buildSrvQuery(service.instanceName, service.serviceType)
        assertNotNull(MdnsServiceResponseBuilder.buildResponseIfMatch(query, service))
    }

    @Test fun `browser TXT follow-up query is answered`() {
        val query = MdnsPacketCodec.buildTxtQuery(service.instanceName, service.serviceType)
        assertNotNull(MdnsServiceResponseBuilder.buildResponseIfMatch(query, service))
    }

    @Test fun `double-suffixed FQDN query is not answered`() {
        val query = MdnsPacketCodec.buildSrvQuery(service.instanceFqdn, service.serviceType)
        assertNull(MdnsServiceResponseBuilder.buildResponseIfMatch(query, service))
    }

    @Test fun `query with unicast bit sets QU flag`() {
        val questions = MdnsPacketCodec.readQuestions(
            MdnsPacketCodec.buildQuery("_plainapp._tcp.local", MdnsPacketCodec.TYPE_PTR, unicastResponse = true)
        )!!
        assertTrue(questions.single().unicastResponseRequested)
    }

    // ── PTR response round-trip (the discovery path) ──────────────────────────

    @Test fun `PTR query yields PTR record plus A additional`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildPtrQuery(service.serviceType),
            service,
        )
        assertNotNull(response)
        val parsed = MdnsPacketCodec.parseResponse(response!!.bytes)
        assertNotNull(parsed)
        assertTrue(parsed!!.isResponse)

        val ptr = parsed.answers.single()
        assertEquals(MdnsPacketCodec.TYPE_PTR, ptr.type)
        assertEquals(service.serviceType, ptr.name)
        assertEquals(service.instanceFqdn, ptr.ptrTarget)
        // RFC 6762 §10.2: PTR rnames are shared, so the cache-flush bit must be clear.
        assertFalse("PTR record must not carry the cache-flush bit", ptr.cacheFlush)

        val a = parsed.additional.single { it.type == MdnsPacketCodec.TYPE_A }
        assertEquals(service.targetHostname, a.name)
        assertEquals(service.ips.single(), a.ip)
    }

    @Test fun `PTR query with QU bit requests a unicast response`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildQuery(service.serviceType, MdnsPacketCodec.TYPE_PTR, unicastResponse = true),
            service,
        )
        assertNotNull(response)
        assertTrue(response!!.unicastResponseRequested)
    }

    @Test fun `plain PTR query does not request unicast response`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildPtrQuery(service.serviceType),
            service,
        )!!
        assertFalse(response.unicastResponseRequested)
    }

    // ── SRV / TXT / A responses ───────────────────────────────────────────────

    @Test fun `SRV query yields SRV answer plus A additional`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildSrvQuery(service.instanceName, service.serviceType),
            service,
        )!!
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)!!
        assertEquals(1, parsed.answers.size)
        assertEquals(MdnsPacketCodec.TYPE_SRV, parsed.answers.single().type)
        assertEquals(service.port, parsed.answers.single().srv!!.port)
        assertEquals(service.ips.single(), parsed.additional.single { it.type == MdnsPacketCodec.TYPE_A }.ip)
    }

    @Test fun `TXT query yields TXT record`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildTxtQuery(service.instanceName, service.serviceType),
            service,
        )!!
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)!!
        assertEquals(1, parsed.answers.size)
        assertEquals(service.txtRecords, parsed.answers.single().txtStrings)
    }

    @Test fun `A query yields A record`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildQuery(service.targetHostname, MdnsPacketCodec.TYPE_A),
            service,
        )!!
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)!!
        assertEquals(1, parsed.answers.size)
        assertEquals(service.targetHostname, parsed.answers.single().name)
        assertEquals(service.ips.single(), parsed.answers.single().ip)
    }

    @Test fun `ANY query for the service type is scoped to the queried name`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildQuery(service.serviceType, MdnsPacketCodec.TYPE_ANY),
            service,
        )!!
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)!!
        // RFC 6762 §6: only records whose name matches the question are
        // answered — the PTR (service type). RFC 6763 §12: SRV/TXT/A ride in
        // the additional section so one query resolves the full service.
        assertEquals(listOf(MdnsPacketCodec.TYPE_PTR), parsed.answers.map { it.type })
        assertEquals(
            listOf(MdnsPacketCodec.TYPE_SRV, MdnsPacketCodec.TYPE_TXT, MdnsPacketCodec.TYPE_A),
            parsed.additional.map { it.type },
        )
    }

    @Test fun `ANY query for the instance fqdn yields SRV TXT and A`() {
        val response = MdnsServiceResponseBuilder.buildResponseIfMatch(
            MdnsPacketCodec.buildQuery(service.instanceFqdn, MdnsPacketCodec.TYPE_ANY),
            service,
        )!!
        val parsed = MdnsPacketCodec.parseResponse(response.bytes)!!
        assertEquals(
            setOf(MdnsPacketCodec.TYPE_SRV, MdnsPacketCodec.TYPE_TXT),
            parsed.answers.map { it.type }.toSet(),
        )
        assertEquals(listOf(MdnsPacketCodec.TYPE_A), parsed.additional.map { it.type })
    }

    @Test fun `ANY query for an unrelated name yields null`() {
        assertNull(
            MdnsServiceResponseBuilder.buildResponseIfMatch(
                MdnsPacketCodec.buildQuery("_other._tcp.local", MdnsPacketCodec.TYPE_ANY),
                service,
            ),
        )
    }

    // ── Goodbye (RFC 6762 §8.4) ──────────────────────────────────────────────
    // Sent when a published instance is replaced by one with a different
    // instance FQDN (device renamed): TTL=0 makes peers drop the stale records
    // at once instead of listing the old name until the 120s TTL expires.

    @Test fun `goodbye carries PTR SRV and TXT with zero TTL`() {
        val parsed = MdnsPacketCodec.parseResponse(MdnsServiceResponseBuilder.buildGoodbye(service))!!
        assertTrue(parsed.isResponse)
        assertEquals(
            listOf(MdnsPacketCodec.TYPE_PTR, MdnsPacketCodec.TYPE_SRV, MdnsPacketCodec.TYPE_TXT),
            parsed.answers.map { it.type },
        )
        parsed.answers.forEach { assertEquals(0L, it.ttl) }
        assertEquals(service.instanceFqdn, parsed.answers.first().ptrTarget)
    }

    @Test fun `goodbye withdraws the renamed instance only`() {
        val renamed = service.copy(instanceName = "Pixel 8")
        val parsed = MdnsPacketCodec.parseResponse(MdnsServiceResponseBuilder.buildGoodbye(service))!!
        val ptr = parsed.answers.single { it.type == MdnsPacketCodec.TYPE_PTR }
        assertEquals(service.instanceFqdn, ptr.ptrTarget)
        assertEquals(service.instanceFqdn, parsed.answers.single { it.type == MdnsPacketCodec.TYPE_SRV }.name)
        assertNotEquals(renamed.instanceFqdn, ptr.ptrTarget)
    }

    // ── Negative cases ────────────────────────────────────────────────────────

    @Test fun `unrelated query yields null`() {
        val query = MdnsPacketCodec.buildPtrQuery("_other._tcp.local")
        assertNull(MdnsServiceResponseBuilder.buildResponseIfMatch(query, service))
    }

    @Test fun `empty IP list yields null`() {
        val bare = service.copy(ips = emptyList())
        assertNull(MdnsServiceResponseBuilder.buildResponseIfMatch(MdnsPacketCodec.buildPtrQuery(service.serviceType), bare))
    }

    @Test fun `incoming response packet yields null`() {
        val query = MdnsPacketCodec.buildPtrQuery(service.serviceType)
        query[2] = 0x84.toByte() // QR=1 — a response, not a query
        assertNull(MdnsServiceResponseBuilder.buildResponseIfMatch(query, service))
    }

    // ── Name compression in responses ─────────────────────────────────────────

    @Test fun `parseResponse resolves compressed record names`() {
        val hostname = "plainapp-abc.local"
        val nameBytes = MdnsPacketCodec.encodeName(hostname)

        // Header (12 bytes) + two A records. The first record's name lands at
        // offset 12; the second record's name is a compression pointer (0xC00C)
        // back to that offset — the name is written once, then referenced.
        val out = mutableListOf<Byte>()
        MdnsPacketCodec.writeHeader(out, answers = 2, additional = 0)
        MdnsPacketCodec.writeRecord(out, nameBytes, MdnsPacketCodec.TYPE_A, 0x8001, 120, ipToBytesLocal("192.168.1.5"))
        out.addAll(byteArrayOf(0xC0.toByte(), 0x0C.toByte()).toList())
        MdnsPacketCodec.writeU16(out, MdnsPacketCodec.TYPE_A)
        MdnsPacketCodec.writeU16(out, 0x8001)
        MdnsPacketCodec.writeU32(out, 120)
        MdnsPacketCodec.writeU16(out, 4)
        out.addAll(ipToBytesLocal("192.168.1.6").toList())

        val parsed = MdnsPacketCodec.parseResponse(out.toByteArray())!!
        assertEquals(2, parsed.answers.size)
        parsed.answers.forEach { assertEquals(hostname, it.name) }
        assertEquals(listOf("192.168.1.5", "192.168.1.6"), parsed.answers.map { it.ip })
    }

    private fun ipToBytesLocal(ip: String): ByteArray =
        ip.split(".").map { it.toInt().toByte() }.toByteArray()
}
