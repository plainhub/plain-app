package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.ImageEditorProjectHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ImageEditorRustHttpTest {
    @Test
    fun canvasProjectsUseRustAndListsReturnSummaries() = runBlocking {
        val id = "image-rust-${UUID.randomUUID()}"
        try {
            val saved = ImageEditorProjectHelper.addOrUpdateAsync(id) {
                stateB64 = "AQID"
                thumbnail = "data:image/jpeg;base64,AQID"
                canvasWidth = 100
                canvasHeight = 200
                layerCount = 2
            }
            val summary = ImageEditorProjectHelper.listAsync(50).first { it.id.value == id }
            assertEquals(saved.thumbnail, summary.thumbnail)
            assertEquals(100, summary.canvasWidth)
            val updated = ImageEditorProjectHelper.addOrUpdateAsync(id) { stateB64 = "BAU="; layerCount = 3 }
            assertEquals(saved.createdAt, updated.createdAt)
            assertEquals(saved.thumbnail, updated.thumbnail)
            assertEquals("BAU=", checkNotNull(ImageEditorProjectHelper.getByIdAsync(id)).stateB64)
            ImageEditorProjectHelper.broadcastUpdate(id, "AQID")
            ImageEditorProjectHelper.deleteAsync(id)
            assertNull(ImageEditorProjectHelper.getByIdAsync(id))
        } finally {
            ImageEditorProjectHelper.deleteAsync(id)
        }
    }
}
