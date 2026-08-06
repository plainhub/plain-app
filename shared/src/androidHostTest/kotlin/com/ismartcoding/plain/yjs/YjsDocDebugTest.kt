package com.ismartcoding.plain.yjs

import com.ismartcoding.plain.lib.yjs.YjsDoc
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import org.junit.Assume.assumeTrue

class YjsDocDebugTest {

    private val payloadFile = java.io.File("/tmp/web-payload.b64")

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun debug_parse_structure() {
        // This is a scratch debug test that only runs when a captured payload
        // is present on disk; skip silently otherwise so CI stays green.
        assumeTrue("Missing scratch payload $payloadFile — skipping", payloadFile.exists())
        val bytes = Base64.Default.decode(payloadFile.readText().trim())
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
