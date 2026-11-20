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
import com.ismartcoding.plain.features.media.actions.MediaAction
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import java.util.UUID

class FileIndexVolumeTest {
    @Test
    fun resolvesIndexedImagesOnEveryMountedExternalVolume() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        withIndexedImages { id, name, volume ->
            val rows = fileIndexRows(ProviderPlan(idsColumn = "_id", ids = listOf(id)), 1, 0, FileSortBy.NAME_ASC)
            assertEquals("Index lookup on $volume", listOf(id to name), rows.map { it.mediaId to it.name })
        }
    }

    @Test
    fun trashesAndRestoresAnImageOnSecondaryStorage() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val secondaryVolumes = MediaStore.getExternalVolumeNames(InstrumentationRegistry.getInstrumentation().targetContext) - MediaStore.VOLUME_EXTERNAL_PRIMARY
        assumeTrue(secondaryVolumes.isNotEmpty())
        withIndexedImages(secondaryVolumes) { id, _, volume ->
            for (action in listOf(MediaAction.TRASH, MediaAction.RESTORE)) {
                val result = GraphqlApi.execute(Mutation.mediaAction(action, DataType.IMAGE, "ids:$id", ""))
                assertEquals("$action on $volume", 1, result.affectedCount)
            }
        }
    }

    private suspend fun withIndexedImages(volumes: Set<String> = MediaStore.getExternalVolumeNames(InstrumentationRegistry.getInstrumentation().targetContext), checkImage: suspend (String, String, String) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
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
                checkImage(id, name, volume)
            } finally {
                resolver.delete(uri, null, null)
            }
        }
    }
}
