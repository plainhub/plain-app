package com.ismartcoding.plain.features.chat.peer

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ManagedResourceUrlTest {
    @Test
    fun thumbnailUsesNamedSizeAndKeepsOpaqueReference() {
        val url = Url(managedResourceUrl("https://device.invalid:8443/", "opaque+/=reference", ResourceSize.SM))
        assertEquals("/fs", url.encodedPath)
        assertEquals("opaque+/=reference", url.parameters["id"])
        assertEquals("sm", url.parameters["s"])
        assertNull(url.parameters["w"])
        assertNull(url.parameters["h"])
        assertNull(url.parameters["cc"])
    }

    @Test
    fun largePreviewUsesNamedSize() {
        val url = Url(managedResourceUrl("https://device.invalid", "opaque", ResourceSize.LG))
        assertEquals("lg", url.parameters["s"])
    }

    @Test
    fun fullResourceDoesNotRequestAThumbnail() {
        val url = Url(managedResourceUrl("https://device.invalid:8443", "opaque", null))
        assertEquals("opaque", url.parameters["id"])
        assertNull(url.parameters["s"])
        assertNull(url.parameters["w"])
        assertNull(url.parameters["h"])
        assertNull(url.parameters["cc"])
    }
}
