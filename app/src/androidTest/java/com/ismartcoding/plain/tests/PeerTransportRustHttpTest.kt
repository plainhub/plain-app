package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.chat.peer.transport.PeerTransportHost
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.chat.peer.transport.SignedRequest
import com.ismartcoding.plain.db.DPeer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PeerTransportRustHttpTest {
    @Test
    fun rustSharesCircuitStateForSendsAndDownloadsAndRejectsLateReceipts() = runBlocking {
        val id = "synthetic-peer-transport-${UUID.randomUUID()}"
        val peer = DPeer(id = id, name = id)
        suspend fun call(body: JsonObject) = RustContentApi.postJson("chat/transport", body).getValue("result")
        suspend fun begin(types: List<String>) = call(buildJsonObject {
            put("action", "beginDownload"); put("id", id); put("available", JsonArray(types.map(::JsonPrimitive)))
        }).jsonObject
        suspend fun finish(ticket: JsonElement, kind: String) = call(buildJsonObject {
            put("action", "finishDownload"); put("ticket", ticket); put("outcome", buildJsonObject {
                put("kind", kind); if (kind == "unavailable") put("error", "synthetic offline")
            })
        }).jsonObject
        try {
            RustPeerStore.insert(peer)
            val capabilities = PeerTransportHost.handle("peerTransportCapabilities", buildJsonObject {}).jsonArray.map { it.jsonPrimitive.content }
            assertTrue(capabilities.containsAll(listOf("LAN", "BLE")))
            for (type in capabilities.filter { it != "LAN" }) {
                repeat(2) {
                    val ticket = begin(listOf(type)).getValue("ticket")
                    assertEquals(type, ticket.jsonObject.getValue("transport").jsonPrimitive.content)
                    assertTrue(finish(ticket, "unavailable").getValue("ticket") is JsonNull)
                }
                assertTrue(begin(listOf(type)).getValue("ticket") is JsonNull)
            }
            var blocked = false
            try { PeerTransportRouter.send(peer, SignedRequest("synthetic request", ""), ByteArray(32) { 7 }) }
            catch (_: IllegalStateException) { blocked = true }
            assertTrue(blocked)
            peer.ip = "127.0.0.1"
            RustPeerStore.update(peer)
            val ticket = begin(listOf("LAN")).getValue("ticket")
            assertEquals("LAN", ticket.jsonObject.getValue("transport").jsonPrimitive.content)
            finish(ticket, "connected")
            var duplicateRejected = false
            try { finish(ticket, "connected") } catch (_: IllegalStateException) { duplicateRejected = true }
            assertTrue(duplicateRejected)
            val pending = begin(listOf("LAN")).getValue("ticket")
            RustPeerStore.delete(id)
            var deletedRejected = false
            try { finish(pending, "unavailable") } catch (_: IllegalStateException) { deletedRejected = true }
            assertTrue(deletedRejected)
        } finally {
            RustPeerStore.delete(id)
            com.ismartcoding.plain.chat.peer.PeerCacher.load()
        }
    }
}
