package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.features.files.FileAction
import kotlin.test.Test
import kotlin.test.assertEquals

class ManagedResourceUrlTest {
    @Test
    fun thumbnailUsesNamedSizeAndKeepsOpaqueReference() {
        assertEquals(
            "https://[2001:db8::1]:8443/fs?id=opaque%2B%2F%3D%26%25%F0%9F%94%91&action=PREVIEW&size=SM",
            managedResourceUrl("https://[2001:db8::1]:8443/", "opaque+/=&%🔑", ResourceSize.SM),
        )
    }

    @Test
    fun largePreviewUsesNamedSize() {
        assertEquals(
            "https://device.invalid/fs?id=opaque&action=PREVIEW&size=LG",
            managedResourceUrl("https://device.invalid", "opaque", ResourceSize.LG),
        )
    }

    @Test
    fun downloadsRequestOriginalBytesExplicitly() {
        assertEquals(
            "https://device.invalid/fs?id=opaque&action=DOWNLOAD",
            managedResourceUrl("https://device.invalid", "opaque", null, FileAction.DOWNLOAD),
        )
    }

    @Test
    fun fullResourceDoesNotRequestAThumbnail() {
        assertEquals(
            "https://device.invalid:8443/fs?id=opaque&action=PREVIEW",
            managedResourceUrl("https://device.invalid:8443", "opaque", null),
        )
    }
}
