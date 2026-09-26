package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Routing lock for the JSON upstream control envelope (API_SPEC §12.3):
 * only `{"type":"screenMirrorControl","input":{…}}` routes to the control
 * dispatcher. Everything else — unknown types, bare pre-envelope inputs,
 * malformed JSON — must be dropped (null), so future upstream event types
 * can never be silently swallowed by, or misrouted into, screen-mirror
 * control (ignoreUnknownKeys would otherwise let a `{"action":…}` field of
 * an unrelated message inject a touch).
 */
class UpstreamControlDecodeTest {
    @Test
    fun envelopeRoutesToControlInput() {
        val input = decodeUpstreamEnvelope(
            """{"type":"screenMirrorControl","input":{"action":"BACK"}}""".encodeToByteArray(),
        )
        assertEquals(ScreenMirrorControlAction.BACK, input?.action)
    }

    @Test
    fun envelopeCarriesFullInputShape() {
        val input = decodeUpstreamEnvelope(
            """
            {"type":"screenMirrorControl","input":{
              "action":"SCROLL","x":0.5,"y":0.25,"deltaX":-10,"deltaY":120}}
            """.trimIndent().encodeToByteArray(),
        )
        assertNotNull(input)
        assertEquals(ScreenMirrorControlAction.SCROLL, input.action)
        assertEquals(0.5f, input?.x)
        assertEquals(0.25f, input?.y)
        assertEquals(-10f, input?.deltaX)
        assertEquals(120f, input?.deltaY)
    }

    @Test
    fun unknownTypeIsDropped() {
        // a future upstream event type must not reach control dispatch —
        // even when its payload coincidentally contains an "action" field
        val input = decodeUpstreamEnvelope(
            """{"type":"deviceCommand","action":"BACK"}""".encodeToByteArray(),
        )
        assertNull(input)
    }

    @Test
    fun barePreEnvelopeInputIsRejected() {
        assertNull(decodeUpstreamEnvelope("""{"action":"BACK"}""".encodeToByteArray()))
    }

    @Test
    fun malformedJsonIsDropped() {
        assertNull(decodeUpstreamEnvelope("not json".encodeToByteArray()))
        assertNull(decodeUpstreamEnvelope("{}".encodeToByteArray()))
        assertNull(decodeUpstreamEnvelope("""[1,2,3]""".encodeToByteArray()))
    }

    @Test
    fun invalidActionEnumIsDropped() {
        val input = decodeUpstreamEnvelope(
            """{"type":"screenMirrorControl","input":{"action":"NOT_AN_ACTION"}}""".encodeToByteArray(),
        )
        assertNull(input)
    }
}
