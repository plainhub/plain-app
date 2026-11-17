package com.ismartcoding.plain.features.dlna

import com.ismartcoding.plain.lib.JsonHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.*

class DlnaPlayerReportsTest {
    private inline fun <reified T> T.payload() = Json.parseToJsonElement(JsonHelper.jsonEncode(this)).jsonObject

    @Test
    fun playerFactsPreserveMediaIdentityStateNamesAndExactMilliseconds() {
        val uri = "https://example.test/owned?name=\"αβ\""
        val position = Position(uri, Long.MAX_VALUE, Long.MAX_VALUE - 1)
        assertEquals(uri, position.payload().getValue("mediaUri").jsonPrimitive.content)
        assertEquals(Long.MAX_VALUE, position.payload().getValue("positionMs").jsonPrimitive.long)
        assertEquals(Long.MAX_VALUE - 1, position.payload().getValue("durationMs").jsonPrimitive.long)
        val playback = Playback(uri, DlnaPlaybackState.PAUSED)
        assertEquals("PAUSED_PLAYBACK", playback.payload().getValue("state").jsonPrimitive.content)
        val seek = Seek(uri, 5_000_000_001)
        assertEquals(5_000_000_001, seek.payload().getValue("positionMs").jsonPrimitive.long)
        val stopped = Stopped(uri)
        assertEquals(setOf("mediaUri"), stopped.payload().keys)
        assertEquals(uri, stopped.payload().getValue("mediaUri").jsonPrimitive.content)
    }
}
