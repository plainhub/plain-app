package com.ismartcoding.plain.tests

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.ai.ImageIndexCatalog
import com.ismartcoding.plain.appContext
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ImageIndexRustHttpTest {
    @Test
    fun platformCatalogResolvesSyntheticImagesAndRejectsStaleRevisions() {
        val resolver = appContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "plain-index-${UUID.randomUUID()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PlainSyntheticTests")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        try {
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(0xff336699.toInt())
                checkNotNull(resolver.openOutputStream(uri)).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { bitmap.recycle() }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            assertEquals(1, resolver.update(uri, values, null, null))
            val snapshot = ImageIndexCatalog.snapshot()
            val revision = snapshot.getValue("revision").jsonPrimitive.content
            val id = checkNotNull(uri.lastPathSegment)
            val selected = ImageIndexCatalog.resolve(revision, listOf(id))
            assertEquals(id, selected.single().jsonObject.getValue("id").jsonPrimitive.content)
            assertTrue(selected.single().jsonObject.getValue("path").jsonPrimitive.content.endsWith(".png"))
            ImageIndexCatalog.close()
            assertTrue(runCatching { ImageIndexCatalog.verify(revision) }.isFailure)
        } finally {
            resolver.delete(uri, null, null)
            ImageIndexCatalog.close()
        }
    }
}
