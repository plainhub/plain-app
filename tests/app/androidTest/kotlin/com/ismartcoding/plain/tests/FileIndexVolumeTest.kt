package com.ismartcoding.plain.tests

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.platform.system.fileIndexRows
import com.ismartcoding.plain.rust.host.provider.ProviderPlan
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class FileIndexVolumeTest {
    @Test
    fun resolvesIndexedImagesOnEveryMountedExternalVolume() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val volumes = MediaStore.getExternalVolumeNames(context)
        assumeTrue(volumes.isNotEmpty())
        for (volume in volumes) {
            val name = "file-index-${UUID.randomUUID()}.png"
            val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.getContentUri(volume), ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }))
            try {
                requireNotNull(resolver.openOutputStream(uri)).use { stream ->
                    check(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, stream))
                }
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                val id = ContentUris.parseId(uri).toString()
                val rows = fileIndexRows(ProviderPlan(idsColumn = "_id", ids = listOf(id)), 1, 0, FileSortBy.NAME_ASC)
                assertEquals("Index lookup on $volume", listOf(id to name), rows.map { it.mediaId to it.name })
            } finally {
                resolver.delete(uri, null, null)
            }
        }
    }
}
