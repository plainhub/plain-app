package com.ismartcoding.plain.api.graphql

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.files.extension
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CanonicalFileModelTest {
    private val payload = """{"name":"song.mp3","path":"/music/song.mp3","permission":"rw","createdAt":null,"updatedAt":"2026-01-01T00:00:00Z","size":42,"isDir":false,"childCount":0,"mediaId":null}"""

    @Test
    fun mainSchemaPayloadProvidesFileIdentityWithoutAnotherSerializedField() {
        val file = JsonHelper.jsonDecode<File>(payload)
        val item: IData = file
        assertEquals("/music/song.mp3", item.id)
        assertEquals(null, file.mediaId)
        assertFalse(JsonHelper.jsonEncodeToElement(file).jsonObject.containsKey("id"))
    }

    @Test
    fun systemClassificationComesFromTheMainFileContract() {
        val file = JsonHelper.jsonDecode<File>(payload.dropLast(1) + ",\"isSystem\":true}")
        assertEquals(true, file.isSystem)
        assertEquals("/music/song.mp3", file.path)
        assertEquals(42L, file.size)
    }

    @Test
    fun pathChangesUpdateIdentityAndExtension() {
        val file = JsonHelper.jsonDecode<File>(payload)
        assertEquals("mp3", file.extension)
        file.path = "/music/song.flac"
        assertEquals("/music/song.flac", file.id)
        assertEquals("flac", file.extension)
        val item: IData = file
        item.id = "/music/renamed.wav"
        assertEquals("/music/renamed.wav", file.path)
        assertEquals("wav", file.extension)
    }
}
