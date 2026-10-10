package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.chat.download.DownloadQueue
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.chat.peer.transport.PeerTransportHost
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.DownloadStatus
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DownloadQueueRustHttpTest {
    @Test
    fun uiRequestsUseRustSchedulingAndHostFailureNeverImportsOrCompletes() = runBlocking {
        val id = "synthetic-download-queue-${UUID.randomUUID()}"
        val file = DMessageFile(id = "$id-file", uri = "fsid:$id-source", fileName = "fixture.txt", size = 3)
        val peer = DPeer(id = id, name = id)
        val chat = DChat(id = id, fromId = id, toId = "me", content = DMessageContent(MessageType.FILES, DMessageFiles(listOf(file))))
        suspend fun transport(body: JsonObject) = RustContentApi.postJsonOrThrow("chat/transport", body).getValue("result")
        suspend fun queue(body: JsonObject) = RustContentApi.postJsonOrThrow("chat/download", body).getValue("result")
        suspend fun snapshot() = queue(buildJsonObject { put("action", "snapshot") }).jsonObject.getValue("tasks").jsonArray.firstOrNull { it.jsonObject.getValue("id").jsonPrimitive.content == file.id }?.jsonObject
        try {
            RustPeerStore.insert(peer)
            RustChatStore.insert(chat)
            val capabilities = PeerTransportHost.handle("peerTransportCapabilities", buildJsonObject {}).jsonArray
            for (type in capabilities.filter { it.jsonPrimitive.content != "LAN" }) repeat(2) {
                val ticket = transport(buildJsonObject { put("action", "beginDownload"); put("id", id); put("available", JsonArray(listOf(type))) }).jsonObject.getValue("ticket")
                transport(buildJsonObject {
                    put("action", "finishDownload"); put("ticket", ticket)
                    put("outcome", buildJsonObject { put("kind", "unavailable"); put("error", "synthetic blocked") })
                })
            }
            assertEquals(file.id, DownloadQueue.addDownloadTask(file, peer, id))
            withTimeout(10000) { while (DownloadQueue.downloadProgress.value[file.id]?.status != DownloadStatus.FAILED || DownloadCenter.progress.value[file.id]?.status != DownloadStatus.FAILED) delay(20) }
            assertEquals(DownloadStatus.FAILED, DownloadCenter.progress.value[file.id]?.status)
            val first = snapshot()!!
            assertTrue(first.getValue("error").jsonPrimitive.content.isNotEmpty())
            assertEquals(file.uri, (RustChatStore.getById(id)!!.content.value as DMessageFiles).items.single().uri)
            val oldGeneration = first.getValue("generation")
            assertFalse(File(RustContentApi.directory, "attachment-transfers/${oldGeneration.jsonPrimitive.content}").exists())
            DownloadQueue.retryDownload(file.id)
            withTimeout(10000) {
                while (true) {
                    val current = snapshot()
                    if (current != null && current.getValue("generation") != oldGeneration && current.getValue("status").jsonPrimitive.content == "FAILED") break
                    delay(20)
                }
            }
            assertFalse(queue(buildJsonObject {
                put("action", "finish"); put("id", file.id); put("generation", oldGeneration); put("error", JsonNull)
            }).jsonPrimitive.boolean)
            DownloadQueue.removeDownload(file.id)
            withTimeout(10000) { while (DownloadQueue.downloadProgress.value.containsKey(file.id) || DownloadCenter.progress.value.containsKey(file.id)) delay(20) }
        } finally {
            queue(buildJsonObject { put("action", "control"); put("id", file.id); put("command", "remove") })
            DownloadQueue.refresh()
            RustChatStore.delete(id)
            RustPeerStore.delete(id)
            com.ismartcoding.plain.chat.ChatCacher.load()
            com.ismartcoding.plain.chat.peer.PeerCacher.load()
        }
    }
    @Test
    fun realRustTlsStreamCommitsAndRemovesCompletedUiTask() = runBlocking {
        val id = "synthetic-download-stream-${UUID.randomUUID()}"
        val oldService = com.ismartcoding.plain.preferences.UserPrefs.service.value
        val oldHttp = com.ismartcoding.plain.preferences.UserPrefs.httpPort.value
        val oldHttps = com.ismartcoding.plain.preferences.UserPrefs.httpsPort.value
        val root = File(com.ismartcoding.plain.appContext.cacheDir, id).apply { mkdirs() }
        val seed = id.toByteArray()
        val payload = ByteArray(1024 * 1024 + 17) { (seed[it % seed.size].toInt() xor (it % 251)).toByte() }
        var hash: String? = null
        val fileId = "$id-file"
        try {
            com.ismartcoding.plain.preferences.UserPrefs.service.set(true)
            com.ismartcoding.plain.platform.stopHttpEngineAsync()
            com.ismartcoding.plain.preferences.UserPrefs.httpPort.set(0)
            com.ismartcoding.plain.preferences.UserPrefs.httpsPort.set(0)
            com.ismartcoding.plain.platform.startHttpEngineAsync()
            assertTrue(com.ismartcoding.plain.platform.checkHttpServerAsync())
            val source = File(root, "source.bin").apply { writeBytes(payload) }
            val peer = DPeer(id = id, name = id, ip = "127.0.0.1", port = com.ismartcoding.plain.preferences.UserPrefs.httpsPort.value)
            val file = DMessageFile(id = fileId, uri = "fsid:${com.ismartcoding.plain.helpers.UrlHelper.encrypt(source.absolutePath)}", size = payload.size.toLong(), fileName = "fixture.bin")
            RustPeerStore.insert(peer)
            RustChatStore.insert(DChat(id = id, fromId = id, toId = "me", content = DMessageContent(MessageType.FILES, DMessageFiles(listOf(file)))))
            DownloadQueue.addDownloadTask(file, peer, id)
            val downloaded = withTimeout(15000) {
                while (true) {
                    val current = (RustChatStore.getById(id)!!.content.value as DMessageFiles).items.single()
                    if (current.uri.startsWith("fid:")) return@withTimeout current
                    val task = DownloadQueue.downloadProgress.value[fileId]
                    check(task?.status != DownloadStatus.FAILED) { task?.error ?: "Transfer failed" }
                    delay(20)
                }
                error("Unreachable")
            }
            hash = downloaded.uri.removePrefix("fid:").substringBefore('.')
            val stored = com.ismartcoding.plain.helpers.AppFileStore.getById(hash!!)!!
            assertEquals(1, stored.refCount)
            assertArrayEquals(payload, File(RustContentApi.directory, stored.realPath).readBytes())
            withTimeout(10000) {
                while (DownloadQueue.downloadProgress.value.containsKey(fileId) || DownloadCenter.progress.value.containsKey(fileId)) delay(20)
            }
        } finally {
            RustContentApi.postJsonOrThrow("chat/download", buildJsonObject { put("action", "control"); put("id", fileId); put("command", "remove") })
            DownloadQueue.refresh()
            RustChatStore.delete(id)
            hash?.let { value -> while (com.ismartcoding.plain.helpers.AppFileStore.getById(value) != null) com.ismartcoding.plain.helpers.AppFileStore.release(value) }
            RustPeerStore.delete(id)
            root.deleteRecursively()
            com.ismartcoding.plain.platform.stopHttpEngineAsync()
            com.ismartcoding.plain.preferences.UserPrefs.httpPort.set(oldHttp)
            com.ismartcoding.plain.preferences.UserPrefs.httpsPort.set(oldHttps)
            com.ismartcoding.plain.preferences.UserPrefs.service.set(oldService)
            if (oldService) com.ismartcoding.plain.platform.startHttpEngineAsync()
            com.ismartcoding.plain.chat.ChatCacher.load()
            com.ismartcoding.plain.chat.peer.PeerCacher.load()
        }
    }

}
