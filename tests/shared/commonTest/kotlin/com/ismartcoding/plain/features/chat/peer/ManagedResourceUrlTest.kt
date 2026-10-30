package com.ismartcoding.plain.features.chat.peer

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ManagedResourceUrlTest {
    @Test
    fun thumbnailKeepsTheOriginalAspectRatioAndOpaqueReference() {
        val url = Url(managedResourceUrl("https://device.invalid:8443/", "opaque+/=reference", 256))
        assertEquals("/fs", url.encodedPath)
        assertEquals("opaque+/=reference", url.parameters["id"])
        assertEquals("256", url.parameters["w"])
        assertEquals("256", url.parameters["h"])
        assertEquals("false", url.parameters["cc"])
    }

    @Test
    fun fullResourceDoesNotRequestAThumbnail() {
        val url = Url(managedResourceUrl("https://device.invalid:8443", "opaque", 0))
        assertEquals("opaque", url.parameters["id"])
        assertNull(url.parameters["w"])
        assertNull(url.parameters["h"])
        assertNull(url.parameters["cc"])
    }
}
