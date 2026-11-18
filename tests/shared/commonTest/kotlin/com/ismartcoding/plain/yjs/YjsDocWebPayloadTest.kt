package com.ismartcoding.plain.yjs

import com.ismartcoding.plain.lib.yjs.ArrowLayer
import com.ismartcoding.plain.lib.yjs.EditorFontStyle
import com.ismartcoding.plain.lib.yjs.EditorFontWeight
import com.ismartcoding.plain.lib.yjs.EditorTextAlign
import com.ismartcoding.plain.lib.yjs.EditorTextDecoration
import com.ismartcoding.plain.lib.yjs.FreehandLayer
import com.ismartcoding.plain.lib.yjs.StickerLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.lib.yjs.YjsDoc
import com.ismartcoding.plain.lib.yjs.YjsDocEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class YjsDocWebPayloadTest {

    // Editor document generated with Yjs encodeStateAsUpdate (binary Update V1).
    private val webPayloadB64 =
        "AXwqACgBBG1ldGENZm9ybWF0VmVyc2lvbgF9ASgBBG1ldGELY2FudmFzV2lkdGgBfYAUKAEEbWV0YQxjYW52YXNIZWlnaHQBfZALKAEEbWV0YQdiZ0Nv" +
        "bG9yAXcJI2ZmZmZmZmZmKAEEbWV0YQppbWdPZmZzZXRYAX0AKAEEbWV0YQppbWdPZmZzZXRZAX0AKAEEbWV0YQhpbWdBbHBoYQF9pAEHAQZsYXllcnMB" +
        "KAAqBwJpZAF3B0wtYXJyb3coACoHBHR5cGUBdwVBUlJPVygAKgcHdmlzaWJsZQF4KAAqBwRuYW1lAXcFQVJST1coACoHAngxAX2QASgAKgcCeTEBfZAB" +
        "KAAqBwJ4MgF9kAYoACoHAnkyAX2YAygAKgcFY29sb3IBdwcjZmYwMDAwKAAqBwlsaW5lV2lkdGgBfQYoACoHCHJvdGF0aW9uAX0AhyoHASgAKhMCaWQB" +
        "dwZMLXJlY3QoACoTBHR5cGUBdwRSRUNUKAAqEwd2aXNpYmxlAXgoACoTBG5hbWUBdwRSRUNUKAAqEwF4AX20BygAKhMBeQF9pAEoACoTAXcBfZgEKAAq" +
        "EwFoAX2gAigAKhMFY29sb3IBdwcjMDA2NmZmKAAqEwlsaW5lV2lkdGgBfQSHKhMBKAAqHgJpZAF3CUwtZWxsaXBzZSgAKh4EdHlwZQF3B0VMTElQU0Uo" +
        "ACoeB3Zpc2libGUBeCgAKh4EbmFtZQF3B0VMTElQU0UoACoeAmN4AX2oDygAKh4CY3kBfbQCKAAqHgJyeAF9uAEoACoeAnJ5AX2QASgAKh4FY29sb3IB" +
        "dwcjMDBjYzAwKAAqHglsaW5lV2lkdGgBfQSHKh4BKAAqKQJpZAF3C0wtaGlnaGxpZ2h0KAAqKQR0eXBlAXcJSElHSExJR0hUKAAqKQd2aXNpYmxlAXgo" +
        "ACopBG5hbWUBdwlISUdITElHSFQoACopAXgBfZABKAAqKQF5AX2EBCgAKikBdwF9gAUoACopAWgBfTwoACopBWNvbG9yAXcWcmdiYSgyNTUsIDI1NSwg" +
        "MCwgMC40KYcqKQEoACozAmlkAXcITC1tb3NhaWMoACozBHR5cGUBdwZNT1NBSUMoACozB3Zpc2libGUBeCgAKjMEbmFtZQF3Bk1PU0FJQygAKjMBeAF9" +
        "tAcoACozAXkBfYQEKAAqMwF3AX2IAygAKjMBaAF9uAEoACozCWJsb2NrU2l6ZQF9DIcqMwEoACo9AmlkAXcGTC10ZXh0KAAqPQR0eXBlAXcEVEVYVCgA" +
        "Kj0HdmlzaWJsZQF4KAAqPQRuYW1lAXcEVEVYVCgAKj0BeAF9kAEoACo9AXkBfYgIKAAqPQR0ZXh0AXcWV2ViIGVkaXRvciBwYXJpdHkgdGVzdCgAKj0I" +
        "Zm9udFNpemUBfTgoACo9BWNvbG9yAXcHIzAwMDAwMCgAKj0KZm9udEZhbWlseQF3CnNhbnMtc2VyaWYoACo9CmZvbnRXZWlnaHQBdwRCT0xEKAAqPQlm" +
        "b250U3R5bGUBdwZJVEFMSUMoACo9CXRleHRBbGlnbgF3BkNFTlRFUigAKj0NbGV0dGVyU3BhY2luZwF9ACgAKj0OdGV4dERlY29yYXRpb24BdwlVTkRF" +
        "UkxJTkUoACo9BGdsb3cBeSgAKj0Icm90YXRpb24BfQAoACo9CG1heFdpZHRoAX2YCScAKj0KdGV4dFN0cm9rZQEoACpQBXdpZHRoAX0CKAAqUAVjb2xv" +
        "cgF3ByNmZjAwMDAnACo9CnRleHRTaGFkb3cBKAAqUwdvZmZzZXRYAX0DKAAqUwdvZmZzZXRZAX0DKAAqUwRibHVyAX0EKAAqUwVjb2xvcgF3D3JnYmEo" +
        "MCwwLDAsMC43KYcqPQEoACpYAmlkAXcKTC1mcmVlaGFuZCgAKlgEdHlwZQF3CEZSRUVIQU5EKAAqWAd2aXNpYmxlAXgoACpYBG5hbWUBdwhGUkVFSEFO" +
        "RCgAKlgFY29sb3IBdwcjZmYwMGZmKAAqWAlsaW5lV2lkdGgBfQgnACpYBnBvaW50cwAIACpfBnUCfaAMfZAGdQJ9tAx9pAZ1An2IDX2uBnUCfZwNfbgG" +
        "dQJ9sA19swZ1An2EDn2kBocqWAEoACpmAmlkAXcJTC1zdGlja2VyKAAqZgR0eXBlAXcHU1RJQ0tFUigAKmYHdmlzaWJsZQF4KAAqZgRuYW1lAXcHU1RJ" +
        "Q0tFUigAKmYBeAF9qA8oACpmAXkBfZAGKAAqZgF3AX2IAygAKmYBaAF9uAEoACpmBWNvbG9yAXcHI2ZmZWIzYigAKmYEdGV4dAF3BE5PVEUoACpmCGZv" +
        "bnRTaXplAX0kKAAqZgpmb250V2VpZ2h0AXcIU0VNSUJPTEQoACpmCWZvbnRTdHlsZQF3Bk5PUk1BTCgAKmYIcm90YXRpb24BfQCHKmYBKAAqdQJpZAF3" +
        "B0wtaW1hZ2UoACp1BHR5cGUBdwVJTUFHRSgAKnUHdmlzaWJsZQF4KAAqdQRuYW1lAXcFSU1BR0UoACp1AXgBfQAoACp1AXkBfQAoACp1AXcBfSgoACp1" +
        "AWgBfSgoACp1B29wYWNpdHkBfaQBKAAqdQhyb3RhdGlvbgF9ACgBBmltYWdlcwdMLWltYWdlAXcaZGF0YTppbWFnZS9wbmc7YmFzZTY0LEFBPT0A"

    @OptIn(ExperimentalEncodingApi::class)
    private val bytes = Base64.Default.decode(webPayloadB64)
    private val doc = YjsDoc(bytes)

    @Test
    fun parses_canvas_size_from_web_payload() {
        val size = doc.getCanvasSize()
        assertEquals(1280, size.width)
        assertEquals(720, size.height)
    }

    @Test
    fun parses_background_color_from_web_payload() {
        assertEquals("#ffffffff", doc.getBgColor())
    }

    @Test
    fun parses_image_offset_and_alpha_from_web_payload() {
        assertEquals(0.0, doc.getImgOffset().x)
        assertEquals(0.0, doc.getImgOffset().y)
        assertEquals(100.0, doc.getImgAlpha())
    }

    @Test
    fun extracts_all_layer_types_from_web_payload() {
        val layers = doc.getLayers()
        assertEquals(9, layers.size)
        val types = layers.map { it.type.name }
        assertEquals(
            listOf("ARROW", "RECT", "ELLIPSE", "HIGHLIGHT", "MOSAIC", "TEXT", "FREEHAND", "STICKER", "IMAGE"),
            types,
        )
    }

    @Test
    fun parses_arrow_layer_properties() {
        val arrow = doc.getLayers().first { it is ArrowLayer } as ArrowLayer
        assertEquals("L-arrow", arrow.id)
        assertEquals("#ff0000", arrow.color)
        assertEquals(6.0, arrow.lineWidth)
        assertTrue(arrow.visible)
    }

    @Test
    fun parses_text_layer_with_textStroke_and_textShadow() {
        val text = doc.getLayers().first { it is TextLayer } as TextLayer
        assertEquals("L-text", text.id)
        assertEquals("Web editor parity test", text.text)
        assertEquals(56.0, text.fontSize)
        assertEquals("#000000", text.color)
        assertEquals(EditorFontWeight.BOLD, text.fontWeight)
        assertEquals(EditorFontStyle.ITALIC, text.fontStyle)
        assertEquals(EditorTextAlign.CENTER, text.textAlign)
        assertEquals(EditorTextDecoration.UNDERLINE, text.textDecoration)
        assertEquals(2.0, text.textStroke.width)
        assertEquals("#ff0000", text.textStroke.color)
        assertEquals(3.0, text.textShadow.offsetX)
        assertEquals(3.0, text.textShadow.offsetY)
        assertEquals(4.0, text.textShadow.blur)
        assertEquals("rgba(0,0,0,0.7)", text.textShadow.color)
    }

    @Test
    fun parses_freehand_layer_with_6_points() {
        val freehand = doc.getLayers().first { it is FreehandLayer } as FreehandLayer
        assertEquals("L-freehand", freehand.id)
        assertEquals(6, freehand.points.size)
        assertEquals(800.0, freehand.points[0].x)
        assertEquals(400.0, freehand.points[0].y)
    }

    @Test
    fun parses_sticker_layer() {
        val sticker = doc.getLayers().first { it is StickerLayer } as StickerLayer
        assertEquals("L-sticker", sticker.id)
        assertEquals("NOTE", sticker.text)
        assertEquals(36.0, sticker.fontSize)
        assertEquals("#ffeb3b", sticker.color)
        assertEquals(EditorFontWeight.SEMIBOLD, sticker.fontWeight)
        assertEquals(EditorFontStyle.NORMAL, sticker.fontStyle)
    }

    @Test
    fun roundtrip_encode_and_reparse_preserves_document_enum_values() {
        val layers = doc.getLayers()
        val encoded = YjsDocEncoder.encode(
            canvasSize = doc.getCanvasSize(),
            bgColor = doc.getBgColor(),
            sourceImage = doc.getSourceImage(),
            imgOffset = doc.getImgOffset(),
            imgAlpha = doc.getImgAlpha(),
            layers = layers,
            images = doc.getImages(),
        )
        val reparsed = YjsDoc(encoded)
        assertEquals(1, (reparsed.getMetaValue("formatVersion") as Number).toInt())
        assertEquals(layers, reparsed.getLayers())
        assertEquals(doc.getImages(), reparsed.getImages())
        val wireText = encoded.decodeToString()
        for ((field, value) in mapOf(
            "type" to "STICKER",
            "fontWeight" to "SEMIBOLD",
            "fontStyle" to "ITALIC",
            "textAlign" to "CENTER",
            "textDecoration" to "UNDERLINE",
        )) {
            assertTrue(wireField(field, value) in wireText, "Missing persisted enum $field=$value")
        }
    }

    @Test
    fun rejects_lowercase_layer_and_style_wire_values() {
        for ((field, value) in mapOf("type" to "STICKER", "fontWeight" to "SEMIBOLD")) {
            val needle = wireField(field, value).encodeToByteArray()
            val start = (0..bytes.size - needle.size).first { offset ->
                needle.indices.all { bytes[offset + it] == needle[it] }
            }
            val malformed = bytes.copyOf()
            wireField(field, value.lowercase()).encodeToByteArray().copyInto(malformed, start)
            assertFailsWith<IllegalArgumentException> { YjsDoc(malformed).getLayers() }
        }
    }

    private fun wireField(field: String, value: String): String =
        "$field\u0001w${value.length.toChar()}$value"
}
