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
        assertEquals("preview", url.parameters["action"])
        assertEquals("opaque+/=reference", url.parameters["id"])
        assertEquals("sm", url.parameters["size"])
        assertNull(url.parameters["w"])
        assertNull(url.parameters["h"])
        assertNull(url.parameters["cc"])
    }

    @Test
    fun largePreviewUsesNamedSize() {
        val url = Url(managedResourceUrl("https://device.invalid", "opaque", ResourceSize.LG))
        assertEquals("lg", url.parameters["size"])
    }

    @Test
    fun downloadsRequestOriginalBytesExplicitly() {
        val url = Url(managedResourceUrl("https://device.invalid", "opaque", null, com.ismartcoding.plain.features.files.FileAction.DOWNLOAD))
        assertEquals("download", url.parameters["action"])
        assertNull(url.parameters["size"])
    }

    @Test
    fun fullResourceDoesNotRequestAThumbnail() {
        val url = Url(managedResourceUrl("https://device.invalid:8443", "opaque", null))
        assertEquals("opaque", url.parameters["id"])
        assertNull(url.parameters["size"])
        assertNull(url.parameters["w"])
        assertNull(url.parameters["h"])
        assertNull(url.parameters["cc"])
    }
}
