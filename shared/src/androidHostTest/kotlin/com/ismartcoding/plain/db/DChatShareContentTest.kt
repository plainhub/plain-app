package com.ismartcoding.plain.db

import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DChatShareContentTest {
    private val parser = Json { ignoreUnknownKeys = true }

    private fun sample() = DMessageShare(
        shareId = "sid123",
        urlToken = "TOKEN",
        peerInfo = DSharePeerInfo(id = "peer1", ip = "192.168.1.2", port = 8443),
        name = "项目资料",
        itemCount = 12,
        totalSize = 248600000,
        expiresAt = Instant.parse("2026-09-22T18:00:00Z"),
    )

    @Test
    fun shareContentRoundTrip() {
        val parsed = DChat.parseContent(DMessageContent(MessageType.SHARE, sample()).toJSONString())

        assertEquals(MessageType.SHARE, parsed.type)
        val share = parsed.value as DMessageShare
        assertEquals("sid123", share.shareId)
        assertEquals("TOKEN", share.urlToken)
        assertEquals("peer1", share.peerInfo.id)
        assertEquals("192.168.1.2", share.peerInfo.ip)
        assertEquals(8443, share.peerInfo.port)
        assertEquals("项目资料", share.name)
        assertEquals(12, share.itemCount)
        assertEquals(248600000, share.totalSize)
        assertEquals(Instant.parse("2026-09-22T18:00:00Z"), share.expiresAt)
    }

    @Test
    fun shareContentWithoutExpiryRoundTrip() {
        val content = DMessageContent(
            MessageType.SHARE,
            DMessageShare(
                shareId = "sid",
                urlToken = "t",
                peerInfo = DSharePeerInfo(id = "p", ip = "10.0.0.2", port = 8443),
                name = "photos",
            ),
        )
        val parsed = DChat.parseContent(content.toJSONString())

        assertEquals(MessageType.SHARE, parsed.type)
        val share = parsed.value as DMessageShare
        assertNull(share.expiresAt)
        assertEquals(0, share.itemCount)
    }

    @Test
    fun legacySharePayloadFallsBackToRawText() {
        // Pre-peerInfo cards (url field, no urlToken) must render as raw text,
        // not crash with SHARE type + text value.
        val legacy = """{"type":"SHARE","value":{"shareId":"s","url":"https://h:8443/s/s#t","name":"n"}}"""
        val parsed = DChat.parseContent(legacy)

        assertEquals(MessageType.TEXT, parsed.type)
        assertEquals(legacy, (parsed.value as DMessageText).text)
    }

    @Test
    fun nonJsonContentFallsBackToRawText() {
        val raw = "plain legacy content"
        val parsed = DChat.parseContent(raw)

        assertEquals(MessageType.TEXT, parsed.type)
        assertEquals(raw, (parsed.value as DMessageText).text)
    }

    @Test
    fun wirePayloadIsTypedJsonObject() {
        // The wire JSON must keep the {"type","value"} envelope so receivers
        // can route on the type name.
        val wire = DMessageContent(MessageType.SHARE, sample()).toJSONString()
        val obj = parser.parseToJsonElement(wire).jsonObject
        assertEquals("SHARE", obj["type"]!!.jsonPrimitive.content)
        val value = obj["value"]!!.jsonObject
        assertEquals("sid123", value["shareId"]!!.jsonPrimitive.content)
        assertEquals("TOKEN", value["urlToken"]!!.jsonPrimitive.content)
        assertEquals("192.168.1.2", value["peerInfo"]!!.jsonObject["ip"]!!.jsonPrimitive.content)
    }
}
