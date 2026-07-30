package com.ismartcoding.plain.features.dlna.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DlnaDeviceParsingTest {

    private val sampleResponse = (
        "HTTP/1.1 200 OK\r\n" +
            "CACHE-CONTROL: max-age=1800\r\n" +
            "LOCATION: http://192.168.1.50:7878/description.xml\r\n" +
            "SERVER: Android/1.0 UPnP/1.1 PlainApp/1.0\r\n" +
            "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n" +
            "USN: uuid:12345678-1234-4123-8123-123456789abc::urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"
        )

    // ── Header parsing ───

    @Test
    fun `parses LOCATION header from SSDP response`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        assertEquals("http://192.168.1.50:7878/description.xml", device.location.trim())
    }

    @Test
    fun `parses USN header from SSDP response`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        assertTrue(device.uSN.contains("uuid:12345678-1234-4123-8123-123456789abc"))
    }

    @Test
    fun `parses ST header from SSDP response`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        assertEquals("urn:schemas-upnp-org:device:MediaRenderer:1", device.sT.trim())
    }

    @Test
    fun `parses SERVER header from SSDP response`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        assertTrue(device.server.contains("PlainApp"))
    }

    @Test
    fun `missing header returns empty string`() {
        val device = DlnaDevice("10.0.0.1", "HTTP/1.1 200 OK\r\n\r\n")
        assertEquals("", device.location)
        assertEquals("", device.uSN)
        assertEquals("", device.sT)
    }

    // ── getBaseUrl ───

    @Test
    fun `getBaseUrl extracts host and port from LOCATION`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        assertEquals("http://192.168.1.50:7878", device.getBaseUrl())
    }

    @Test
    fun `getBaseUrl handles URL with path after host port`() {
        val response = "HTTP/1.1 200 OK\r\nLOCATION: http://10.0.0.5:5000/desc.xml\r\n\r\n"
        val device = DlnaDevice("10.0.0.5", response)
        assertEquals("http://10.0.0.5:5000", device.getBaseUrl())
    }

    @Test
    fun `getBaseUrl returns empty string when LOCATION is missing`() {
        val device = DlnaDevice("10.0.0.1", "HTTP/1.1 200 OK\r\n\r\n")
        assertEquals("", device.getBaseUrl())
    }

    // ── isAVTransport ───

    @Test
    fun `isAVTransport returns false when description is not loaded`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        assertFalse(device.isAVTransport(), "should be false before description XML is fetched")
    }

    @Test
    fun `isAVTransport returns true when AVTransport service is present`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        val xml = (
            "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">" +
                "<device><deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>" +
                "<friendlyName>Pixel</friendlyName>" +
                "<serviceList><service>" +
                "<serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>" +
                "<serviceId>urn:upnp-org:serviceId:AVTransport</serviceId>" +
                "<controlURL>/AVTransport/control</controlURL>" +
                "</service></serviceList>" +
                "</device></root>"
            )
        device.update(xml)
        assertTrue(device.isAVTransport(), "should be true when AVTransport service is in description")
    }

    @Test
    fun `getAVTransportService returns the service with matching serviceId`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        val xml = (
            "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">" +
                "<device><deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>" +
                "<friendlyName>Pixel</friendlyName>" +
                "<serviceList><service>" +
                "<serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>" +
                "<serviceId>urn:upnp-org:serviceId:AVTransport</serviceId>" +
                "<controlURL>/AVTransport/control</controlURL>" +
                "</service></serviceList>" +
                "</device></root>"
            )
        device.update(xml)
        val service = device.getAVTransportService()
        assertEquals("urn:upnp-org:serviceId:AVTransport", service?.serviceId)
        assertEquals("/AVTransport/control", service?.controlURL)
    }

    @Test
    fun `getAVTransportService returns null when no AVTransport service`() {
        val device = DlnaDevice("192.168.1.50", sampleResponse)
        val xml = (
            "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">" +
                "<device><deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>" +
                "<friendlyName>Pixel</friendlyName>" +
                "<serviceList></serviceList>" +
                "</device></root>"
            )
        device.update(xml)
        assertNull(device.getAVTransportService())
    }
}
