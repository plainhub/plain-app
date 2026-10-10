package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.platform.streamZipToSink
import com.ismartcoding.plain.platform.StreamSink
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class ShareRustHttpTest {
    @Test
    fun sharesResolveFreshRustRootsAndRevokeOldTokens() = runBlocking {
        val marker = "share-rust-${UUID.randomUUID()}"
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, marker).apply { mkdirs() }
        val folder = File(root,"photos").apply { mkdirs() }
        val other = File(root,"other").apply { mkdirs() }
        File(folder,"a.txt").writeText("synthetic")
        File(folder,"empty").mkdir()
        val outside = File(other,"secret.txt").apply { writeText("outside") }
        android.system.Os.symlink(outside.absolutePath, File(folder,"escape.txt").absolutePath)
        val service = UserPrefs.service.value
        var id: String? = null
        try {
            UserPrefs.service.set(true)
            val share = ShareManager.createShare(marker,listOf(folder.absolutePath),true,null)
            id = share.id
            assertTrue(ShareManager.buildLink(share, "127.0.0.1").contains("/s/${share.id}#"))
            assertNotNull(ShareManager.loadAuth(share.id))
            val encrypted = UrlHelper.encrypt("{\"sharedId\":\"${share.id}\",\"virtualPath\":\"photos/a.txt\"}",Base64.decode(share.urlToken))
            assertEquals(File(folder,"a.txt").canonicalPath,ShareManager.resolveSharedPath(share.id,encrypted))
            assertEquals("photos/a.txt",ShareManager.browse(share.id,"photos").second.filter { !it.isDir }.single().virtualPath)
            val folderId = UrlHelper.encrypt("{\"sharedId\":\"${share.id}\",\"virtualPath\":\"photos\"}",Base64.decode(share.urlToken))
            val entries = ShareManager.zipEntries(share.id, folderId)
            assertEquals(listOf("a.txt", "empty/"), entries.map { it.entryName })
            val output = java.io.ByteArrayOutputStream()
            val sink = object : StreamSink {
                override suspend fun write(bytes: ByteArray) { output.write(bytes) }
                override suspend fun write(bytes: ByteArray, offset: Int, length: Int) { output.write(bytes, offset, length) }
                override suspend fun flush() {}
                override suspend fun close() {}
            }
            assertTrue(streamZipToSink(entries, sink, recursive = false))
            val names = mutableListOf<String>()
            java.util.zip.ZipInputStream(output.toByteArray().inputStream()).use { zip ->
                while (true) { val entry = zip.nextEntry ?: break; names += entry.name; zip.closeEntry() }
            }
            assertEquals(listOf("a.txt", "empty/"), names)
            assertNull(ShareManager.resolveVirtualPath(share,"photos/../other"))
            UserPrefs.service.set(false)
            assertNull(ShareManager.loadAuth(share.id))
            assertNull(ShareManager.resolveSharedPath(share.id,encrypted))
            UserPrefs.service.set(true)
            ShareManager.updateShare(share.id,marker,null,listOf(other.absolutePath))
            assertNull(ShareManager.resolveSharedPath(share.id,encrypted))
            ShareManager.updateShare(share.id,marker,Instant.parse("2000-01-01T00:00:00Z"))
            assertNull(ShareManager.loadAuth(share.id))
            ShareManager.updateShare(share.id,marker,null)
            assertNotNull(ShareManager.loadAuth(share.id))
            ShareManager.deleteShare(share.id)
            assertNull(ShareManager.loadAuth(share.id))
            assertNull(ShareManager.getShare(share.id))
        } finally {
            id?.let { ShareManager.deleteShare(it) }
            UserPrefs.service.set(service)
            root.deleteRecursively()
        }
    }
}
