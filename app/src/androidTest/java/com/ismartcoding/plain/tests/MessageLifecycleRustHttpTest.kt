package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.enums.ChatStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class MessageLifecycleRustHttpTest {
    @Test
    fun rustCreatesReceivesDeduplicatesAndMergesDeliveryFromCurrentStoredState() = runBlocking {
        val id = "synthetic-message-lifecycle-${UUID.randomUUID()}"
        val chats = mutableListOf<String>()
        val content = DMessageContent(MessageType.TEXT, DMessageText("fixture ' 中文"))
        try {
            RustPeerStore.insert(DPeer(id = id, name = id))
            val local = RustChatStore.create("local", "", content)
            chats += local.id
            assertEquals("me", local.fromId)
            assertEquals(ChatStatus.SENT, local.status)
            val remote = RustChatStore.create(id, "", content)
            chats += remote.id
            assertEquals(ChatStatus.PENDING, remote.status)
            val partial = requireNotNull(RustChatStore.delivery(remote.id, listOf(DMessageDeliveryResult(id, id), DMessageDeliveryResult("second", "second", "offline"))))
            assertEquals(ChatStatus.PARTIAL, partial.status)
            val retry = requireNotNull(RustChatStore.delivery(remote.id, listOf(DMessageDeliveryResult("second", "second")), retry = true))
            assertEquals(ChatStatus.SENT, retry.status)
            assertEquals(2, retry.parseStatusData()!!.results.size)
            assertEquals(remote.createdAt, retry.createdAt)
            assertEquals(remote.content.toJSONString(), retry.content.toJSONString())
            val signature = Base64.encode(ByteArray(64) { 7 })
            val timestamp = System.currentTimeMillis()
            val received = requireNotNull(RustChatStore.receive(id, "", content.toJSONString(), signature, timestamp))
            chats += received.chat.id
            assertEquals(id, received.peer.id)
            assertEquals("me", received.chat.toId)
            assertEquals(ChatStatus.SENT, received.chat.status)
            assertNull(RustChatStore.receive(id, "", content.toJSONString(), signature, timestamp))
            RustChatStore.delete(received.chat.id)
            assertNull(RustChatStore.receive(id, "", content.toJSONString(), signature, timestamp))
            RustChatStore.delete(remote.id)
            assertNull(RustChatStore.delivery(remote.id, emptyList()))
        } finally {
            RustChatStore.deleteByIds(chats)
            RustPeerStore.delete(id)
            com.ismartcoding.plain.chat.peer.PeerCacher.load()
            com.ismartcoding.plain.chat.ChatCacher.load()
        }
    }
    @Test
    fun rustFileCommandsBindSharedOwnershipAndIgnoreTheCurrentUiTarget() = runBlocking {
        val manager = com.ismartcoding.plain.chat.ChatManager
        val local = com.ismartcoding.plain.chat.data.ChatTarget.parseId("peer:local")
        val wrongTarget = com.ismartcoding.plain.chat.data.ChatTarget.parseId("peer:synthetic-other")
        val bytes = "synthetic-${UUID.randomUUID()}".toByteArray()
        val owned = com.ismartcoding.plain.helpers.AppFileStore.importBytes(bytes, "text/plain")
        val file = DMessageFile(id = owned.id, uri = "content://synthetic", fileName = "fixture.txt", size = 0)
        val chats = mutableListOf<String>()
        try {
            val first = manager.insertFilesImmediate(local, listOf(file), false).also { chats += it.id }
            val second = manager.insertFilesImmediate(local, listOf(file), false).also { chats += it.id }
            val imported = file.copy(uri = com.ismartcoding.plain.helpers.AppFileStore.toFidUri(owned))
            val json = kotlinx.serialization.json.Json { encodeDefaults = true }
            val response = com.ismartcoding.plain.api.RustContentApi.postJsonOrThrow("chat/service", kotlinx.serialization.json.buildJsonObject {
                put("action", kotlinx.serialization.json.JsonPrimitive("replaceFilesMany"))
                put("ids", kotlinx.serialization.json.JsonArray(chats.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                put("items", json.parseToJsonElement(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(DMessageFile.serializer()), listOf(imported))))
            })
            assertTrue(response.containsKey("result"))
            assertEquals(2, com.ismartcoding.plain.helpers.AppFileStore.getById(owned.id)!!.refCount)
            val saved = manager.updateFilesMessage(first.id, listOf(imported), wrongTarget, emptySet())!!
            assertEquals(ChatStatus.SENT, saved.status)
            assertEquals(bytes.size.toLong(), (saved.content.value as DMessageFiles).items.single().size)
            RustChatStore.delete(first.id)
            assertEquals(1, com.ismartcoding.plain.helpers.AppFileStore.getById(owned.id)!!.refCount)
            assertTrue(java.io.File(com.ismartcoding.plain.helpers.AppFileStore.resolveUri(imported.uri)).exists())
            RustChatStore.delete(second.id)
            assertNull(com.ismartcoding.plain.helpers.AppFileStore.getById(owned.id))
        } finally {
            RustChatStore.deleteByIds(chats)
            while (com.ismartcoding.plain.helpers.AppFileStore.getById(owned.id) != null) com.ismartcoding.plain.helpers.AppFileStore.release(owned.id)
            com.ismartcoding.plain.chat.ChatCacher.load()
        }
    }

}
