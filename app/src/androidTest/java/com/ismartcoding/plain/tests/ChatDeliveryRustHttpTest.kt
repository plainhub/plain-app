package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.ChatSender
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.chat.channel.RustChannelStore
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.enums.ChatStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChatDeliveryRustHttpTest {
    @Test
    fun sendsUsePersistedTargetsAndChannelMembershipInsteadOfCallerSnapshots() = runBlocking {
        val id = "synthetic-chat-delivery-${UUID.randomUUID()}"
        val chats = mutableListOf<String>()
        val channel = RustChannelStore.create(id)
        try {
            val content = DMessageContent(MessageType.TEXT, DMessageText("fixture"))
            RustPeerStore.insert(DPeer(id = id, name = id))
            val local = RustChatStore.create("local", "", content)
            chats += local.id
            local.toId = id
            local.status = ChatStatus.FAILED
            ChatSender.send(local)
            assertEquals(ChatStatus.SENT, local.status)
            assertEquals("local", local.toId)
            val peer = RustChatStore.create(id, "", content)
            chats += peer.id
            peer.toId = "local"
            ChatSender.send(peer)
            assertEquals(ChatStatus.FAILED, peer.status)
            assertEquals(id, peer.toId)
            assertEquals("peer unpaired", peer.parseStatusData()!!.results.single().error)
            val group = RustChatStore.create("", channel.id, content)
            chats += group.id
            ChatSender.send(group)
            assertEquals(ChatStatus.SENT, group.status)
            RustChannelStore.action(channel.id, "invite", peer = id)
            var rejected = false
            try { ChatSender.sendToChannelMembers(group, listOf(id)) } catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
            assertEquals(ChatStatus.SENT, RustChatStore.getById(group.id)!!.status)
        } finally {
            RustChatStore.deleteByIds(chats)
            RustChannelStore.remove(channel.id)
            RustPeerStore.delete(id)
            com.ismartcoding.plain.chat.ChatCacher.load()
            com.ismartcoding.plain.chat.peer.PeerCacher.load()
            com.ismartcoding.plain.chat.channel.ChannelCacher.load()
        }
    }
}
