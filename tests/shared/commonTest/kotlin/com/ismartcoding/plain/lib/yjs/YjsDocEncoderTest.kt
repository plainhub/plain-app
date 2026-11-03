package com.ismartcoding.plain.lib.yjs

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YjsDocEncoderTest {
    @Test
    fun signed_integers_match_lib0_wire_vectors() {
        val vectors = listOf(
            0L to listOf(0),
            1L to listOf(1),
            -1L to listOf(65),
            63L to listOf(63),
            -63L to listOf(127),
            64L to listOf(128, 1),
            -64L to listOf(192, 1),
            127L to listOf(191, 1),
            -127L to listOf(255, 1),
            128L to listOf(128, 2),
            -128L to listOf(192, 2),
            1080L to listOf(184, 16),
            -1080L to listOf(248, 16),
            1920L to listOf(128, 30),
            -1920L to listOf(192, 30),
            2147483647L to listOf(191, 255, 255, 255, 15),
            -2147483648L to listOf(192, 128, 128, 128, 16),
            9007199254740991L to listOf(191, 255, 255, 255, 255, 255, 255, 31),
            -9007199254740991L to listOf(255, 255, 255, 255, 255, 255, 255, 31),
        )
        for ((value, wire) in vectors) {
            val expected = wire.map { it.toByte() }.toByteArray()
            assertContentEquals(expected, YjsEncoder().apply { writeVarInt(value) }.toByteArray(), "value=$value")
            val decoder = YjsDecoder(expected)
            assertEquals(value, decoder.readVarInt())
            assertTrue(decoder.eof())
        }
    }

    @Test
    fun saved_editor_document_restores_its_canvas_and_rectangle() {
        val rectangle = RectLayer(id = "rectangle", x = 64.0, y = 128.0, w = 320.0, h = 180.0)
        val encoded = YjsDocEncoder.encode(
            canvasSize = CanvasSize(1920, 1080),
            bgColor = "transparent",
            sourceImage = null,
            imgOffset = Point(-64.0, 128.0),
            imgAlpha = 100.0,
            layers = listOf(rectangle),
        )
        val restored = YjsDoc(encoded)
        assertEquals(CanvasSize(1920, 1080), restored.getCanvasSize())
        assertEquals("transparent", restored.getBgColor())
        assertEquals(Point(-64.0, 128.0), restored.getImgOffset())
        assertEquals(100.0, restored.getImgAlpha())
        assertEquals(listOf(rectangle), restored.getLayers())
    }
}
