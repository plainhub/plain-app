package com.ismartcoding.plain.yjs

import com.ismartcoding.plain.lib.yjs.CanvasSize
import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.RectLayer
import com.ismartcoding.plain.lib.yjs.Point
import com.ismartcoding.plain.lib.yjs.YjsDoc
import com.ismartcoding.plain.lib.yjs.YjsDocEncoder
import com.ismartcoding.plain.lib.yjs.YjsDecoder
import com.ismartcoding.plain.helpers.Base64Lenient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImageEditorDocumentFormatTest {
    @Test
    fun writesCurrentFormatAndRestoresEmbeddedImageResources() {
        val png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg=="
        val layer = ImageLayer(id = "image", x = 50.0, y = 40.0, w = 64.0, h = 48.0, opacity = 0.5, rotation = 20.0)
        val bytes = YjsDocEncoder.encode(CanvasSize(320, 240), "#ffffff", png, Point(4.0, 8.0), 80.0, listOf(layer), mapOf(layer.id to png))
        val document = YjsDoc(bytes)
        assertEquals(1.0, (document.getMetaValue("formatVersion") as Number).toDouble())
        assertEquals(CanvasSize(320, 240), document.getCanvasSize())
        assertEquals(Point(4.0, 8.0), document.getImgOffset())
        assertEquals(80.0, document.getImgAlpha())
        assertEquals(png, document.getSourceImage())
        assertEquals(mapOf(layer.id to png), document.getImages())
        assertEquals(listOf(layer), document.getLayers())
    }

    @Test
    fun readsAllNineLayersAndResourcesFromTheIndependentWebDocument() {
        val document = YjsDoc(Base64Lenient.decode(webDocument))
        assertEquals(1.0, (document.getMetaValue("formatVersion") as Number).toDouble())
        assertEquals(CanvasSize(640, 480), document.getCanvasSize())
        assertEquals(Point(4.0, 8.0), document.getImgOffset())
        assertEquals(80.0, document.getImgAlpha())
        assertEquals(listOf("arrow", "rect", "ellipse", "highlight", "mosaic", "text", "image", "freehand", "sticker"), document.getLayers().map { it.type })
        val image = document.getLayers().filterIsInstance<ImageLayer>().single()
        assertEquals(0.5, image.opacity)
        assertEquals(20.0, image.rotation)
        assertEquals(setOf(image.id), document.getImages().keys)
        assertEquals(document.getSourceImage(), document.getImages().getValue(image.id))
        val encoded = YjsDocEncoder.encode(document.getCanvasSize(), document.getBgColor(), document.getSourceImage(), document.getImgOffset(), document.getImgAlpha(), document.getLayers(), document.getImages())
        val restored = YjsDoc(encoded)
        assertEquals(document.getLayers(), restored.getLayers())
        assertEquals(document.getImages(), restored.getImages())
    }

    @Test
    fun readsReorderedAndDeletedLayersFromAnEditedWebDocumentWithUndoHistory() {
        val document = YjsDoc(Base64Lenient.decode(
            "AY0BAgAoAQRtZXRhDWZvcm1hdFZlcnNpb24BfQEoAQRtZXRhC2NhbnZhc1dpZHRoAX2ACigBBG1ldGEMY2FudmFzSGVpZ2h0AX2g" +
            "BygBBG1ldGEHYmdDb2xvcgF3ByNmZmZmZmYoAQRtZXRhCmltZ09mZnNldFgBfQQoAQRtZXRhCmltZ09mZnNldFkBfQgoAQRtZXRh" +
            "CGltZ0FscGhhAX2QASgBBG1ldGELc291cmNlSW1hZ2UBd3ZkYXRhOmltYWdlL3BuZztiYXNlNjQsaVZCT1J3MEtHZ29BQUFBTlNV" +
            "aEVVZ0FBQUFFQUFBQUJDQVlBQUFBZkZjU0pBQUFBRFVsRVFWUjRuR1A0ejhEd0h3QUZBQUgvaVprOUhRQUFBQUJKUlU1RXJrSmdn" +
            "Zz09BwEGbGF5ZXJzASgAAggCaWQBdwVhcnJvdygAAggEdHlwZQF3BWFycm93KAACCAd2aXNpYmxlAXgoAAIIBG5hbWUBdwVhcnJv" +
            "dygAAggCeDEBfQooAAIIAnkxAX0UKAACCAJ4MgF9pAEoAAIIAnkyAX2QASgAAggFY29sb3IBdwcjZWY0NDQ0KAACCAlsaW5lV2lk" +
            "dGgBfQQoAAIICHJvdGF0aW9uAX0PhwIIASgAAhQCaWQBdwRyZWN0KAACFAR0eXBlAXcEcmVjdCgAAhQHdmlzaWJsZQF4KAACFARu" +
            "YW1lAXcEcmVjdCgAAhQBeAF9FCgAAhQBeQF9HigAAhQBdwF9kAEoAAIUAWgBfSgoAAIUBWNvbG9yAXcHIzIyYzU1ZSgAAhQJbGlu" +
            "ZVdpZHRoAX0ChwIUASgAAh8CaWQBdwdlbGxpcHNlKAACHwR0eXBlAXcHZWxsaXBzZSgAAh8HdmlzaWJsZQF4KAACHwRuYW1lAXcH" +
            "ZWxsaXBzZSgAAh8CY3gBfZYCKAACHwJjeQF9kAEoAAIfAnJ4AX0oKAACHwJyeQF9FCgAAh8FY29sb3IBdwcjM2I4MmY2KAACHwls" +
            "aW5lV2lkdGgBfQSHAh8BKAACKgJpZAF3CWhpZ2hsaWdodCgAAioEdHlwZQF3CWhpZ2hsaWdodCgAAioHdmlzaWJsZQF4KAACKgRu" +
            "YW1lAXcJaGlnaGxpZ2h0KAACKgF4AX0oKAACKgF5AX24ASgAAioBdwF9pAEoAAIqAWgBfSgoAAIqBWNvbG9yAXcTcmdiYSgyNTUs" +
            "MjU1LDAsMC40KYcCKgEoAAI0AmlkAXcGbW9zYWljKAACNAR0eXBlAXcGbW9zYWljKAACNAd2aXNpYmxlAXgoAAI0BG5hbWUBdwZt" +
            "b3NhaWMoAAI0AXgBfYwCKAACNAF5AX24ASgAAjQBdwF9pAEoAAI0AWgBfSgoAAI0CWJsb2NrU2l6ZQF9DIcCNAEoAAI+AmlkAXcE" +
            "dGV4dCgAAj4EdHlwZQF3BHRleHQoAAI+B3Zpc2libGUBeCgAAj4EbmFtZQF3BHRleHQoAAI+AXgBfYgDKAACPgF5AX2IAygAAj4E" +
            "dGV4dAF3DVBvcnRhYmxlIHRleHQoAAI+CGZvbnRTaXplAX0YKAACPgVjb2xvcgF3ByMwMDAwMDAoAAI+CmZvbnRGYW1pbHkBdwpz" +
            "YW5zLXNlcmlmKAACPgpmb250V2VpZ2h0AXcEYm9sZCgAAj4JZm9udFN0eWxlAXcGaXRhbGljKAACPgl0ZXh0QWxpZ24BdwRsZWZ0" +
            "KAACPg1sZXR0ZXJTcGFjaW5nAX0CKAACPg50ZXh0RGVjb3JhdGlvbgF3CXVuZGVybGluZScAAj4KdGV4dFN0cm9rZQEoAAJOBXdp" +
            "ZHRoAX0CKAACTgVjb2xvcgF3ByNlZjQ0NDQnAAI+CnRleHRTaGFkb3cBKAACUQdvZmZzZXRYAX0CKAACUQdvZmZzZXRZAX0EKAAC" +
            "UQRibHVyAX0IKAACUQVjb2xvcgF3D3JnYmEoMCwwLDAsMC43KSgAAj4EZ2xvdwF4KAACPghyb3RhdGlvbgF9CigAAj4IbWF4V2lk" +
            "dGgBfbADhwI+ASgAAlkCaWQBdwVpbWFnZSgAAlkEdHlwZQF3BWltYWdlKAACWQd2aXNpYmxlAXgoAAJZBG5hbWUBdwVpbWFnZSgA" +
            "AlkBeAF9kAYoAAJZAXkBfYgDKAACWQF3AX2AAigAAlkBaAF9oAEoAAJZB29wYWNpdHkBfD8AAAAoAAJZCHJvdGF0aW9uAX0UhwJZ" +
            "ASgAAmQCaWQBdwhmcmVlaGFuZCgAAmQEdHlwZQF3CGZyZWVoYW5kKAACZAd2aXNpYmxlAXgoAAJkBG5hbWUBdwhmcmVlaGFuZCcA" +
            "AmQGcG9pbnRzAAgAAmkDdQJ9Cn2sBHUCfRR9tgR1An0ofYAFKAACZAVjb2xvcgF3ByM4YjVjZjYoAAJkCWxpbmVXaWR0aAF9CIcC" +
            "ZAEoAAJvAmlkAXcHc3RpY2tlcigAAm8EdHlwZQF3B3N0aWNrZXIoAAJvB3Zpc2libGUBeCgAAm8EbmFtZQF3B3N0aWNrZXIoAAJv" +
            "AXgBfZAGKAACbwF5AX2eBSgAAm8BdwF9uAEoAAJvAWgBfZABKAACbwR0ZXh0AXcETm90ZSgAAm8FY29sb3IBdwcjZmVmMDhhKAAC" +
            "bwhmb250U2l6ZQF9FCgAAm8KZm9udFdlaWdodAF3AzYwMCgAAm8JZm9udFN0eWxlAXcGbm9ybWFsKAACbwhyb3RhdGlvbgF9BSgB" +
            "BmltYWdlcwVpbWFnZQF3dmRhdGE6aW1hZ2UvcG5nO2Jhc2U2NCxpVkJPUncwS0dnb0FBQUFOU1VoRVVnQUFBQUVBQUFBQkNBWUFB" +
            "QUFmRmNTSkFBQUFEVWxFUVZSNG5HUDR6OER3SHdBRkFBSC9pWms5SFFBQUFBQkpSVTVFcmtKZ2dnPT1HAggBKAACfwJpZAF3B3N0" +
            "aWNrZXIoAAJ/BHR5cGUBdwdzdGlja2VyKAACfwd2aXNpYmxlAXgoAAJ/BG5hbWUBdwdzdGlja2VyKAACfwF4AX2QBigAAn8BeQF9" +
            "ngUoAAJ/AXcBfbgBKAACfwFoAX2QASgAAn8EdGV4dAF3BE5vdGUoAAJ/BWNvbG9yAXcHI2ZlZjA4YSgAAn8IZm9udFNpemUBfRQo" +
            "AAJ/CmZvbnRXZWlnaHQBdwM2MDAoAAJ/CWZvbnRTdHlsZQF3Bm5vcm1hbCgAAn8Icm90YXRpb24BfQWoAkUBdxRFZGl0ZWQgcG9y" +
            "dGFibGUgdGV4dAECAwgMRQFvDw=="
        ))
        assertEquals(listOf("sticker", "rect", "ellipse", "highlight", "mosaic", "text", "image", "freehand"), document.getLayers().map { it.id })
        val text = document.getLayers().filterIsInstance<com.ismartcoding.plain.lib.yjs.TextLayer>().single()
        assertEquals("Edited portable text", text.text)
        assertEquals(setOf("image"), document.getImages().keys)
    }

    @Test
    fun preservesTheStandardYjsOrderAfterMultipleEditorsInsertLayers() {
        val document = YjsDoc(Base64Lenient.decode(
            "BAsoAEcUAAEoACgAAmlkAXcBZCgAKAAEdHlwZQF3BHJlY3QoACgAB3Zpc2libGUBeCgAKAAEbmFtZQF3BHJlY3QoACgAAXgBfRQo" +
            "ACgAAXkBfR4oACgAAXcBfZABKAAoAAFoAX0oKAAoAAVjb2xvcgF3ByMyMmM1NWUoACgACWxpbmVXaWR0aAF9AgseAEcKAwEoAB4A" +
            "AmlkAXcBYygAHgAEdHlwZQF3BHJlY3QoAB4AB3Zpc2libGUBeCgAHgAEbmFtZQF3BHJlY3QoAB4AAXgBfRQoAB4AAXkBfR4oAB4A" +
            "AXcBfZABKAAeAAFoAX0oKAAeAAVjb2xvcgF3ByMyMmM1NWUoAB4ACWxpbmVXaWR0aAF9AgsUAEcKAwEoABQAAmlkAXcBYigAFAAE" +
            "dHlwZQF3BHJlY3QoABQAB3Zpc2libGUBeCgAFAAEbmFtZQF3BHJlY3QoABQAAXgBfRQoABQAAXkBfR4oABQAAXcBfZABKAAUAAFo" +
            "AX0oKAAUAAVjb2xvcgF3ByMyMmM1NWUoABQACWxpbmVXaWR0aAF9Ag4KACgBBG1ldGENZm9ybWF0VmVyc2lvbgF9ASgBBG1ldGEL" +
            "Y2FudmFzV2lkdGgBfYAKKAEEbWV0YQxjYW52YXNIZWlnaHQBfaAHBwEGbGF5ZXJzASgACgMCaWQBdwFhKAAKAwR0eXBlAXcEcmVj" +
            "dCgACgMHdmlzaWJsZQF4KAAKAwRuYW1lAXcEcmVjdCgACgMBeAF9FCgACgMBeQF9HigACgMBdwF9kAEoAAoDAWgBfSgoAAoDBWNv" +
            "bG9yAXcHIzIyYzU1ZSgACgMJbGluZVdpZHRoAX0CAA=="
        ))
        assertEquals(listOf("d", "b", "c", "a"), document.getLayers().map { it.id })
    }

    @Test
    fun encodesMapStringsAndArrayOriginsForStandardYjsReaders() {
        val bytes = YjsDocEncoder.encode(CanvasSize(320, 240), "#ffffff", null, Point(0.0, 0.0), 100.0, listOf(RectLayer(id = "first"), RectLayer(id = "second")))
        val decoder = YjsDecoder(bytes)
        assertEquals(1L, decoder.readVarUint())
        decoder.readVarUint()
        assertEquals(1L, decoder.readVarUint())
        assertEquals(0L, decoder.readVarUint())
        val metadata = mutableMapOf<String, Any?>()
        var info = decoder.readUint8()
        while (info == 40) {
            assertEquals(1L, decoder.readVarUint())
            assertEquals("meta", decoder.readVarString())
            val key = decoder.readVarString()
            assertEquals(1L, decoder.readVarUint())
            metadata[key] = decoder.readAny()
            info = decoder.readUint8()
        }
        assertEquals("#ffffff", metadata["bgColor"])
        assertEquals(1.0, (metadata.getValue("formatVersion") as Number).toDouble())
        assertEquals(7, info)
        assertEquals(1L, decoder.readVarUint())
        assertEquals("layers", decoder.readVarString())
        assertEquals(1L, decoder.readVarUint())
        val firstLayer = mutableMapOf<String, Any?>()
        info = decoder.readUint8()
        while (info == 40) {
            assertEquals(0L, decoder.readVarUint())
            assertEquals(1L, decoder.readVarUint())
            assertEquals(metadata.size.toLong(), decoder.readVarUint())
            val key = decoder.readVarString()
            assertEquals(1L, decoder.readVarUint())
            firstLayer[key] = decoder.readAny()
            info = decoder.readUint8()
        }
        assertEquals("first", firstLayer["id"])
        assertEquals("rect", firstLayer["type"])
        assertEquals(135, info)
        assertEquals(1L, decoder.readVarUint())
        assertEquals(metadata.size.toLong(), decoder.readVarUint())
        assertEquals(1L, decoder.readVarUint())
    }

    @Test
    fun rejectsUnsupportedLayersWithoutDroppingThem() {
        val payload = "AQYDACgBBG1ldGENZm9ybWF0VmVyc2lvbgF9AQcBBmxheWVycwEoAAMBAmlkAXcHdW5rbm93bigAAwEEdHlwZQF3C3Vuc3VwcG9ydGVkKAADAQd2aXNpYmxlAXgoAAMBBG5hbWUBdwdVbmtub3duAA=="
        assertFailsWith<IllegalStateException> { YjsDoc(Base64Lenient.decode(payload)).getLayers() }
    }

    // Generated from plainapp-test/fixtures/editor/document-v1.yjs.
    private val webDocument =
        "AX0CACgBBG1ldGENZm9ybWF0VmVyc2lvbgF9ASgBBG1ldGELY2FudmFzV2lkdGgBfYAKKAEEbWV0YQxjYW52YXNIZWlnaHQBfaAH" +
        "KAEEbWV0YQdiZ0NvbG9yAXcHI2ZmZmZmZigBBG1ldGEKaW1nT2Zmc2V0WAF9BCgBBG1ldGEKaW1nT2Zmc2V0WQF9CCgBBG1ldGEI" +
        "aW1nQWxwaGEBfZABKAEEbWV0YQtzb3VyY2VJbWFnZQF3dmRhdGE6aW1hZ2UvcG5nO2Jhc2U2NCxpVkJPUncwS0dnb0FBQUFOU1Vo" +
        "RVVnQUFBQUVBQUFBQkNBWUFBQUFmRmNTSkFBQUFEVWxFUVZSNG5HUDR6OER3SHdBRkFBSC9pWms5SFFBQUFBQkpSVTVFcmtKZ2dn" +
        "PT0HAQZsYXllcnMBKAACCAJpZAF3BWFycm93KAACCAR0eXBlAXcFYXJyb3coAAIIB3Zpc2libGUBeCgAAggEbmFtZQF3BWFycm93" +
        "KAACCAJ4MQF9CigAAggCeTEBfRQoAAIIAngyAX2kASgAAggCeTIBfZABKAACCAVjb2xvcgF3ByNlZjQ0NDQoAAIICWxpbmVXaWR0" +
        "aAF9BCgAAggIcm90YXRpb24BfQ+HAggBKAACFAJpZAF3BHJlY3QoAAIUBHR5cGUBdwRyZWN0KAACFAd2aXNpYmxlAXgoAAIUBG5h" +
        "bWUBdwRyZWN0KAACFAF4AX0UKAACFAF5AX0eKAACFAF3AX2QASgAAhQBaAF9KCgAAhQFY29sb3IBdwcjMjJjNTVlKAACFAlsaW5l" +
        "V2lkdGgBfQKHAhQBKAACHwJpZAF3B2VsbGlwc2UoAAIfBHR5cGUBdwdlbGxpcHNlKAACHwd2aXNpYmxlAXgoAAIfBG5hbWUBdwdl" +
        "bGxpcHNlKAACHwJjeAF9lgIoAAIfAmN5AX2QASgAAh8CcngBfSgoAAIfAnJ5AX0UKAACHwVjb2xvcgF3ByMzYjgyZjYoAAIfCWxp" +
        "bmVXaWR0aAF9BIcCHwEoAAIqAmlkAXcJaGlnaGxpZ2h0KAACKgR0eXBlAXcJaGlnaGxpZ2h0KAACKgd2aXNpYmxlAXgoAAIqBG5h" +
        "bWUBdwloaWdobGlnaHQoAAIqAXgBfSgoAAIqAXkBfbgBKAACKgF3AX2kASgAAioBaAF9KCgAAioFY29sb3IBdxNyZ2JhKDI1NSwy" +
        "NTUsMCwwLjQphwIqASgAAjQCaWQBdwZtb3NhaWMoAAI0BHR5cGUBdwZtb3NhaWMoAAI0B3Zpc2libGUBeCgAAjQEbmFtZQF3Bm1v" +
        "c2FpYygAAjQBeAF9jAIoAAI0AXkBfbgBKAACNAF3AX2kASgAAjQBaAF9KCgAAjQJYmxvY2tTaXplAX0MhwI0ASgAAj4CaWQBdwR0" +
        "ZXh0KAACPgR0eXBlAXcEdGV4dCgAAj4HdmlzaWJsZQF4KAACPgRuYW1lAXcEdGV4dCgAAj4BeAF9iAMoAAI+AXkBfYgDKAACPgR0" +
        "ZXh0AXcNUG9ydGFibGUgdGV4dCgAAj4IZm9udFNpemUBfRgoAAI+BWNvbG9yAXcHIzAwMDAwMCgAAj4KZm9udEZhbWlseQF3CnNh" +
        "bnMtc2VyaWYoAAI+CmZvbnRXZWlnaHQBdwRib2xkKAACPglmb250U3R5bGUBdwZpdGFsaWMoAAI+CXRleHRBbGlnbgF3BGxlZnQo" +
        "AAI+DWxldHRlclNwYWNpbmcBfQIoAAI+DnRleHREZWNvcmF0aW9uAXcJdW5kZXJsaW5lJwACPgp0ZXh0U3Ryb2tlASgAAk4Fd2lk" +
        "dGgBfQIoAAJOBWNvbG9yAXcHI2VmNDQ0NCcAAj4KdGV4dFNoYWRvdwEoAAJRB29mZnNldFgBfQIoAAJRB29mZnNldFkBfQQoAAJR" +
        "BGJsdXIBfQgoAAJRBWNvbG9yAXcPcmdiYSgwLDAsMCwwLjcpKAACPgRnbG93AXgoAAI+CHJvdGF0aW9uAX0KKAACPghtYXhXaWR0" +
        "aAF9sAOHAj4BKAACWQJpZAF3BWltYWdlKAACWQR0eXBlAXcFaW1hZ2UoAAJZB3Zpc2libGUBeCgAAlkEbmFtZQF3BWltYWdlKAAC" +
        "WQF4AX2QBigAAlkBeQF9iAMoAAJZAXcBfYACKAACWQFoAX2gASgAAlkHb3BhY2l0eQF8PwAAACgAAlkIcm90YXRpb24BfRSHAlkB" +
        "KAACZAJpZAF3CGZyZWVoYW5kKAACZAR0eXBlAXcIZnJlZWhhbmQoAAJkB3Zpc2libGUBeCgAAmQEbmFtZQF3CGZyZWVoYW5kJwAC" +
        "ZAZwb2ludHMACAACaQN1An0KfawEdQJ9FH22BHUCfSh9gAUoAAJkBWNvbG9yAXcHIzhiNWNmNigAAmQJbGluZVdpZHRoAX0IhwJk" +
        "ASgAAm8CaWQBdwdzdGlja2VyKAACbwR0eXBlAXcHc3RpY2tlcigAAm8HdmlzaWJsZQF4KAACbwRuYW1lAXcHc3RpY2tlcigAAm8B" +
        "eAF9kAYoAAJvAXkBfZ4FKAACbwF3AX24ASgAAm8BaAF9kAEoAAJvBHRleHQBdwROb3RlKAACbwVjb2xvcgF3ByNmZWYwOGEoAAJv" +
        "CGZvbnRTaXplAX0UKAACbwpmb250V2VpZ2h0AXcDNjAwKAACbwlmb250U3R5bGUBdwZub3JtYWwoAAJvCHJvdGF0aW9uAX0FKAEG" +
        "aW1hZ2VzBWltYWdlAXd2ZGF0YTppbWFnZS9wbmc7YmFzZTY0LGlWQk9SdzBLR2dvQUFBQU5TVWhFVWdBQUFBRUFBQUFCQ0FZQUFB" +
        "QWZGY1NKQUFBQURVbEVRVlI0bkdQNHo4RHdId0FGQUFIL2laazlIUUFBQUFCSlJVNUVya0pnZ2c9PQA="
}
