package com.ismartcoding.plain.api

import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.HEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.events.WebSocketData
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EventDirectionContractTest {
    @Test
    fun wirePacketsAndCapabilitiesUseNamesWithoutNumericAliases() {
        val packet = JsonHelper.jsonEncodeToElement(HostEventPacket(EventType.SCREEN_MIRROR_VIDEO_CODEC.name, "{}"))
        val kind = packet.jsonObject.getValue("type").jsonPrimitive
        assertTrue(kind.isString)
        assertEquals("SCREEN_MIRROR_VIDEO_CODEC", kind.content)
        val greeting = JsonHelper.jsonDecode<ContentEventPacket>("""{"type":"CONTENT_CHANGED","payload":"{}","hostCapabilities":{"textTypes":["SCREEN_MIRROR_VIDEO_CODEC"],"binaryTypes":["SCREEN_MIRROR_VIDEO"]}}""")
        assertEquals(EventType.CONTENT_CHANGED.name, greeting.type)
        val capabilities = requireNotNull(greeting.hostCapabilities)
        assertTrue(capabilities.accepts(EventType.SCREEN_MIRROR_VIDEO_CODEC, WebSocketData.Text("{}")))
        assertTrue(capabilities.accepts(EventType.SCREEN_MIRROR_VIDEO, WebSocketData.Binary(byteArrayOf(0, 1))))
        assertFalse(capabilities.accepts(EventType.SCREEN_MIRROR_VIDEO, WebSocketData.Text("{}")))
        assertFalse(capabilities.accepts(EventType.PAIRING_REQUEST_RECEIVED, WebSocketData.Text("{}")))
        assertTrue(EventType.entries.all { it.name.matches(Regex("[A-Z_]+")) })
    }

    @Test
    fun httpEventsHaveAnExplicitLocalOriginAndProjectionsCannotPublish() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "shared/src/commonMain").isDirectory }
        val sources = File(root, "shared/src/commonMain/kotlin/com/ismartcoding/plain")
        val httpEvents = File(sources, "events/HttpApiEvents.kt").readText()
        val declarations = Regex("(?:data )?class (\\w+)[^\\n]*: (\\w+)\\(\\)")
            .findAll(httpEvents).toList()
        assertTrue(declarations.isNotEmpty())
        declarations.forEach { declaration ->
            assertTrue(declaration.value, declaration.groupValues[1].startsWith("H"))
            assertTrue(declaration.value, declaration.groupValues[2] == HEvent::class.java.simpleName)
        }
        assertTrue(!HEvent::class.java.isAssignableFrom(WebSocketEvent::class.java))
        for (path in listOf("api/HttpEventProjection.kt", "discover/PairingProjection.kt", "discover/PairingCore.kt",
            "discover/RustNearbyDevices.kt", "discover/RustMdnsRuntime.kt",
            "chat/channel/ChannelSystemMessageReceiver.kt", "ai/RustImageModels.kt",
            "features/dlna/DlnaRendererState.kt", "features/PomodoroHost.kt")) {
            val source = File(sources, path).readText()
            assertTrue("$path must only project HTTP state locally", !source.contains("WebSocketEvent"))
        }
    }
}
