package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.*
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.PeerStatus
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.preferences.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PeerLanRustHttpTest {
    @Test
    fun rustSendsEncryptedPeerQueryThroughActualTls() = runBlocking {
        RustContentApi.start()
        val actor = RustSystemState.state.value.clientId
        val originalActor = RustPeerStore.getById(actor)
        val id = "synthetic-lan-${UUID.randomUUID()}"
        val key = ByteArray(32) { 7 }
        val signingKey = com.ismartcoding.plain.platform.generateEd25519KeyPair()
        val publicKey = Base64.encode(signingKey.second)
        val service = UserPrefs.service.value
        val http = UserPrefs.httpPort.value
        val https = UserPrefs.httpsPort.value
        try {
            UserPrefs.service.set(true)
            stopHttpEngineAsync()
            UserPrefs.httpPort.set(0)
            UserPrefs.httpsPort.set(0)
            startHttpEngineAsync()
            assertTrue(com.ismartcoding.plain.platform.checkHttpServerAsync())
            val incoming = DPeer(id = actor, name = "synthetic actor", key = Base64.encode(key), publicKey = publicKey, status = PeerStatus.PAIRED)
            if (originalActor == null) RustPeerStore.insert(incoming) else RustPeerStore.update(incoming)
            RustPeerStore.insert(DPeer(id = id, name = id, ip = "bad,127.0.0.1", port = UserPrefs.httpsPort.value, key = Base64.encode(key), publicKey = publicKey, status = PeerStatus.PAIRED))
            val timestamp = System.currentTimeMillis()
            val document = """{"query":"query { __typename }","variables":{}}"""
            val signature = Base64.encode(com.ismartcoding.plain.platform.signEd25519(signingKey.first, "$timestamp$document".encodeToByteArray()))
            val result = RustContentApi.postJsonOrThrow("chat/transport", buildJsonObject {
                put("action", "send"); put("id", id); put("channel_id", "")
                put("key", Base64.encode(key)); put("body", "$signature|$timestamp|$document")
            }, longRunning = true).getValue("result").jsonObject
            assertEquals("Query", result.getValue("data").jsonObject.getValue("__typename").jsonPrimitive.content)
            assertTrue(result["errors"] == null || result["errors"] is JsonNull)
        } finally {
            stopHttpEngineAsync()
            RustPeerStore.delete(id)
            if (originalActor == null) RustPeerStore.delete(actor) else RustPeerStore.update(originalActor)
            UserPrefs.httpPort.set(http)
            UserPrefs.httpsPort.set(https)
            UserPrefs.service.set(service)
            PeerCacher.load()
        }
    }
}
