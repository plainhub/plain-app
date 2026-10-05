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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.DownloadStatus
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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = "synthetic-shared-client-${UUID.randomUUID()}"
        val folder = File(context.cacheDir, marker).apply { mkdirs() }
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
            val nested = File(folder, "子目录").apply { mkdirs() }
            val nestedFile = File(nested, "nested.txt").apply { writeText("synthetic nested") }
            val roots = SharedLinkClient.fetchSharedInfo(result.link, null).entries
            val rootEntry = roots.single { it.name == marker }
            val syncPlan = SharedLinkClient.plan(ShareBatchType.SYNC, result.link, listOf(rootEntry), "${folder.absolutePath}/output", "/downloads/PlainApp")
            assertEquals(2, syncPlan.totalFiles)
            assertEquals(file.length() + nestedFile.length(), syncPlan.totalSize)
            val nestedTarget = syncPlan.targets.single { it.entry.name == nestedFile.name }
            assertEquals("${folder.absolutePath}/output/$marker/子目录", nestedTarget.writeDir)
            assertEquals("$marker/子目录/nested.txt", nestedTarget.entryName)
            assertFalse(nestedTarget.storeToDownloads)
            val publicPlan = SharedLinkClient.plan(ShareBatchType.MULTI, result.link, listOf(entry), "", "/downloads/PlainApp")
            assertTrue(publicPlan.targets.single().storeToDownloads)
            val zipPlan = SharedLinkClient.plan(ShareBatchType.ZIP, result.link, listOf(rootEntry), "", "/downloads/PlainApp")
            assertEquals(syncPlan.targets.map { it.entryName }, zipPlan.targets.map { it.entryName })
            val archive = ByteArrayOutputStream()
            SharedLinkClient.packZip(listOf(
                com.ismartcoding.plain.platform.ZipStreamEntry(file.absolutePath, "$marker/${file.name}"),
                com.ismartcoding.plain.platform.ZipStreamEntry(nestedFile.absolutePath, nestedTarget.entryName),
            )) { bytes, length -> archive.write(bytes, 0, length) }
            val extracted = mutableMapOf<String, String>()
            java.util.zip.ZipInputStream(archive.toByteArray().inputStream()).use { zip ->
                while (true) {
                    val item = zip.nextEntry ?: break
                    extracted[item.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
            }
            assertEquals("synthetic download", extracted["$marker/${file.name}"])
            assertEquals("synthetic nested", extracted[nestedTarget.entryName])
            val batchOutput = File(context.cacheDir, "$marker-output").apply { mkdirs() }
            try {
                SharedFolderDownloadEngine.enqueueDirSync(messageId, result.link, result.info.urlToken, rootEntry, batchOutput.absolutePath)
                val sync = awaitBatch(messageId, ShareBatchType.SYNC)
                assertEquals("COMPLETED", sync.getValue("status").jsonPrimitive.content)
                assertEquals(2, sync.getValue("doneFiles").jsonPrimitive.int)
                assertArrayEquals(file.readBytes(), File(batchOutput, "$marker/${file.name}").readBytes())
                assertArrayEquals(nestedFile.readBytes(), File(batchOutput, "$marker/子目录/nested.txt").readBytes())
                SharedFolderDownloadEngine.refresh()
                assertEquals(DownloadStatus.COMPLETED, DownloadCenter.get(sync.getValue("id").jsonPrimitive.content)!!.status)
                val batchJson = Json { encodeDefaults = true }
                RustContentApi.postJson("shares/batch", buildJsonObject {
                    put("action", "enqueue"); put("intent", buildJsonObject {
                        put("message_id", messageId); put("kind", "ZIP"); put("link", batchJson.encodeToJsonElement(result.link))
                        put("url_token", result.info.urlToken); put("entries", batchJson.encodeToJsonElement(listOf(rootEntry)))
                        put("target_dir", ""); put("downloads_base", batchOutput.absolutePath); put("zip_name", "archive.zip")
                    })
                })
                val zipBatch = awaitBatch(messageId, ShareBatchType.ZIP)
                assertEquals("COMPLETED", zipBatch.getValue("status").jsonPrimitive.content)
                assertEquals(2, zipBatch.getValue("doneFiles").jsonPrimitive.int)
                val zipContents = mutableMapOf<String, ByteArray>()
                java.util.zip.ZipInputStream(File(batchOutput, "archive.zip").inputStream()).use { zip ->
                    while (true) { val item = zip.nextEntry ?: break; zipContents[item.name] = zip.readBytes() }
                }
                assertArrayEquals(file.readBytes(), zipContents["$marker/${file.name}"])
                assertArrayEquals(nestedFile.readBytes(), zipContents["$marker/子目录/nested.txt"])
                val blocked = File(batchOutput, "blocked").apply { writeText("blocked") }
                SharedFolderDownloadEngine.enqueueFile(messageId, result.link, result.info.urlToken, entry, blocked.absolutePath)
                val failed = awaitBatch(messageId, ShareBatchType.FILE)
                assertEquals("FAILED", failed.getValue("status").jsonPrimitive.content)
                assertEquals(0, failed.getValue("doneFiles").jsonPrimitive.int)
                assertTrue(blocked.delete()); assertTrue(blocked.mkdirs())
                SharedFolderDownloadEngine.retryFailed(failed.getValue("id").jsonPrimitive.content)
                val retried = awaitBatch(messageId, ShareBatchType.FILE, "COMPLETED")
                assertEquals(1, retried.getValue("doneFiles").jsonPrimitive.int)
                assertArrayEquals(file.readBytes(), File(blocked, file.name).readBytes())
            } finally {
                val tasks = RustContentApi.postJson("shares/batch", buildJsonObject { put("action", "snapshot") }).getValue("result").jsonObject.getValue("tasks").jsonArray
                tasks.filter { it.jsonObject.getValue("messageId").jsonPrimitive.content == messageId }.forEach { task ->
                    RustContentApi.postJson("shares/batch", buildJsonObject { put("action", "control"); put("id", task.jsonObject.getValue("id")); put("command", "remove") })
                }
                batchOutput.deleteRecursively()
            }
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
    private suspend fun awaitBatch(messageId: String, kind: ShareBatchType, expected: String? = null): JsonObject = withTimeout(30_000) {
        while (true) {
            val tasks = RustContentApi.postJson("shares/batch", buildJsonObject { put("action", "snapshot") }).getValue("result").jsonObject.getValue("tasks").jsonArray
            val row = tasks.map { it.jsonObject }.firstOrNull { it.getValue("messageId").jsonPrimitive.content == messageId && it.getValue("type").jsonPrimitive.content == kind.name }
            if (row != null) {
                val status = row.getValue("status").jsonPrimitive.content
                if (if (expected != null) status == expected else status in listOf("COMPLETED", "PARTIAL", "FAILED", "CANCELED")) return@withTimeout row
            }
            delay(50)
        }
        error("Unreachable")
    }

}
