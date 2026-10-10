package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.*
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.PeerStatus
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PeerStatusRustHttpTest {
    @Test
    fun rustTlsStatusKeepsPeerOnlineUntilLastAuthenticatedConnectionCloses() = runBlocking {
        val id = "synthetic-status-${UUID.randomUUID()}"
        val key = ByteArray(32) { 6 }
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val peer = DPeer(id = id, name = id, publicKey = Base64.encode(publicKey), key = Base64.encode(key), status = PeerStatus.PAIRED)
        val service = UserPrefs.service.value
        val http = UserPrefs.httpPort.value
        val https = UserPrefs.httpsPort.value
        val client = createPeerStatusHttpClient()
        val tasks = mutableListOf<Job>()
        suspend fun snapshot() = RustContentApi.postJsonOrThrow("chat/peer-status", buildJsonObject { put("action", "snapshot") }).getValue("result").jsonObject
        suspend fun waitState(online: Boolean, revision: Long = -1) = withTimeout(5000) {
            while (true) {
                val row = snapshot()
                if ((row.getValue("online").jsonArray.any { it.jsonPrimitive.content == id }) == online && row.getValue("revision").jsonPrimitive.long > revision) break
                delay(20)
            }
        }
        try {
            RustPeerStore.insert(peer)
            UserPrefs.service.set(true)
            stopHttpEngineAsync()
            UserPrefs.httpPort.set(0)
            UserPrefs.httpsPort.set(0)
            startHttpEngineAsync()
            assertTrue(com.ismartcoding.plain.platform.checkHttpServerAsync())
            assertTrue(snapshot().getValue("outgoing").jsonObject.getValue("started").jsonPrimitive.boolean)
            val url = "wss://127.0.0.1:${UserPrefs.httpsPort.value}/status?cid=$id"
            suspend fun connect(release: CompletableDeferred<Unit>) {
                val ready = CompletableDeferred<Unit>()
                tasks += launch {
                    try {
                        client.webSocket(url) { socket ->
                            val timestamp = System.currentTimeMillis()
                            val signature = Base64.encode(signEd25519(privateKey, "$timestamp$id".encodeToByteArray()))
                            socket.sendBinary(PeerWireTestApi.encrypt(key, "$signature|$timestamp|$id"))
                            assertEquals("ok", socket.incoming.receive().text)
                            ready.complete(Unit)
                            release.await()
                            socket.close()
                        }
                    } catch (error: Throwable) { ready.completeExceptionally(error); throw error }
                }
                withTimeout(5000) { ready.await() }
            }
            val first = CompletableDeferred<Unit>()
            val second = CompletableDeferred<Unit>()
            connect(first)
            connect(second)
            waitState(true)
            PeerStatusProjection.refresh()
            assertTrue(PeerStatusManager.isOnline(id))
            val revision = snapshot().getValue("revision").jsonPrimitive.long
            first.complete(Unit)
            waitState(true, revision)
            PeerStatusProjection.refresh()
            assertTrue(PeerStatusManager.isOnline(id))
            second.complete(Unit)
            waitState(false)
            PeerStatusProjection.refresh()
            assertFalse(PeerStatusManager.isOnline(id))
            RustContentApi.postJsonOrThrow("chat/peer-status", buildJsonObject { put("action", "stop") })
            assertFalse(snapshot().getValue("outgoing").jsonObject.getValue("started").jsonPrimitive.boolean)
            RustContentApi.postJsonOrThrow("chat/peer-status", buildJsonObject { put("action", "start") })
            assertTrue(snapshot().getValue("outgoing").jsonObject.getValue("started").jsonPrimitive.boolean)
            stopHttpEngineAsync()
            assertFalse(snapshot().getValue("outgoing").jsonObject.getValue("started").jsonPrimitive.boolean)
        } finally {
            tasks.forEach { it.cancelAndJoin() }
            stopHttpEngineAsync()
            UserPrefs.httpPort.set(http)
            UserPrefs.httpsPort.set(https)
            UserPrefs.service.set(service)
            RustPeerStore.remove(id)
            PeerStatusProjection.refresh()
            client.close()
        }
    }
}
