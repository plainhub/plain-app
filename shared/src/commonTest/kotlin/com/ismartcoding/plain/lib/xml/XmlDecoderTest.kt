package com.ismartcoding.plain.lib.xml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XmlDecoderTest {

    // ── Basic flat object ──────────────────────────────────────────

    @Serializable
    data class Person(val name: String = "", val age: Int = 0)

    @Test
    fun `decodes simple flat object`() {
        val xml = "<root><name>Alice</name><age>30</age></root>"
        val p = decodeXml<Person>(xml)
        assertEquals("Alice", p.name)
        assertEquals(30, p.age)
    }

    @Test
    fun `missing fields use defaults`() {
        val xml = "<root><name>Bob</name></root>"
        val p = decodeXml<Person>(xml)
        assertEquals("Bob", p.name)
        assertEquals(0, p.age)
    }

    // ── @SerialName mapping ────────────────────────────────────────

    @Serializable
    data class TransportInfo(
        @SerialName("CurrentTransportState") val state: String = "",
        @SerialName("CurrentSpeed") val speed: Int = 0,
    )

    @Test
    fun `maps fields via SerialName`() {
        val xml = """
            <u:GetTransportInfoResponse xmlns:u="urn:test">
                <CurrentTransportState>PLAYING</CurrentTransportState>
                <CurrentSpeed>2</CurrentSpeed>
            </u:GetTransportInfoResponse>
        """.trimIndent()
        val r = decodeXml<TransportInfo>(xml)
        assertEquals("PLAYING", r.state)
        assertEquals(2, r.speed)
    }

    // ── Nested objects ─────────────────────────────────────────────

    @Serializable
    data class Service(val serviceType: String = "", val controlURL: String = "")

    @Serializable
    data class Device(val deviceType: String = "", val friendlyName: String = "", val service: Service = Service())

    @Serializable
    data class DeviceDescription(val device: Device = Device(), val URLBase: String = "")

    @Test
    fun `decodes nested objects`() {
        val xml = """
            <root>
                <URLBase>http://192.168.1.1:80</URLBase>
                <device>
                    <deviceType>urn:test:device</deviceType>
                    <friendlyName>My Device</friendlyName>
                    <service>
                        <serviceType>urn:test:service</serviceType>
                        <controlURL>/control</controlURL>
                    </service>
                </device>
            </root>
        """.trimIndent()
        val desc = decodeXml<DeviceDescription>(xml)
        assertEquals("http://192.168.1.1:80", desc.URLBase)
        assertEquals("urn:test:device", desc.device.deviceType)
        assertEquals("My Device", desc.device.friendlyName)
        assertEquals("urn:test:service", desc.device.service.serviceType)
        assertEquals("/control", desc.device.service.controlURL)
    }

    // ── List fields: wrapper element pattern ───────────────────────

    @Serializable
    data class DeviceService(
        val serviceType: String = "",
        val serviceId: String = "",
    )

    @Serializable
    data class DeviceWithServices(
        val deviceType: String = "",
        val serviceList: List<DeviceService> = emptyList(),
    )

    @Test
    fun `decodes list via wrapper element`() {
        val xml = """
            <root>
                <deviceType>urn:test</deviceType>
                <serviceList>
                    <service>
                        <serviceType>typeA</serviceType>
                        <serviceId>idA</serviceId>
                    </service>
                    <service>
                        <serviceType>typeB</serviceType>
                        <serviceId>idB</serviceId>
                    </service>
                </serviceList>
            </root>
        """.trimIndent()
        val d = decodeXml<DeviceWithServices>(xml)
        assertEquals(2, d.serviceList.size)
        assertEquals("typeA", d.serviceList[0].serviceType)
        assertEquals("idB", d.serviceList[1].serviceId)
    }

    @Test
    fun `list with single wrapper child still produces array`() {
        val xml = """
            <root>
                <serviceList>
                    <service>
                        <serviceType>only</serviceType>
                        <serviceId>one</serviceId>
                    </service>
                </serviceList>
            </root>
        """.trimIndent()
        val d = decodeXml<DeviceWithServices>(xml)
        assertEquals(1, d.serviceList.size)
        assertEquals("only", d.serviceList[0].serviceType)
    }

    @Test
    fun `empty wrapper produces empty list`() {
        val xml = "<root><serviceList></serviceList></root>"
        val d = decodeXml<DeviceWithServices>(xml)
        assertTrue(d.serviceList.isEmpty())
    }

    // ── List fields: same-name siblings pattern ────────────────────

    @Serializable
    data class TagsContainer(val tags: List<String> = emptyList())

    @Test
    fun `decodes list via same-name siblings`() {
        val xml = "<root><tags>a</tags><tags>b</tags><tags>c</tags></root>"
        val c = decodeXml<TagsContainer>(xml)
        assertEquals(listOf("a", "b", "c"), c.tags)
    }

    @Test
    fun `single same-name element produces single-item list`() {
        val xml = "<root><tags>solo</tags></root>"
        val c = decodeXml<TagsContainer>(xml)
        assertEquals(listOf("solo"), c.tags)
    }

    // ── Primitive type conversion ──────────────────────────────────

    @Serializable
    data class Primitives(
        val i: Int = 0,
        val l: Long = 0L,
        val d: Double = 0.0,
        val f: Float = 0f,
        val b: Boolean = false,
    )

    @Test
    fun `converts primitive types`() {
        val xml = "<root><i>42</i><l>9999999999</l><d>3.14</d><f>2.5</f><b>true</b></root>"
        val p = decodeXml<Primitives>(xml)
        assertEquals(42, p.i)
        assertEquals(9999999999L, p.l)
        assertEquals(3.14, p.d, 0.001)
        assertEquals(2.5f, p.f, 0.001f)
        assertTrue(p.b)
    }

    @Test
    fun `invalid primitive falls back to default`() {
        val xml = "<root><i>not_a_number</i><b>maybe</b></root>"
        val p = decodeXml<Primitives>(xml)
        assertEquals(0, p.i)
        assertEquals(false, p.b)
    }

    // ── parseData (SOAP envelope extraction) ───────────────────────

    @Test
    fun `parseData extracts first child of Body`() {
        val soap = """
            <?xml version="1.0"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                <s:Body>
                    <u:GetTransportInfoResponse xmlns:u="urn:upnp">
                        <CurrentTransportState>STOPPED</CurrentTransportState>
                        <CurrentSpeed>1</CurrentSpeed>
                    </u:GetTransportInfoResponse>
                </s:Body>
            </s:Envelope>
        """.trimIndent()
        val r = parseData<TransportInfo>(soap)
        assertEquals("STOPPED", r.state)
        assertEquals(1, r.speed)
    }

    @Test
    fun `parseData throws when Body is missing`() {
        val xml = "<root><something>else</something></root>"
        assertFailsWith<XmlDecodeException> { parseData<TransportInfo>(xml) }
    }

    @Test
    fun `parseData throws when Body has no child`() {
        val xml = "<root><s:Body></s:Body></root>"
        assertFailsWith<XmlDecodeException> { parseData<TransportInfo>(xml) }
    }

    // ── Namespace prefix handling ──────────────────────────────────

    @Test
    fun `namespace prefixes on children are stripped`() {
        val xml = """
            <root>
                <ns:name>Charlie</ns:name>
                <ns:age>25</ns:age>
            </root>
        """.trimIndent()
        val p = decodeXml<Person>(xml)
        assertEquals("Charlie", p.name)
        assertEquals(25, p.age)
    }

    // ── CDATA sections ─────────────────────────────────────────────

    @Serializable
    data class Article(val title: String = "", val content: String = "")

    @Test
    fun `decodes CDATA content`() {
        val xml = """
            <root>
                <title>Hello</title>
                <content><![CDATA[<p>HTML & stuff</p>]]></content>
            </root>
        """.trimIndent()
        val a = decodeXml<Article>(xml)
        assertEquals("Hello", a.title)
        assertEquals("<p>HTML & stuff</p>", a.content)
    }

    // ── XML entities ───────────────────────────────────────────────

    @Test
    fun `decodes XML entities in text`() {
        val xml = "<root><name>Tom &amp; Jerry &lt;3</name></root>"
        val p = decodeXml<Person>(xml)
        assertEquals("Tom & Jerry <3", p.name)
    }

    // ── Nullable fields ────────────────────────────────────────────

    @Serializable
    data class Optional(val required: String = "", val optional: String? = null)

    @Test
    fun `missing nullable field defaults to null`() {
        val xml = "<root><required>yes</required></root>"
        val o = decodeXml<Optional>(xml)
        assertEquals("yes", o.required)
        assertNull(o.optional)
    }

    @Test
    fun `present nullable field gets value`() {
        val xml = "<root><required>yes</required><optional>present</optional></root>"
        val o = decodeXml<Optional>(xml)
        assertEquals("present", o.optional)
    }

    // ── Unknown keys ignored ───────────────────────────────────────

    @Test
    fun `unknown XML elements are ignored`() {
        val xml = "<root><name>Dan</name><age>40</age><unknown>data</unknown><extra>123</extra></root>"
        val p = decodeXml<Person>(xml)
        assertEquals("Dan", p.name)
        assertEquals(40, p.age)
    }

    // ── Self-closing tags ──────────────────────────────────────────

    @Serializable
    data class WithBool(val enabled: Boolean = false, val flag: String = "")

    @Test
    fun `handles self-closing tags`() {
        val xml = "<root><enabled>true</enabled><flag/></root>"
        val w = decodeXml<WithBool>(xml)
        assertTrue(w.enabled)
        assertEquals("", w.flag)
    }

    // ── Error handling ─────────────────────────────────────────────

    @Test
    fun `empty xml throws exception`() {
        assertFailsWith<XmlDecodeException> { decodeXml<Person>("") }
    }

    @Test
    fun `malformed xml throws exception`() {
        assertFailsWith<XmlDecodeException> { decodeXml<Person>("<root><name>unclosed") }
    }

    // ── Attributes are ignored ─────────────────────────────────────

    @Test
    fun `attributes on elements are ignored`() {
        val xml = """<root attr="val"><name>Eve</name><age>20</age></root>"""
        val p = decodeXml<Person>(xml)
        assertEquals("Eve", p.name)
        assertEquals(20, p.age)
    }
}
