package com.ismartcoding.plain.tests

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.features.share.*
import com.ismartcoding.plain.platform.startHttpEngineAsync
import com.ismartcoding.plain.platform.stopHttpEngineAsync
import com.ismartcoding.plain.preferences.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class SharedClientRustHttpTest {
    @Test
    fun rootBrowsesGuestTlsUpdatesCurrentCardAndStreamsEncodedFiles() = runBlocking {
        RustContentApi.start()
        val marker = "synthetic-shared-client-${UUID.randomUUID()}"
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, marker).apply { mkdirs() }
        val file = File(folder, "a +?#中文%.txt").apply { writeText("synthetic download") }
        val service = UserPrefs.service.value
        val http = UserPrefs.httpPort.value
        val https = UserPrefs.httpsPort.value
        var shareId: String? = null
        val messageId = "$marker-message"
        try {
            UserPrefs.service.value = true
            stopHttpEngineAsync()
            UserPrefs.httpPort.value = 0
            UserPrefs.httpsPort.value = 0
            assertTrue(startHttpEngineAsync())
            val share = ShareManager.createShare(marker, listOf(folder.absolutePath), ShareCrypto.newUrlToken(), true, null)
            shareId = share.id
            val old = DMessageShare(shareId = share.id, urlToken = ShareManager.sharedToken(share.id), peerInfo = DSharePeerInfo(SystemPrefs.clientId.value, "bad,127.0.0.1", 1), name = "old", itemCount = 1, totalSize = file.length(), expiresAt = Instant.parse("2030-01-01T00:00:00Z"))
            RustChatStore.insert(DChat(id = messageId, fromId = "me", toId = "local", content = DMessageContent(MessageType.SHARE, old)))
            val initial = SharedLinkClient.initialLink(messageId, old)
            assertEquals("127.0.0.1", initial.host)
            assertEquals(UserPrefs.httpsPort.value, initial.port)
            assertTrue(SharedLinkClient.pageUrl(initial).contains("/s/${share.id}#"))
            val result = SharedLinkClient.browse(messageId, old, folder.name)!!
            assertEquals(marker, result.info.name)
            assertEquals(marker, result.card.name)
            assertNull(result.card.expiresAt)
            assertEquals(initial.port, result.card.peerInfo.port)
            val saved = RustChatStore.getById(messageId)!!.content.value as DMessageShare
            assertEquals(marker, saved.name)
            assertNull(saved.expiresAt)
            val entry = result.info.entries.single()
            assertEquals(file.name, entry.name)
            val url = SharedLinkClient.fileUrl(result.link, result.info.urlToken, entry.virtualPath)
            val parsed = Uri.parse(url)
            assertEquals(share.id, parsed.getQueryParameter("sid"))
            assertEquals(file.canonicalPath, ShareManager.resolveSharedPath(share.id, parsed.getQueryParameter("id")!!))
            val output = ByteArrayOutputStream()
            var finalBytes = -1L
            SharedLinkClient.downloadTo(url, { bytes, length -> output.write(bytes, 0, length) }) { done, _ -> finalBytes = done }
            assertArrayEquals(file.readBytes(), output.toByteArray())
            assertEquals(file.length(), finalBytes)
            val direct = SharedLinkClient.fetchSharedInfo(result.link, folder.name)
            assertEquals(entry.virtualPath, direct.entries.single().virtualPath)
            val own = ShareManager.buildLink(share, "::1")
            assertTrue(own.startsWith("https://[::1]:${initial.port}/s/"))
            var rejected = false
            try { SharedLinkClient.browse(messageId, old, folder.name) } catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
        } finally {
            stopHttpEngineAsync()
            RustChatStore.delete(messageId)
            shareId?.let { ShareManager.deleteShare(it) }
            UserPrefs.httpPort.value = http
            UserPrefs.httpsPort.value = https
            UserPrefs.service.value = service
            folder.deleteRecursively()
        }
    }
}
