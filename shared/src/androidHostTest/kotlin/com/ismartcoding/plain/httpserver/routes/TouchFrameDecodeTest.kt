package com.ismartcoding.plain.httpserver.routes

import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Byte-level lock for the binary touch frame decoded from the WS upstream
 * control channel (API_SPEC §12.4). The layout must stay byte-identical with
 * plain-cast and with the plain-desktop encoder (touch-frame.ts golden frame
 * asserts the same byte sequence) — any change here breaks both clients.
 */
class TouchFrameDecodeTest {
    private fun sample(action: Int, pointerId: Int, x: Int, y: Int, dtMs: Int = 0): ByteArray =
        byteArrayOf(
            action.toByte(),
            pointerId.toByte(),
            (x and 0xff).toByte(),
            ((x shr 8) and 0xff).toByte(),
            (y and 0xff).toByte(),
            ((y shr 8) and 0xff).toByte(),
            (dtMs and 0xff).toByte(),
            ((dtMs shr 8) and 0xff).toByte(),
        )

    private fun frame(vararg samples: ByteArray, streamId: Int = 0): ByteArray {
        val out = ByteArray(4 + samples.size * 8)
        out[0] = 0x54
        out[1] = samples.size.toByte()
        out[2] = (streamId and 0xff).toByte()
        out[3] = ((streamId shr 8) and 0xff).toByte()
        samples.forEachIndexed { i, s -> s.copyInto(out, 4 + i * 8) }
        return out
    }

    @Test
    fun downSampleRoundTrips() {
        val inputs = decodeTouchFrame(frame(sample(0, 7, 32768, 16384)))
        assertEquals(1, inputs.size)
        val input = inputs[0]
        assertEquals(ScreenMirrorControlAction.TOUCH_DOWN, input.action)
        assertEquals(7, input.pointerId)
        assertEquals(32768 / 65535f, input.x)
        assertEquals(16384 / 65535f, input.y)
    }

    @Test
    fun coordinatesAreLittleEndian() {
        val inputs = decodeTouchFrame(frame(sample(1, 0, 0x1234, 0x5678)))
        assertEquals(0x1234 / 65535f, inputs[0].x)
        assertEquals(0x5678 / 65535f, inputs[0].y)
    }

    @Test
    fun actionMappingDownMoveUpCancel() {
        val inputs = decodeTouchFrame(
            frame(
                sample(0, 1, 0, 0),
                sample(1, 1, 100, 100),
                sample(2, 1, 200, 200),
                sample(3, 1, 300, 300),
            ),
        )
        assertEquals(
            listOf(
                ScreenMirrorControlAction.TOUCH_DOWN,
                ScreenMirrorControlAction.TOUCH_MOVE,
                ScreenMirrorControlAction.TOUCH_UP,
                // CANCEL maps to UP: the accessibility injector cannot cancel
                // mid-stream, a released touch is the same terminal state
                ScreenMirrorControlAction.TOUCH_UP,
            ),
            inputs.map { it.action },
        )
    }

    @Test
    fun multiSampleFramePreservesOrder() {
        val inputs = decodeTouchFrame(
            frame(sample(0, 3, 0, 0), sample(1, 3, 65535, 65535), sample(2, 3, 100, 200)),
        )
        assertEquals(3, inputs.size)
        assertEquals(ScreenMirrorControlAction.TOUCH_DOWN, inputs[0].action)
        assertEquals(3, inputs[0].pointerId)
        assertEquals(1f, inputs[1].x)
        assertEquals(1f, inputs[1].y)
        assertEquals(100 / 65535f, inputs[2].x)
        assertEquals(200 / 65535f, inputs[2].y)
    }

    @Test
    fun streamIdHeaderIsIgnored() {
        // reserved for the parallel-screen protocol; the mirror channel always
        // sees 0 but must not choke on other values
        val inputs = decodeTouchFrame(frame(sample(0, 1, 0, 0), streamId = 5))
        assertEquals(1, inputs.size)
    }

    @Test
    fun malformedFramesAreDroppedNotThrown() {
        assertTrue(decodeTouchFrame(ByteArray(0)).isEmpty())
        assertTrue(decodeTouchFrame(byteArrayOf(0x54)).isEmpty())
        assertTrue(decodeTouchFrame(byteArrayOf(0x54, 0, 0, 0)).isEmpty()) // count=0
        assertTrue(decodeTouchFrame(byteArrayOf(0x53, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0)).isEmpty()) // wrong magic
        // declared count=2 but only one sample present
        val truncated = frame(sample(0, 1, 0, 0), sample(1, 1, 0, 0)).copyOfRange(0, 4 + 8)
        assertTrue(decodeTouchFrame(truncated).isEmpty())
    }

    @Test
    fun goldenFrameMatchesApiSpecSection12() {
        // The exact bytes a compliant client sends for
        // DOWN(0.5, 0.25, pointerId=1) + MOVE(0.75, 0.5, dt=16ms):
        // x=round(0.5*65535)=32768=0x8000, y=round(0.25*65535)=16384=0x4000,
        // x2=round(0.75*65535)=49151=0xBFFF, y2=32768=0x8000 — all little-endian.
        val bytes = byteArrayOf(
            0x54, 0x02, 0x00, 0x00,
            0x00, 0x01, 0x00, 0x80.toByte(), 0x00, 0x40, 0x00, 0x00,
            0x01, 0x01, 0xFF.toByte(), 0xBF.toByte(), 0x00, 0x80.toByte(), 0x10, 0x00,
        )
        val inputs = decodeTouchFrame(bytes)
        assertEquals(2, inputs.size)
        assertEquals(ScreenMirrorControlAction.TOUCH_DOWN, inputs[0].action)
        assertEquals(32768 / 65535f, inputs[0].x)
        assertEquals(16384 / 65535f, inputs[0].y)
        assertEquals(ScreenMirrorControlAction.TOUCH_MOVE, inputs[1].action)
        assertEquals(49151 / 65535f, inputs[1].x)
        assertEquals(32768 / 65535f, inputs[1].y)
    }
}
