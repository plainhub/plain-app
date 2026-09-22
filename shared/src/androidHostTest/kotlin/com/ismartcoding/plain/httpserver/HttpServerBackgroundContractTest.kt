package com.ismartcoding.plain.httpserver

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpServerBackgroundContractTest {
    @Test
    fun removingActivityTaskDoesNotStopTheForegroundServer() {
        val manifest = File("../app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(manifest)
        val android = "http://schemas.android.com/apk/res/android"
        val services = document.getElementsByTagName("service")
        val service = (0 until services.length).map { services.item(it) as org.w3c.dom.Element }
            .single { it.getAttributeNS(android, "name") == ".services.HttpServerService" }
        assertEquals("false", service.getAttributeNS(android, "stopWithTask"))
    }
}
