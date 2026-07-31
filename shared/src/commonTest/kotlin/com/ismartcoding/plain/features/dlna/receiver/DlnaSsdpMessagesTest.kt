package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.features.dlna.DlnaRendererState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DlnaSsdpMessagesTest {

    private val testUuid = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"

    @BeforeTest
    fun setup() {
        DlnaRendererState.port.value = 7878
    }

    @AfterTest
    fun teardown() {
        DlnaRendererState.reset()
    }

    // ── aliveMessages ───

    @Test
    fun `aliveMessages returns three NOTIFY ssdp alive messages`() {
        val messages = DlnaSsdpMessages.aliveMessages(testUuid)

        assertEquals(3, messages.size)
        messages.forEach { msg ->
            assertTrue(msg.startsWith("NOTIFY * HTTP/1.1"), "should start with NOTIFY")
            assertTrue(msg.contains("NTS: ssdp:alive"), "should be ssdp:alive")
            assertTrue(msg.contains("HOST: ${DlnaSsdpMessages.SSDP_ADDR}:${DlnaSsdpMessages.SSDP_PORT}"))
        }
    }

    @Test
    fun `aliveMessages includes root device and device type and service entries`() {
        val messages = DlnaSsdpMessages.aliveMessages(testUuid)

        assertTrue(messages.any { it.contains("NT: upnp:rootdevice") }, "should include root device")
        assertTrue(messages.any { it.contains("NT: ${DlnaSsdpMessages.DEVICE_TYPE}") }, "should include device type")
        assertTrue(messages.any { it.contains("NT: ${DlnaSsdpMessages.AVT_TYPE}") }, "should include AVTransport service")
    }

    @Test
    fun `aliveMessages USN contains uuid`() {
        val messages = DlnaSsdpMessages.aliveMessages(testUuid)

        messages.forEach { msg ->
            assertTrue(msg.contains("USN: $testUuid"), "USN should contain the uuid")
        }
    }

    @Test
    fun `aliveMessages LOCATION contains device port`() {
        val messages = DlnaSsdpMessages.aliveMessages(testUuid)

        messages.forEach { msg ->
            assertTrue(msg.contains("LOCATION: http://"), "LOCATION should be an HTTP URL")
            assertTrue(msg.contains(":7878/description.xml"), "LOCATION should contain the port and path")
        }
    }

    // ── byebyeMessages ───

    @Test
    fun `byebyeMessages returns three NOTIFY ssdp byebye messages`() {
        val messages = DlnaSsdpMessages.byebyeMessages(testUuid)

        assertEquals(3, messages.size)
        messages.forEach { msg ->
            assertTrue(msg.startsWith("NOTIFY * HTTP/1.1"))
            assertTrue(msg.contains("NTS: ssdp:byebye"), "should be ssdp:byebye")
        }
    }

    @Test
    fun `byebyeMessages does not contain alive`() {
        val messages = DlnaSsdpMessages.byebyeMessages(testUuid)

        messages.forEach { msg ->
            assertFalse(msg.contains("ssdp:alive"), "byebye must not contain ssdp:alive")
        }
    }

    // ── searchResponses ───

    @Test
    fun `searchResponses returns three HTTP 200 OK responses`() {
        val responses = DlnaSsdpMessages.searchResponses(testUuid)

        assertEquals(3, responses.size)
        responses.forEach { msg ->
            assertTrue(msg.startsWith("HTTP/1.1 200 OK"), "search response must be HTTP 200 OK")
            assertTrue(msg.contains("CACHE-CONTROL: max-age=1800"))
            assertTrue(msg.contains("LOCATION: http://"), "should include LOCATION URL")
        }
    }

    @Test
    fun `searchResponses include ST and USN headers with uuid`() {
        val responses = DlnaSsdpMessages.searchResponses(testUuid)

        assertTrue(responses.any { it.contains("ST: upnp:rootdevice") })
        assertTrue(responses.any { it.contains("ST: ${DlnaSsdpMessages.DEVICE_TYPE}") })
        assertTrue(responses.any { it.contains("ST: ${DlnaSsdpMessages.AVT_TYPE}") })
        responses.forEach { msg ->
            assertTrue(msg.contains("USN: $testUuid"), "USN should contain the uuid")
        }
    }

    @Test
    fun `searchResponses LOCATION contains configured port`() {
        DlnaRendererState.port.value = 7879
        val responses = DlnaSsdpMessages.searchResponses(testUuid)

        responses.forEach { msg ->
            assertTrue(msg.contains(":7879/description.xml"), "LOCATION should reflect the current port")
        }
    }
}
