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
import kotlinx.serialization.json.*
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
            assertFalse(receive(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(invite.copy(signature = ""))))
            assertNull(RustChannelStore.getById(id))
            assertTrue(receive(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(invite)))
            val first = RustChannelStore.getById(id)!!
            assertEquals(prefix, first.name)
            assertEquals(1L, first.version)
            assertEquals(ChannelMemberStatus.PENDING, first.members.single { it.peerId == actor }.status)
            val update = ChannelUpdate(id, "$prefix updated", members, version = 2, signature = signature(2, ChannelSystemMessageAction.UPDATE, ""))
            assertFalse(receive(owner.id, ChannelSystemMessageType.UPDATE, jsonEncode(update.copy(signature = ""))))
            assertEquals(prefix, RustChannelStore.getById(id)!!.name)
            assertTrue(receive(owner.id, ChannelSystemMessageType.UPDATE, jsonEncode(update)))
            assertEquals(update.channelName, RustChannelStore.getById(id)!!.name)
            val stale = ChannelKick(id, 1, signature(1, ChannelSystemMessageAction.KICK, actor))
            assertTrue(receive(owner.id, ChannelSystemMessageType.KICK, jsonEncode(stale)))
            assertEquals(ChatChannelStatus.JOINED, RustChannelStore.getById(id)!!.status)
            val kick = ChannelKick(id, 2, signature(2, ChannelSystemMessageAction.KICK, ""))
            assertTrue(receive(owner.id, ChannelSystemMessageType.KICK, jsonEncode(kick)))
            assertEquals(ChatChannelStatus.KICKED, RustChannelStore.getById(id)!!.status)
            assertFalse(RustChannelStore.getById(id)!!.members.any { it.peerId == actor })
            assertFalse(receive(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(invite)))
            val reinvite = invite.copy(version = 3, signature = signature(3, ChannelSystemMessageAction.INVITE, actor))
            assertTrue(receive(owner.id, ChannelSystemMessageType.INVITE, jsonEncode(reinvite)))
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
    @Test
    fun outboundPreparationUsesRustSignaturesAndLatestTransportKeys() = runBlocking {
        val prefix = "synthetic-channel-send-${UUID.randomUUID()}"
        val actor = TempData.clientId
        val paired = DPeer(id = "$prefix-paired", name = prefix, key = Base64.encode(ByteArray(32) { 8 }), status = PeerStatus.PAIRED, ip = "127.0.0.1", port = 1)
        val group = DPeer(id = "$prefix-group", name = prefix, status = PeerStatus.CHANNEL, ip = "127.0.0.1", port = 1)
        val publicKey = Base64.decode(com.ismartcoding.plain.helpers.SignatureHelper.getRawPublicKeyBase64Async())
        val channel = RustChannelStore.create("中文 $prefix")
        try {
            com.ismartcoding.plain.api.RustContentApi.postJsonOrThrow("chat/channel", buildJsonObject {
                put("action", "send"); put("id", channel.id)
                put("message_type", ChannelSystemMessageType.UPDATE.name); put("target", "")
            }, longRunning = true)
            RustPeerStore.insert(paired, group)
            RustChannelStore.action(channel.id, "invite", peer = paired.id)
            val current = RustChannelStore.action(channel.id, "invite", peer = group.id)
            val prepared = RustChannelOutgoingStore.prepare(current, ChannelSystemMessageType.INVITE, paired.id)
            val pieces = RustChannelOutgoingStore.wire(prepared).split('|', limit = 3)
            assertTrue(com.ismartcoding.plain.platform.verifyEd25519(publicKey, (pieces[1] + pieces[2]).encodeToByteArray(), Base64.decode(pieces[0])))
            val wire = Json.parseToJsonElement(pieces[2]).jsonObject
            val payload = Json.parseToJsonElement(wire.getValue("variables").jsonObject.getValue("payload").jsonPrimitive.content).jsonObject
            assertEquals(actor, payload.getValue("owner").jsonPrimitive.content)
            assertTrue(com.ismartcoding.plain.platform.verifyEd25519(publicKey, channelMessagePayload(current.id, current.version, ChannelSystemMessageAction.INVITE, paired.id).encodeToByteArray(), Base64.decode(payload.getValue("signature").jsonPrimitive.content)))
            val target = prepared.getValue("targets").jsonArray.single().jsonObject
            assertEquals(paired.key, target.getValue("key").jsonPrimitive.content)
            assertEquals("", target.getValue("channelId").jsonPrimitive.content)
            paired.key = Base64.encode(ByteArray(32) { 9 })
            RustPeerStore.update(paired)
            val broadcast = RustChannelOutgoingStore.prepare(current, ChannelSystemMessageType.UPDATE)
            val targets = broadcast.getValue("targets").jsonArray.map { it.jsonObject }.associateBy { it.getValue("peer").jsonObject.getValue("id").jsonPrimitive.content }
            assertEquals(paired.key, targets.getValue(paired.id).getValue("key").jsonPrimitive.content)
            assertEquals(current.key, targets.getValue(group.id).getValue("key").jsonPrimitive.content)
            assertEquals(current.id, targets.getValue(group.id).getValue("channelId").jsonPrimitive.content)
            val removed = RustChannelStore.remove(current.id)!!
            assertEquals(2, RustChannelOutgoingStore.prepare(removed, ChannelSystemMessageType.KICK).getValue("targets").jsonArray.size)
        } finally {
            RustChannelStore.remove(channel.id)
            RustPeerStore.deleteByIds(listOf(paired.id, group.id))
            ChannelCacher.load()
            PeerCacher.load()
        }
    }

    private suspend fun receive(from: String, type: ChannelSystemMessageType, payload: String): Boolean {
        val result = com.ismartcoding.plain.api.RustContentApi.postJson("chat/store", buildJsonObject {
            put("action", "receiveChannel"); put("actor", TempData.clientId)
            put("from_id", from); put("message_type", type.name); put("payload", payload)
        }).getOrNull()?.get("result")?.jsonObject ?: return false
        ChannelCacher.load()
        return result.getValue("accepted").jsonPrimitive.boolean
    }

}
