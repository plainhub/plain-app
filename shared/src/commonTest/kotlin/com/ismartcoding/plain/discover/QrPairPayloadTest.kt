package com.ismartcoding.plain.discover

import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrPairPayloadTest {

    @Test fun `parses a full plainapp code`() {
        val payload = QrPairPayload.parse(
            "plainapp://pair?v=1&id=epq4h39gdlop&name=Mac%20Book%20Pro&ips=192.168.123.22,198.19.0.33&port=8443",
        )
        assertNotNull(payload)
        assertEquals("epq4h39gdlop", payload.id)
        assertEquals("Mac Book Pro", payload.name)
        assertEquals(listOf("192.168.123.22", "198.19.0.33"), payload.ips)
        assertEquals(8443, payload.port)
    }

    @Test fun `parses percent-encoded non-ascii name`() {
        val payload = QrPairPayload.parse("plainapp://pair?v=1&id=x&name=%E4%B8%AD%E6%96%87&ips=10.0.0.2&port=80")
        assertNotNull(payload)
        assertEquals("中文", payload.name)
    }

    @Test fun `rejects non plainapp text`() {
        assertNull(QrPairPayload.parse("https://example.com/pair?v=1"))
        assertNull(QrPairPayload.parse("hello world"))
    }

    @Test fun `rejects codes missing required fields`() {
        assertNull(QrPairPayload.parse("plainapp://pair?v=1&name=Mac&ips=10.0.0.2&port=8443"))
        assertNull(QrPairPayload.parse("plainapp://pair?v=1&id=x&name=Mac&ips=&port=8443"))
        assertNull(QrPairPayload.parse("plainapp://pair?v=1&id=x&name=Mac&ips=10.0.0.2&port=abc"))
    }

    @Test fun `toDevice builds a computer discovered via QR`() {
        val payload = QrPairPayload.parse(
            "plainapp://pair?v=1&id=abc&name=Desk&ips=192.168.1.5&port=8443",
        )!!
        val device = payload.toDevice()
        assertEquals("abc", device.id)
        assertEquals("Desk", device.name)
        assertEquals(listOf("192.168.1.5"), device.ips)
        assertEquals(8443, device.port)
        assertEquals(DeviceType.COMPUTER, device.deviceType)
        assertTrue(DiscoveryMethod.QR in device.discoveryMethods)
    }
}
