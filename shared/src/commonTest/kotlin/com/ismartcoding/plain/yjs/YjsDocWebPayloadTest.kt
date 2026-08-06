package com.ismartcoding.plain.yjs

import com.ismartcoding.plain.lib.yjs.ArrowLayer
import com.ismartcoding.plain.lib.yjs.FreehandLayer
import com.ismartcoding.plain.lib.yjs.StickerLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.lib.yjs.YjsDoc
import com.ismartcoding.plain.lib.yjs.YjsDocEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YjsDocWebPayloadTest {

    // Yjs V1 binary update produced by the web image editor (schema.ts).
    // Encodes 8 layers: arrow, rect, ellipse, highlight, mosaic, text,
    // freehand (6 points), sticker. Canvas 1280x720, white bg, imgAlpha=100.
    private val webPayloadB64 =
        "AW+lmLT7BgAoAQRtZXRhC2NhbnZhc1dpZHRoAX2AFCgBBG1ldGEMY2FudmFzSGVpZ2h0AX2QCygBBG1ldGEHYmdDb2xvcgF3CSNmZmZmZmZmZigBBG1ldGEK" +
        "aW1nT2Zmc2V0WAF9ACgBBG1ldGEKaW1nT2Zmc2V0WQF9ACgBBG1ldGEIaW1nQWxwaGEBfaQBBwEGbGF5ZXJzASgApZi0+wYGAmlkAXcHTC1hcnJvdygApZi0" +
        "+wYGBHR5cGUBdwVhcnJvdygApZi0+wYGB3Zpc2libGUBeCgApZi0+wYGBG5hbWUBdwVBcnJvdygApZi0+wYGAngxAX2QASgApZi0+wYGAnkxAX2QASgApZi0" +
        "+wYGAngyAX2QBigApZi0+wYGAnkyAX2wAygApZi0+wYGBWNvbG9yAXcHI2ZmMDAwMCgApZi0+wYGCWxpbmVXaWR0aAF9BigApZi0+wYGCHJvdGF0aW9uAX0A" +
        "h6WYtPsGBgEoAKWYtPsGEgJpZAF3BkwtcmVjdCgApZi0+wYSBHR5cGUBdwRyZWN0KAClmLT7BhIHdmlzaWJsZQF4KAClmLT7BhIEbmFtZQF3BFJlY3QoAKWY" +
        "tPsGEgF4AX20BygApZi0+wYSAXkBfZABKAClmLT7BhIBdwF9mAQoAKWYtPsGEgFoAX2gAigApZi0+wYSBWNvbG9yAXcHIzAwNjZmZigApZi0+wYSCWxpbmVX" +
        "aWR0aAF9BIelmLT7BhIBKAClmLT7Bh0CaWQBdwlMLWVsbGlwc2UoAKWYtPsGHQR0eXBlAXcHZWxsaXBzZSgApZi0+wYdB3Zpc2libGUBeCgApZi0+wYdBG5h" +
        "bWUBdwdFbGxpcHNlKAClmLT7Bh0CY3gBfagPKAClmLT7Bh0CY3kBfbQCKAClmLT7Bh0CcngBfbgBKAClmLT7Bh0CcnkBfZABKAClmLT7Bh0FY29sb3IBdwcj" +
        "MDBjYzAwKAClmLT7Bh0JbGluZVdpZHRoAX0Eh6WYtPsGHQEoAKWYtPsGKAJpZAF3C0wtaGlnaGxpZ2h0KAClmLT7BigEdHlwZQF3CWhpZ2hsaWdodCgApZi0" +
        "+wYoB3Zpc2libGUBeCgApZi0+wYoBG5hbWUBdwlIaWdobGlnaHQoAKWYtPsGKAF4AX2QASgApZi0+wYoAXkBfagFKAClmLT7BigBdwF9gAUoAKWYtPsGKAFo" +
        "AX08KAClmLT7BigFY29sb3IBdxZyZ2JhKDI1NSwgMjU1LCAwLCAwLjQph6WYtPsGKAEoAKWYtPsGMgJpZAF3CEwtbW9zYWljKAClmLT7BjIEdHlwZQF3Bm1v" +
        "c2FpYygApZi0+wYyB3Zpc2libGUBeCgApZi0+wYyBG5hbWUBdwZNb3NhaWMoAKWYtPsGMgF4AX20BygApZi0+wYyAXkBfagFKAClmLT7BjIBdwF9iAMoAKWY" +
        "tPsGMgFoAX24ASgApZi0+wYyCWJsb2NrU2l6ZQF9DIelmLT7BjIBKAClmLT7BjwCaWQBdwZMLXRleHQoAKWYtPsGPAR0eXBlAXcEdGV4dCgApZi0+wY8B3Zp" +
        "c2libGUBeCgApZi0+wY8BG5hbWUBdwRUZXh0KAClmLT7BjwBeAF9kAEoAKWYtPsGPAF5AX2ICCgApZi0+wY8BHRleHQBdxZXZWIgZWRpdG9yIHBhcml0eSB0" +
        "ZXN0KAClmLT7BjwIZm9udFNpemUBfTgoAKWYtPsGPAVjb2xvcgF3ByMwMDAwMDAoAKWYtPsGPApmb250RmFtaWx5AXcKc2Fucy1zZXJpZigApZi0+wY8CmZv" +
        "bnRXZWlnaHQBdwRib2xkKAClmLT7BjwJZm9udFN0eWxlAXcGbm9ybWFsKAClmLT7BjwJdGV4dEFsaWduAXcEbGVmdCgApZi0+wY8DWxldHRlclNwYWNpbmcB" +
        "fQAoAKWYtPsGPA50ZXh0RGVjb3JhdGlvbgF3BG5vbmUnAKWYtPsGPAp0ZXh0U3Ryb2tlASgApZi0+wZMBXdpZHRoAX0CKAClmLT7BkwFY29sb3IBdwcjZmYw" +
        "MDAwJwClmLT7BjwKdGV4dFNoYWRvdwEoAKWYtPsGTwdvZmZzZXRYAX0DKAClmLT7Bk8Hb2Zmc2V0WQF9AygApZi0+wZPBGJsdXIBfQQoAKWYtPsGTwVjb2xv" +
        "cgF3D3JnYmEoMCwwLDAsMC43KSgApZi0+wY8BGdsb3cBeSgApZi0+wY8CHJvdGF0aW9uAX0AKAClmLT7BjwIbWF4V2lkdGgBfZgJh6WYtPsGPAEoAKWYtPsG" +
        "VwJpZAF3CkwtZnJlZWhhbmQoAKWYtPsGVwR0eXBlAXcIZnJlZWhhbmQoAKWYtPsGVwd2aXNpYmxlAXgoAKWYtPsGVwRuYW1lAXcFQnJ1c2gnAKWYtPsGVwZw" +
        "b2ludHMACAClmLT7BlwGdQJ9oAx9kAZ1An20DH2kBnUCfZINfbgGdQJ9ug19ggd1An2iDn24BnUCfYAPfZoGKAClmLT7BlcFY29sb3IBdwcjZmYwMGZmKACl" +
        "mLT7BlcJbGluZVdpZHRoAX0Ih6WYtPsGVwEoAKWYtPsGZQJpZAF3CUwtc3RpY2tlcigApZi0+wZlBHR5cGUBdwdzdGlja2VyKAClmLT7BmUHdmlzaWJsZQF4" +
        "KAClmLT7BmUEbmFtZQF3B1N0aWNrZXIoAKWYtPsGZQF4AX2oDygApZi0+wZlAXkBfaAHKAClmLT7BmUBdwF9iAMoAKWYtPsGZQFoAX24ASgApZi0+wZlBWNv" +
        "bG9yAXcHI2ZmZWIzYigApZi0+wZlBHRleHQBdwROT1RFKAClmLT7BmUIZm9udFNpemUBfSQoAKWYtPsGZQpmb250V2VpZ2h0AXcEYm9sZCgApZi0+wZlCWZv" +
        "bnRTdHlsZQF3Bm5vcm1hbCgApZi0+wZlCHJvdGF0aW9uAX0AAA=="

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
    fun extracts_all_8_layers_from_web_payload() {
        val layers = doc.getLayers()
        assertEquals(8, layers.size)
        val types = layers.map { it.type }
        assertEquals(
            listOf("arrow", "rect", "ellipse", "highlight", "mosaic", "text", "freehand", "sticker"),
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
    }

    @Test
    fun roundtrip_encode_and_reparse_preserves_layer_count() {
        val layers = doc.getLayers()
        val encoded = YjsDocEncoder.encode(
            canvasSize = doc.getCanvasSize(),
            bgColor = doc.getBgColor(),
            sourceImage = doc.getSourceImage(),
            imgOffset = doc.getImgOffset(),
            imgAlpha = doc.getImgAlpha(),
            layers = layers,
        )
        val reparsed = YjsDoc(encoded)
        assertEquals(8, reparsed.getLayers().size)
    }
}
