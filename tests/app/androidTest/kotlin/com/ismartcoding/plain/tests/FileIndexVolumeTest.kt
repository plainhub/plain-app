package com.ismartcoding.plain.tests

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.crypto.xChaCha20Poly1305Encrypt
import com.ismartcoding.plain.network.TestHttpClientSpec
import com.ismartcoding.plain.network.createTestHttpClient
import com.ismartcoding.plain.network.get
import com.ismartcoding.plain.platform.system.fileIndexRows
import com.ismartcoding.plain.rust.RustRuntime
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.host.provider.ProviderPlan
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import com.ismartcoding.plain.features.media.actions.MediaAction
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import kotlin.io.encoding.Base64

class FileIndexVolumeTest {
    @Serializable
    private data class ResourceAccess(val clientId: String, val urlToken: String, val httpPort: Int)

    @Test
    fun resolvesIndexedImagesOnEveryMountedExternalVolume() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        withIndexedImages { id, name, volume, _ ->
            val rows = fileIndexRows(ProviderPlan(idsColumn = "_id", ids = listOf(id)), 1, 0, FileSortBy.NAME_ASC)
            assertEquals("Index lookup on $volume", listOf(id to name), rows.map { it.mediaId to it.name })
        }
    }

    @Test
    fun trashesAndRestoresAnImageOnSecondaryStorage() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val secondaryVolumes = MediaStore.getExternalVolumeNames(InstrumentationRegistry.getInstrumentation().targetContext) - MediaStore.VOLUME_EXTERNAL_PRIMARY
        assumeTrue(secondaryVolumes.isNotEmpty())
        withIndexedImages(secondaryVolumes) { id, _, volume, _ ->
            for (action in listOf(MediaAction.TRASH, MediaAction.RESTORE)) {
                val result = GraphqlApi.execute(Mutation.mediaAction(action, DataType.IMAGE, "ids:$id", ""))
                assertEquals("$action on $volume", 1, result.affectedCount)
            }
        }
    }

    @Test
    fun previewsTrashedImageByMediaIdOverHttp() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val originalRemoteAccess = Prefs.remoteAccess.value
        try {
            Prefs.remoteAccess.value = true
            RustRuntime.start()
            val access = GraphqlApi.execute(GraphqlRequest<ResourceAccess>(
                "{ app { clientId urlToken httpPort } }", field = "app",
            ))
            createTestHttpClient(TestHttpClientSpec.Local()).use { client ->
                withIndexedImages(setOf(MediaStore.VOLUME_EXTERNAL_PRIMARY)) { id, _, _, uri ->
                    val plan = ProviderPlan(idsColumn = "_id", ids = listOf(id))
                    val original = File(fileIndexRows(plan, 1, 0, FileSortBy.NAME_ASC).single().path).readBytes()
                    val scope = "c.${access.clientId}"
                    val payload = JsonHelper.jsonEncode(mapOf("type" to "MEDIA", "id" to id))
                    val encrypted = xChaCha20Poly1305Encrypt(
                        Base64.decode(access.urlToken), SecureRandom().generateSeed(24),
                        payload.encodeToByteArray(), "FS.$scope".encodeToByteArray(),
                    )
                    val resourceId = "$scope.${Base64.UrlSafe.encode(encrypted).trimEnd('=')}"
                    val url = "http://127.0.0.1:${access.httpPort}/fs?id=$resourceId&action=PREVIEW"
                    val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
                    assertEquals(1, resolver.update(uri, ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_TRASHED, 1)
                    }, null, null))
                    assertTrue(fileIndexRows(plan, 1, 0, FileSortBy.NAME_ASC).isEmpty())
                    for (size in listOf("&size=SM", "&size=LG", "")) {
                        client.get(url + size).use { response ->
                            assertEquals("Trashed preview $size", 200, response.status.value)
                            assertArrayEquals(original, response.bodyAsBytes())
                        }
                    }
                }
            }
        } finally {
            Prefs.remoteAccess.value = originalRemoteAccess
        }
    }

    private suspend fun withIndexedImages(
        volumes: Set<String> = MediaStore.getExternalVolumeNames(InstrumentationRegistry.getInstrumentation().targetContext),
        checkImage: suspend (String, String, String, Uri) -> Unit,
    ) {
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
                checkImage(id, name, volume, uri)
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    resolver.delete(uri, Bundle().apply {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                    })
                } else {
                    resolver.delete(uri, null, null)
                }
            }
        }
    }
}
