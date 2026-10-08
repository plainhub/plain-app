package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.peer.*
import com.ismartcoding.plain.chat.channel.RustChannelStore
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.PeerStatus
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.verifyEd25519
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PeerWireRustHttpTest {
    @Test
    fun requestsUseRustIdentityCurrentKeysAndInteroperableAuthenticatedEncryption() = runBlocking {
        val prefix = "synthetic-peer-wire-${UUID.randomUUID()}"
        val key = ByteArray(32) { 7 }
        val peer = DPeer(id = prefix, name = prefix, key = Base64.encode(key), status = PeerStatus.PAIRED, ip = "127.0.0.1", port = 1)
        val channel = RustChannelStore.create(prefix)
        try {
            RustPeerStore.insert(peer)
            val content = """{"type":"TEXT","value":{"text":"中文 %_ quoted | message"}}"""
            val prepared = PeerWireTestApi.chat(peer.id, content)
            val body = prepared.getValue("body").jsonPrimitive.content
            val parts = body.split('|', limit = 3)
            assertTrue(verifyEd25519(Base64.decode(SignatureHelper.getRawPublicKeyBase64Async()), (parts[1] + parts[2]).encodeToByteArray(), Base64.decode(parts[0])))
            assertEquals(content, Json.parseToJsonElement(parts[2]).jsonObject.getValue("variables").jsonObject.getValue("content").jsonPrimitive.content)
            assertEquals(peer.key, prepared.getValue("key").jsonPrimitive.content)
            val encrypted = PeerWireTestApi.encrypt(key, body)
            assertEquals(body, chaCha20Decrypt(key, encrypted)!!.decodeToString())
            assertEquals(body, PeerWireTestApi.decrypt(key, chaCha20Encrypt(key, body)))
            assertNull(PeerWireTestApi.decrypt(ByteArray(32) { 8 }, encrypted))
            assertNull(PeerWireTestApi.decrypt(key, "plaintext".encodeToByteArray()))
            val group = PeerWireTestApi.chat(peer.id, content, channel.id)
            assertEquals(channel.key, group.getValue("key").jsonPrimitive.content)
            assertEquals(channel.id, group.getValue("channelId").jsonPrimitive.content)
            peer.key = Base64.encode(ByteArray(32) { 9 })
            RustPeerStore.update(peer)
            val aware = PeerWireTestApi.startAware(peer.id)
            assertEquals(peer.key, aware.getValue("key").jsonPrimitive.content)
            assertEquals("", aware.getValue("channelId").jsonPrimitive.content)
            assertTrue(aware.getValue("body").jsonPrimitive.content.contains("startAware"))
        } finally {
            RustChannelStore.remove(channel.id)
            RustPeerStore.delete(peer.id)
            PeerCacher.load()
            com.ismartcoding.plain.chat.channel.ChannelCacher.load()
        }
    }
}
