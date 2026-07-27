package com.ismartcoding.plain.yjs

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test

class YjsDocDebugTest {

    private val webPayloadB64 = java.io.File("/tmp/web-payload.b64").readText().trim()

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun debug_parse_structure() {
        val bytes = Base64.Default.decode(webPayloadB64)
        println("Payload size: ${bytes.size} bytes")

        val doc = YjsDoc(bytes)
        println("Canvas size: ${doc.getCanvasSize()}")
        println("Bg color: ${doc.getBgColor()}")
        println("Img alpha: ${doc.getImgAlpha()}")
        println("Img offset: ${doc.getImgOffset()}")
        println("Source image: ${doc.getSourceImage()}")

        val layers = doc.getLayers()
        println("Layer count: ${layers.size}")
        layers.forEach { layer ->
            println("  layer: id=${layer.id} type=${layer.type} visible=${layer.visible}")
        }
    }
}
