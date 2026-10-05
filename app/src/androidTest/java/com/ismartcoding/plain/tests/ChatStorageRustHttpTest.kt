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
            ChatManager.deleteByIds(emptySet())
            assertNotNull(RustChatStore.getById(one.id))
            ChatManager.clearAllMessages(com.ismartcoding.plain.chat.data.ChatTarget.parseId("peer:${peer.id}"))
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
    @Test
    fun chatDeletionReleasesOwnedAttachmentsThroughRust() = runBlocking {
        val prefix = "synthetic-chat-attachment-${UUID.randomUUID()}"
        val bytes = prefix.toByteArray()
        val store = com.ismartcoding.plain.helpers.AppFileStore
        val file = store.importBytes(bytes, "text/plain")
        val suffix = file.realPath.substringAfterLast('/')
        val content = DChat.parseContent("""{"type":"FILES","value":{"items":[{"uri":"fid:$suffix","fileName":"fixture.txt","size":${bytes.size}}]}}""")
        val direct = DChat(id = "$prefix-direct", fromId = "me", toId = prefix, content = content)
        val group = DChat(id = "$prefix-group", fromId = prefix, toId = "me", channelId = prefix, content = content)
        val path = java.io.File(store.realPathFromId(suffix))
        try {
            store.importBytes(bytes, "text/plain")
            RustChatStore.insert(direct, group)
            assertEquals(2, store.getById(file.id)!!.refCount)
            ChatManager.clearAllMessages(com.ismartcoding.plain.chat.data.ChatTarget.parseId("peer:$prefix"))
            assertNull(RustChatStore.getById(direct.id))
            assertNotNull(RustChatStore.getById(group.id))
            assertEquals(1, store.getById(file.id)!!.refCount)
            assertArrayEquals(bytes, path.readBytes())
            ChatManager.clearAllMessages(com.ismartcoding.plain.chat.data.ChatTarget.parseId("channel:$prefix"))
            assertNull(RustChatStore.getById(group.id))
            assertNull(store.getById(file.id))
            assertFalse(path.exists())
            RustChatStore.deleteByIds(listOf(direct.id, direct.id, group.id))
        } finally {
            RustChatStore.deleteByIds(listOf(direct.id, group.id))
            while (store.getById(file.id) != null) store.release(file.id)
        }
    }

    @Test
    fun peerAndChannelBusinessRemovalIsRustOwnedAndPreservesSharedAttachments() = runBlocking {
        val prefix = "synthetic-chat-entity-${UUID.randomUUID()}"
        val peer = DPeer(id = prefix, name = prefix, status = PeerStatus.PAIRED, key = "", ip = "old", port = 1)
        val channel = DChatChannel(id = "$prefix-channel", name = prefix, ownerId = "$prefix-owner", members = listOf(ChannelMember(peer.id)))
        val store = com.ismartcoding.plain.helpers.AppFileStore
        val file = store.importBytes(prefix.toByteArray(), "text/plain")
        val suffix = file.realPath.substringAfterLast('/')
        val content = DChat.parseContent("""{"type":"FILES","value":{"items":[{"uri":"fid:$suffix","fileName":"fixture.txt","size":${prefix.toByteArray().size}}]}}""")
        val direct = DChat(id = "$prefix-direct", fromId = "me", toId = peer.id, content = content)
        val group = DChat(id = "$prefix-group", fromId = peer.id, toId = "me", channelId = channel.id, content = content)
        try {
            RustPeerStore.insert(peer)
            PeerCacher.load()
            val discovered = com.ismartcoding.plain.chat.peer.PeerManager.applyDeviceDiscovered(peer.id, listOf("127.0.0.1", "::1"), 443, "discovered", DeviceType.PHONE)!!
            assertEquals("127.0.0.1,::1", discovered.ip)
            assertEquals("discovered", RustPeerStore.getById(peer.id)!!.name)
            assertTrue(com.ismartcoding.plain.chat.peer.PeerManager.markUnpaired(peer.id))
            assertNull(com.ismartcoding.plain.chat.peer.PeerManager.applyDeviceDiscovered(peer.id, emptyList(), 1, "stale", DeviceType.PHONE))
            RustChannelStore.insert(channel)
            store.importBytes(prefix.toByteArray(), "text/plain")
            RustChatStore.insert(direct, group)
            assertTrue(com.ismartcoding.plain.chat.peer.PeerManager.deletePeer(peer.id))
            assertEquals(PeerStatus.CHANNEL, RustPeerStore.getById(peer.id)!!.status)
            assertNull(RustChatStore.getById(direct.id))
            assertNotNull(RustChatStore.getById(group.id))
            assertEquals(1, store.getById(file.id)!!.refCount)
            com.ismartcoding.plain.chat.channel.ChannelManager.deleteChannel(channel.id)
            assertNull(RustChannelStore.getById(channel.id))
            assertNull(RustChatStore.getById(group.id))
            assertNull(store.getById(file.id))
            assertTrue(com.ismartcoding.plain.chat.peer.PeerManager.deletePeer(peer.id))
            assertFalse(com.ismartcoding.plain.chat.peer.PeerManager.deletePeer(peer.id))
        } finally {
            RustChatStore.deleteByIds(listOf(direct.id, group.id))
            RustChannelStore.delete(channel.id)
            RustPeerStore.delete(peer.id)
            while (store.getById(file.id) != null) store.release(file.id)
            ChannelCacher.load(); PeerCacher.load(); ChatCacher.load()
        }
    }

    @Test
    fun channelCommandsUseRustKeysMembershipAndOwnerRules() = runBlocking {
        val prefix = "synthetic-channel-state-${UUID.randomUUID()}"
        val actor = com.ismartcoding.plain.TempData.clientId
        var created: DChatChannel? = null
        val peer = DPeer(id = "$prefix-peer", name = prefix, ip = "127.0.0.1", port = 1)
        val foreign = DChatChannel(id = "$prefix-foreign", name = prefix, ownerId = peer.id, members = listOf(ChannelMember(actor, ChannelMemberStatus.PENDING)))
        try {
            val manager = com.ismartcoding.plain.chat.channel.ChannelManager
            val channel = manager.createChannel("  $prefix  ")
            created = channel
            assertEquals(prefix, channel.name)
            assertEquals(actor, channel.ownerId)
            assertEquals(32, android.util.Base64.decode(channel.key, android.util.Base64.DEFAULT).size)
            assertEquals(actor, channel.members.single().peerId)
            val renamed = manager.renameChannel(channel.id, "  $prefix renamed  ")
            assertEquals(2L, renamed.version)
            assertEquals(channel.key, renamed.key)
            val invited = manager.inviteMember(channel.id, peer.id)
            assertEquals(3L, invited.version)
            assertEquals(ChannelMemberStatus.PENDING, invited.members.single { it.peerId == peer.id }.status)
            try { manager.inviteMember(channel.id, peer.id); fail("Duplicate invitation accepted") } catch (_: Exception) { }
            try { manager.leaveChannel(channel.id); fail("Owner was allowed to leave") } catch (_: Exception) { }
            RustPeerStore.insert(peer)
            assertEquals(3L, RustChannelStore.action(channel.id, "resend", peer = peer.id).version)
            RustPeerStore.delete(peer.id)
            val kicked = manager.kickMember(channel.id, peer.id)
            assertEquals(4L, kicked.version)
            assertFalse(kicked.members.any { it.peerId == peer.id })
            RustPeerStore.insert(peer)
            RustChannelStore.insert(foreign)
            assertEquals(foreign.id, RustChannelStore.action(foreign.id, "accept").id)
            RustPeerStore.delete(peer.id)
            manager.leaveChannel(foreign.id)
            val left = RustChannelStore.getById(foreign.id)!!
            assertEquals(ChatChannelStatus.LEFT, left.status)
            assertFalse(left.members.any { it.peerId == actor })
        } finally {
            created?.let { RustChannelStore.remove(it.id) }
            RustChannelStore.remove(foreign.id)
            RustPeerStore.delete(peer.id)
            ChannelCacher.load(); PeerCacher.load(); ChatCacher.load()
        }
    }

}
