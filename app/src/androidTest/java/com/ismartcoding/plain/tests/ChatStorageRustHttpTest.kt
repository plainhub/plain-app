package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.*
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.channel.RustChannelStore
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.db.*
import com.ismartcoding.plain.discover.NearbyDeviceCache
import com.ismartcoding.plain.enums.*
import com.ismartcoding.plain.httpserver.mainschemas.chatItems
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class ChatStorageRustHttpTest {
    @Test
    fun sharedChatPeerChannelAndNearbyCallsUseRustWithLiteralSearchAndIsolatedDeletion() = runBlocking {
        val prefix = "synthetic-chat-${UUID.randomUUID()}"
        val peer = DPeer(id = "$prefix-peer", name = prefix, ip = "127.0.0.1", port = 1)
        val channel = DChatChannel(id = "$prefix-channel", name = prefix, ownerId = "me", members = listOf(ChannelMember(peer.id)), version = 5_000_000_001)
        val one = DChat(id = "$prefix-one", fromId = "me", toId = peer.id, status = ChatStatus.SENT, content = DMessageContent(MessageType.TEXT, DMessageText("$prefix literal %_ 中文")), createdAt = Instant.parse("2026-10-04T00:00:00Z"))
        val two = DChat(id = "$prefix-two", fromId = peer.id, toId = "me", status = ChatStatus.PENDING, content = DMessageContent(MessageType.TEXT, DMessageText("$prefix second")), createdAt = Instant.parse("2026-10-04T00:00:01Z"))
        val group = DChat(id = "$prefix-group", fromId = peer.id, toId = "me", channelId = channel.id, status = ChatStatus.SENT, content = DMessageContent(MessageType.TEXT, DMessageText("$prefix group")))
        val nearby = DNearbyDevice(id = "$prefix-nearby", name = prefix, ips = listOf("127.0.0.1", "::1"), port = 1, deviceType = DeviceType.PHONE, version = "test", platform = "Android", lastSeen = Instant.parse("2026-10-04T00:00:02Z"))
        try {
            RustPeerStore.insert(peer)
            assertEquals(peer.name, RustPeerStore.getById(peer.id)!!.name)
            assertTrue(RustPeerStore.getByIds(emptyList()).isEmpty())
            peer.name = "$prefix renamed"
            RustPeerStore.update(peer)
            assertEquals(peer.name, RustPeerStore.getByIds(listOf(peer.id)).single().name)
            PeerCacher.load()
            PeerCacher.mutatePeer(peer.id) { it.name = "$prefix patched" }
            assertEquals("$prefix patched", RustPeerStore.getById(peer.id)!!.name)
            RustChannelStore.insert(channel)
            assertEquals(channel.version, RustChannelStore.getById(channel.id)!!.version)
            assertEquals(peer.id, RustChannelStore.getById(channel.id)!!.members.single().peerId)
            ChannelCacher.load()
            ChannelCacher.mutateChannel(channel.id) { it.version += 1; it.name = "$prefix patched" }
            assertEquals(channel.version + 1, RustChannelStore.getById(channel.id)!!.version)
            RustChatStore.insert(one, two, group)
            assertEquals(listOf(one.id, two.id), RustChatStore.getByPeerId(peer.id).map { it.id })
            assertEquals(two.id, RustChatStore.getByPeerIdPage(peer.id, 1, 0).single().id)
            assertEquals(1, ChatDbHelper.countAsync("$prefix literal %_"))
            assertEquals(one.id, ChatDbHelper.searchAsync("$prefix literal %_", 10, 0).single().id)
            assertEquals(one.id, chatItems(peer.id, 0, 10, "text:\"$prefix literal %_\"").single().id.value)
            assertEquals(setOf(one.id, two.id), ChatDbHelper.getIdsAsync("peer:${peer.id}"))
            assertEquals(setOf(group.id), ChatDbHelper.getIdsAsync("channel:${channel.id}"))
            RustChatStore.updateStatusAndData(two.id, ChatStatus.PARTIAL, "{\"results\":[]}")
            assertEquals(ChatStatus.PARTIAL, RustChatStore.getById(two.id)!!.status)
            RustChatStore.updateData(ChatItemDataUpdate(two.id, DMessageContent(MessageType.TEXT, DMessageText("changed"))))
            assertEquals("changed", (RustChatStore.getById(two.id)!!.content.value as DMessageText).text)
            NearbyDeviceCache.upsertAsync(nearby)
            NearbyDeviceCache.upsertAsync(nearby.copy(name = "stale", lastSeen = Instant.parse("2026-10-04T00:00:01Z")))
            val saved = NearbyDeviceCache.getAllAsync().single { it.id == nearby.id }
            assertEquals(nearby.name, saved.name)
            assertEquals(nearby.ips, saved.ips)
            NearbyDeviceCache.touchAsync(nearby.id)
            ChatDbHelper.deleteByIdsAsync(emptySet())
            assertNotNull(RustChatStore.getById(one.id))
            ChatDbHelper.deleteAllChatsAsync(peer.id)
            assertNull(RustChatStore.getById(one.id))
            assertNull(RustChatStore.getById(two.id))
            assertNotNull(RustChatStore.getById(group.id))
        } finally {
            RustChatStore.deleteByIds(listOf(one.id, two.id, group.id))
            RustChannelStore.delete(channel.id)
            RustPeerStore.delete(peer.id)
            NearbyDeviceCache.removeAsync(nearby.id)
            PeerCacher.load()
            ChannelCacher.load()
            ChatCacher.load()
        }
    }
}
