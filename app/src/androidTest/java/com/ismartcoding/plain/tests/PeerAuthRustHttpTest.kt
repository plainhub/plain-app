package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.peer.*
import com.ismartcoding.plain.chat.channel.RustChannelStore
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.PeerStatus
import com.ismartcoding.plain.platform.generateEd25519KeyPair
import com.ismartcoding.plain.platform.signEd25519
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PeerAuthRustHttpTest {
    @Test
    fun incomingAuthUsesRustDatabaseIdentityAndKeysWithSafeTimestampValidation() = runBlocking {
        val prefix = "synthetic-peer-auth-${UUID.randomUUID()}"
        val key = ByteArray(32) { 7 }
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val peer = DPeer(id = prefix, name = prefix, key = Base64.encode(key), publicKey = Base64.encode(publicKey), status = PeerStatus.PAIRED, ip = "127.0.0.1", port = 1)
        val channel = RustChannelStore.create(prefix)
        val content = """{"query":"mutation { startAware }","variables":{}}"""
        suspend fun body(encryptionKey: ByteArray, timestamp: Long = System.currentTimeMillis()): ByteArray {
            val signature = Base64.encode(signEd25519(privateKey, "$timestamp$content".encodeToByteArray()))
            return PeerWireTestApi.encrypt(encryptionKey, "$signature|$timestamp|$content")
        }
        try {
            RustPeerStore.insert(peer)
            val encrypted = body(key)
            val result = PeerWireTestApi.authenticatePeer(peer.id, "", encrypted)
            assertEquals(200, result.getValue("status").jsonPrimitive.int)
            assertEquals(content, result.getValue("content").jsonPrimitive.content)
            assertEquals(peer.key, result.getValue("key").jsonPrimitive.content)
            assertEquals(content, PeerWireTestApi.authenticateEnvelope(key, publicKey, encrypted).getValue("content").jsonPrimitive.content)
            assertEquals(400, PeerWireTestApi.authenticatePeer(peer.id, "", body(key, Long.MIN_VALUE)).getValue("status").jsonPrimitive.int)
            peer.key = Base64.encode(ByteArray(32) { 8 })
            RustPeerStore.update(peer)
            assertEquals(401, PeerWireTestApi.authenticatePeer(peer.id, "", encrypted).getValue("status").jsonPrimitive.int)
            peer.status = PeerStatus.CHANNEL
            RustPeerStore.update(peer)
            assertEquals(403, PeerWireTestApi.authenticatePeer(peer.id, "", encrypted).getValue("status").jsonPrimitive.int)
            val group = PeerWireTestApi.authenticatePeer(peer.id, channel.id, body(Base64.decode(channel.key)))
            assertEquals(200, group.getValue("status").jsonPrimitive.int)
            assertEquals(channel.key, group.getValue("key").jsonPrimitive.content)
            assertEquals(401, PeerWireTestApi.authenticatePeer(peer.id, "$prefix-missing", encrypted).getValue("status").jsonPrimitive.int)
        } finally {
            RustChannelStore.remove(channel.id)
            RustPeerStore.delete(peer.id)
            PeerCacher.load()
            com.ismartcoding.plain.chat.channel.ChannelCacher.load()
        }
    }
}
