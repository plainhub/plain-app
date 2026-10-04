package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.discover.RustPairingStore
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.platform.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PairingRustHttpTest {
    @Test
    fun rustOwnsSessionsAndOnceOnlyHandshakeWithoutExportingEphemeralSecrets() = runBlocking {
        val id = "synthetic-pairing-${UUID.randomUUID()}"
        val responderId = "$id-responder"
        val remoteEcdh = generateECDHKeyPair()
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val request = DPairingRequest(fromId = responderId, fromName = "Fixture ' 中文", port = 2443, deviceType = DeviceType.PHONE,
            ecdhPublicKey = Base64.encode(remoteEcdh.publicKeyEncoded), signaturePublicKey = Base64.encode(publicKey),
            timestamp = System.currentTimeMillis(), ips = listOf("127.0.0.1"))
        request.signature = Base64.encode(signEd25519(privateKey, request.toSignatureData().encodeToByteArray()))
        var generation: String? = null
        try {
            val (localRequest, old) = RustPairingStore.start(id, "Fixture", "127.0.0.1", 2443)
            generation = old.generation
            assertEquals(90_000L, old.delayMs)
            assertTrue(verifyEd25519(Base64.decode(localRequest.signaturePublicKey), localRequest.toSignatureData().encodeToByteArray(), Base64.decode(localRequest.signature)))
            val (currentRequest, current) = RustPairingStore.start(id, "updated", "127.0.0.1", 2443)
            generation = current.generation
            assertNull(RustPairingStore.cancel(id, old.generation))
            assertNull(RustPairingStore.expire(old))
            assertNull(RustPairingStore.expire(current))
            val reply = DPairingResponse(fromId = id, toId = currentRequest.fromId, port = 2443, deviceType = DeviceType.PHONE,
                ecdhPublicKey = request.ecdhPublicKey, signaturePublicKey = request.signaturePublicKey, accepted = true,
                timestamp = System.currentTimeMillis(), ips = request.ips)
            reply.signature = Base64.encode(signEd25519(privateKey, reply.toSignatureData().encodeToByteArray()))
            val wrong = reply.copy(toId = "another-device")
            wrong.signature = Base64.encode(signEd25519(privateKey, wrong.toSignatureData().encodeToByteArray()))
            assertNull(RustPairingStore.complete(wrong, ""))
            assertNull(RustPairingStore.receiveCancel(DPairingCancel(fromId = id, toId = "another-device")))
            val completed = requireNotNull(RustPairingStore.complete(reply, "127.0.0.1"))
            val peer = requireNotNull(completed.peer)
            assertEquals(computeECDHSharedKey(remoteEcdh.privateKeyEncoded, Base64.decode(currentRequest.ecdhPublicKey)), peer.key)
            assertEquals("updated", peer.name)
            assertNull(RustPairingStore.complete(reply, "127.0.0.1"))
            val (repairRequest, repair) = RustPairingStore.start(id, "repaired", "", 2443)
            generation = repair.generation
            val repairReply = reply.copy(toId = repairRequest.fromId, timestamp = System.currentTimeMillis())
            repairReply.signature = Base64.encode(signEd25519(privateKey, repairReply.toSignatureData().encodeToByteArray()))
            val repaired = requireNotNull(requireNotNull(RustPairingStore.complete(repairReply, "")).peer)
            assertEquals(peer.createdAt, repaired.createdAt)
            assertEquals("repaired", repaired.name)
            assertNull(RustPairingStore.respond(request.copy(fromName = "forged"), true))
            assertNull(RustPairingStore.respond(request.copy(timestamp = Long.MIN_VALUE), true))
            assertEquals(true, RustPairingStore.receiveRequest(request))
            assertNull(RustPairingStore.receiveRequest(request.copy(fromName = "forged")))
            assertNull(RustPairingStore.receiveCancel(DPairingCancel(fromId = responderId, toId = "wrong")))
            val incomingCancelled = requireNotNull(RustPairingStore.receiveCancel(DPairingCancel(fromId = responderId, toId = currentRequest.fromId)))
            assertEquals(responderId, incomingCancelled.deviceId)
            assertNull(RustPairingStore.respond(request, true))
            assertEquals(true, RustPairingStore.receiveRequest(request))
            val rejected = requireNotNull(RustPairingStore.respond(request, false))
            assertNull(rejected.second); assertEquals("", rejected.first.ecdhPublicKey)
            assertEquals(true, RustPairingStore.receiveRequest(request))
            assertEquals(false, RustPairingStore.receiveRequest(request))
            val (response, accepted) = requireNotNull(RustPairingStore.respond(request, true))
            assertNull(RustPairingStore.respond(request, true))
            assertTrue(verifyEd25519(Base64.decode(response.signaturePublicKey), response.toSignatureData().encodeToByteArray(), Base64.decode(response.signature)))
            assertEquals(computeECDHSharedKey(remoteEcdh.privateKeyEncoded, Base64.decode(response.ecdhPublicKey)), requireNotNull(accepted).key)
            val (_, cancellation) = RustPairingStore.start(id, "cancel", "127.0.0.1", 2443)
            generation = cancellation.generation
            val cancelled = requireNotNull(RustPairingStore.cancel(id, cancellation.generation))
            assertEquals(id, cancelled.second.toId)
            assertEquals(currentRequest.fromId, cancelled.second.fromId)
            assertNull(RustPairingStore.complete(reply, ""))
        } finally {
            generation?.let { RustPairingStore.cancel(id, it) }
            RustPeerStore.deleteByIds(listOf(id, responderId))
            PeerCacher.load()
        }
    }
}
