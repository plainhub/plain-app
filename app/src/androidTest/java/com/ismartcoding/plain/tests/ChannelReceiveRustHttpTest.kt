package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.channel.*
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelInvite
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelUpdate
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelKick
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.MemberPeerInfo
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.*
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import com.ismartcoding.plain.platform.generateEd25519KeyPair
import com.ismartcoding.plain.platform.signEd25519
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class ChannelReceiveRustHttpTest {
    @Test
    fun signedOwnerMessagesUseRustAndRejectUnsignedOrStaleChanges() = runBlocking {
        val prefix = "synthetic-channel-receive-${UUID.randomUUID()}"
        val actor = TempData.clientId
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val owner = DPeer(id = "$prefix-owner", name = prefix, publicKey = Base64.encode(publicKey), status = PeerStatus.CHANNEL, ip = "127.0.0.1", port = 1)
        val id = "$prefix-channel"
        fun signature(version: Long, action: ChannelSystemMessageAction, target: String) = Base64.encode(signEd25519(privateKey, channelMessagePayload(id, version, action, target).encodeToByteArray()))
        val members = listOf(ChannelMember(owner.id), ChannelMember(actor, ChannelMemberStatus.PENDING))
        val invite = ChannelInvite(id, prefix, Base64.encode(ByteArray(32) { 7 }), owner.id, members, listOf(MemberPeerInfo(id = owner.id, name = prefix, publicKey = owner.publicKey)), 1, signature(1, ChannelSystemMessageAction.INVITE, actor))
        try {
            RustPeerStore.insert(owner)
            assertFalse(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(invite.copy(signature = ""))))
            assertNull(RustChannelStore.getById(id))
            assertTrue(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(invite)))
            val first = RustChannelStore.getById(id)!!
            assertEquals(prefix, first.name)
            assertEquals(1L, first.version)
            assertEquals(ChannelMemberStatus.PENDING, first.members.single { it.peerId == actor }.status)
            val update = ChannelUpdate(id, "$prefix updated", members, version = 2, signature = signature(2, ChannelSystemMessageAction.UPDATE, ""))
            assertFalse(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.UPDATE, jsonEncode(update.copy(signature = ""))))
            assertEquals(prefix, RustChannelStore.getById(id)!!.name)
            assertTrue(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.UPDATE, jsonEncode(update)))
            assertEquals(update.channelName, RustChannelStore.getById(id)!!.name)
            val stale = ChannelKick(id, 1, signature(1, ChannelSystemMessageAction.KICK, actor))
            assertTrue(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.KICK, jsonEncode(stale)))
            assertEquals(ChatChannelStatus.JOINED, RustChannelStore.getById(id)!!.status)
            val kick = ChannelKick(id, 2, signature(2, ChannelSystemMessageAction.KICK, ""))
            assertTrue(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.KICK, jsonEncode(kick)))
            assertEquals(ChatChannelStatus.KICKED, RustChannelStore.getById(id)!!.status)
            assertFalse(RustChannelStore.getById(id)!!.members.any { it.peerId == actor })
            assertFalse(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(invite)))
            val reinvite = invite.copy(version = 3, signature = signature(3, ChannelSystemMessageAction.INVITE, actor))
            assertTrue(ChannelSystemMessageReceiver.handle(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(reinvite)))
            assertEquals(first.createdAt, RustChannelStore.getById(id)!!.createdAt)
            assertEquals(ChatChannelStatus.JOINED, RustChannelStore.getById(id)!!.status)
            assertEquals(owner.publicKey, RustPeerStore.getById(owner.id)!!.publicKey)
        } finally {
            RustChannelStore.delete(id)
            RustPeerStore.delete(owner.id)
            ChannelCacher.load()
            PeerCacher.load()
        }
    }
}
