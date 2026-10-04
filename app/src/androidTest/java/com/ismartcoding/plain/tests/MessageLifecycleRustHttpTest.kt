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
}
