package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.server.HttpServiceHandler
import com.ismartcoding.plain.chat.RustChatStore
import com.ismartcoding.plain.chat.channel.*
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelInvite
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.MemberPeerInfo
import com.ismartcoding.plain.chat.peer.*
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.*
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PeerGraphQLRustHttpTest {
    @Test
    fun encryptedTlsAndBleEntryShareRustExecutionAndReplayReceipt() = runBlocking {
        val id = "synthetic-peer-graphql-${UUID.randomUUID()}"
        val key = ByteArray(32) { 7 }
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val peer = DPeer(id = id, name = id, publicKey = Base64.encode(publicKey), key = Base64.encode(key), status = PeerStatus.PAIRED)
        val service = UserPrefs.service.value
        val http = UserPrefs.httpPort.value
        val https = UserPrefs.httpsPort.value
        val active = TempData.activeToId
        val client = createPeerStatusHttpClient()
        suspend fun wire(document: JsonObject): ByteArray {
            val content = document.toString()
            val timestamp = System.currentTimeMillis()
            val signature = Base64.encode(signEd25519(privateKey, "$timestamp$content".encodeToByteArray()))
            return RustPeerWireStore.encrypt(key, "$signature|$timestamp|$content")
        }
        fun decode(body: ByteArray): JsonObject = Json.parseToJsonElement(chaCha20Decrypt(key, body)!!.decodeToString()).jsonObject
        suspend fun ble(body: ByteArray): Pair<Int, ByteArray> = BleBinaryFixture.response(HttpServiceHandler().handleRequest(BleBinaryFixture.peer(id, body), "synthetic-mac"))
        try {
            TempData.activeToId = "peer:$id"
            RustPeerStore.insert(peer)
            UserPrefs.service.value = true
            stopHttpEngineAsync()
            UserPrefs.httpPort.value = 0
            UserPrefs.httpsPort.value = 0
            assertTrue(startHttpEngineAsync())
            val url = "https://127.0.0.1:${UserPrefs.httpsPort.value}/peer_graphql"
            val document = buildJsonObject {
                put("query", "mutation Incoming(\$content:String!) { received:createChatItem(content:\$content) { id fromId channelId content createdAt updatedAt status } }")
                put("operationName", "Incoming")
                put("variables", buildJsonObject { put("content", """{"type":"TEXT","value":{"text":"$id"}}""") })
            }
            val encrypted = wire(document)
            val received = client.post(url, encrypted, "application/octet-stream", mapOf("c-id" to id)).use { response ->
                assertEquals(200, response.status.value)
                decode(response.bodyAsBytes())
            }
            assertFalse(received.containsKey("errors"))
            val item = received.getValue("data").jsonObject.getValue("received").jsonArray.single().jsonObject
            assertEquals(id, item.getValue("fromId").jsonPrimitive.content)
            assertTrue(item.getValue("channelId") is JsonNull)
            assertNotNull(RustChatStore.getById(item.getValue("id").jsonPrimitive.content))
            val replay = ble(encrypted)
            assertEquals(200, replay.first)
            assertTrue(decode(replay.second).getValue("data").jsonObject.getValue("received").jsonArray.isEmpty())
            val invalid = ble(wire(buildJsonObject { put("query", "mutation { createChatItem { id } }") }))
            assertEquals(200, invalid.first)
            assertTrue(decode(invalid.second).getValue("errors").jsonArray.isNotEmpty())
            UserPrefs.service.value = false
            assertEquals(403, ble(encrypted).first)
            client.post(url, encrypted, "application/octet-stream", mapOf("c-id" to id)).use { assertEquals(403, it.status.value) }
        } finally {
            client.close()
            RustChatStore.deleteByPeerId(id)
            RustPeerStore.delete(id)
            PeerCacher.load()
            com.ismartcoding.plain.chat.ChatCacher.load()
            TempData.activeToId = active
            stopHttpEngineAsync()
            UserPrefs.httpPort.value = http
            UserPrefs.httpsPort.value = https
            UserPrefs.service.value = service
            if (service) startHttpEngineAsync()
        }
    }

    @Test
    fun blePeerExecutorChecksChannelSignatureBeforeCommittingInvite() = runBlocking {
        val prefix = "synthetic-peer-channel-${UUID.randomUUID()}"
        val key = ByteArray(32) { 8 }
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val owner = DPeer(id = "$prefix-owner", name = prefix, publicKey = Base64.encode(publicKey), key = Base64.encode(key), status = PeerStatus.PAIRED)
        val channelId = "$prefix-channel"
        val actor = TempData.clientId
        val service = UserPrefs.service.value
        val signature = Base64.encode(signEd25519(privateKey, channelMessagePayload(channelId, 1, ChannelSystemMessageAction.INVITE, actor).encodeToByteArray()))
        val invite = ChannelInvite(channelId, prefix, Base64.encode(ByteArray(32) { 9 }), owner.id,
            listOf(ChannelMember(owner.id), ChannelMember(actor, ChannelMemberStatus.PENDING)),
            listOf(MemberPeerInfo(id = owner.id, name = prefix, publicKey = owner.publicKey)), 1, signature)
        suspend fun deliver(invite: ChannelInvite): Boolean {
            val content = buildJsonObject {
                put("query", "mutation Incoming(\$payload:String!) { channelSystemMessage(type:INVITE,payload:\$payload) }")
                put("variables", buildJsonObject { put("payload", JsonHelper.jsonEncode(invite)) })
            }.toString()
            val timestamp = System.currentTimeMillis()
            val signature = Base64.encode(signEd25519(privateKey, "$timestamp$content".encodeToByteArray()))
            val body = RustPeerWireStore.encrypt(key, "$signature|$timestamp|$content")
            val response = BleBinaryFixture.response(HttpServiceHandler().handleRequest(BleBinaryFixture.peer(owner.id, body), "synthetic-mac"))
            assertEquals(200, response.first)
            val result = Json.parseToJsonElement(chaCha20Decrypt(key, response.second)!!.decodeToString()).jsonObject
            assertFalse(result.containsKey("errors"))
            return result.getValue("data").jsonObject.getValue("channelSystemMessage").jsonPrimitive.boolean
        }
        try {
            RustPeerStore.insert(owner)
            UserPrefs.service.value = true
            assertFalse(deliver(invite.copy(signature = "")))
            assertNull(RustChannelStore.getById(channelId))
            assertTrue(deliver(invite))
            val current = RustChannelStore.getById(channelId)!!
            assertEquals(prefix, current.name)
            assertEquals(1L, current.version)
            assertEquals(ChannelMemberStatus.PENDING, current.members.single { it.peerId == actor }.status)
        } finally {
            RustChannelStore.delete(channelId)
            RustPeerStore.delete(owner.id)
            ChannelCacher.load()
            PeerCacher.load()
            UserPrefs.service.value = service
        }
    }
}
