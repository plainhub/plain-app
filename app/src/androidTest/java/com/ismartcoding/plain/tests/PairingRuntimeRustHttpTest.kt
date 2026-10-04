package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.discover.*
import com.ismartcoding.plain.chat.peer.*
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.security.MessageDigest
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PairingRuntimeRustHttpTest {
    @Test
    fun rustScanSessionValidatesRepliesCachesAndIgnoresOldReadCompletion() = runBlocking {
        val id = "synthetic-scan-runtime-${UUID.randomUUID()}"
        val short = MessageDigest.getInstance("SHA-256").digest(id.encodeToByteArray()).take(8).joinToString("") { "%02x".format(it) }
        val scan = RustNearbyWire.beginScan()
        val reply = DDiscoverReply(id, id, 2443, DeviceType.PHONE, "1", "android")
        try {
            assertEquals("wait", RustNearbyWire.scanSeen(scan, "invalid").getValue("kind").jsonPrimitive.content)
            val first = RustNearbyWire.scanSeen(scan, short)
            assertEquals("read", first.getValue("kind").jsonPrimitive.content)
            val generation = first.getValue("generation").jsonPrimitive.content
            assertEquals("wait", RustNearbyWire.scanSeen(scan, short).getValue("kind").jsonPrimitive.content)
            assertNull(RustNearbyWire.scanReply(scan, short, "old", JsonHelper.jsonEncode(reply)))
            assertNull(RustNearbyWire.scanReply(scan, short, generation, JsonHelper.jsonEncode(reply.copy(id = "wrong"))))
            val second = RustNearbyWire.scanSeen(scan, short).getValue("generation").jsonPrimitive.content
            assertEquals(id, RustNearbyWire.scanReply(scan, short, second, JsonHelper.jsonEncode(reply))!!.id)
            assertEquals("wait", RustNearbyWire.scanSeen(scan, short).getValue("kind").jsonPrimitive.content)
            RustNearbyWire.endScan(scan)
            assertNull(RustNearbyWire.scanReply(scan, short, second, JsonHelper.jsonEncode(reply)))
        } finally { RustNearbyWire.endScan(scan) }
    }

    @Test
    fun runtimeConsumesIncomingBleInvitationAndCommitsActualSharedKeyOnce() = runBlocking {
        val id = "synthetic-incoming-runtime-${UUID.randomUUID()}"
        val ecdh = generateECDHKeyPair()
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val request = DPairingRequest(fromId = id, fromName = id, port = 2443, deviceType = DeviceType.PHONE,
            ecdhPublicKey = Base64.encode(ecdh.publicKeyEncoded), signaturePublicKey = Base64.encode(publicKey),
            timestamp = System.currentTimeMillis(), ips = emptyList())
        request.signature = Base64.encode(signEd25519(privateKey, request.toSignatureData().encodeToByteArray()))
        try {
            assertTrue(RustPairingRuntime.receiveRequest(request, "synthetic-mac", true).jsonPrimitive.boolean)
            assertTrue(RustPairingRuntime.currentRequest(id, request.signature))
            assertFalse(RustPairingRuntime.receiveRequest(request, "synthetic-mac", true).jsonPrimitive.boolean)
            val result = RustPairingRuntime.respond(request, true).jsonObject
            assertFalse(RustPairingRuntime.currentRequest(id, request.signature))
            val response = JsonHelper.jsonDecode<com.ismartcoding.plain.data.DPairingResponse>(result.getValue("response").toString())
            assertTrue(verifyEd25519(Base64.decode(response.signaturePublicKey), response.toSignatureData().encodeToByteArray(), Base64.decode(response.signature)))
            val peer = requireNotNull(RustPeerStore.getById(id))
            assertEquals(computeECDHSharedKey(ecdh.privateKeyEncoded, Base64.decode(response.ecdhPublicKey)), peer.key)
            assertTrue(RustPairingRuntime.respond(request, true) is JsonNull)
        } finally {
            RustPairingRuntime.receiveCancel(com.ismartcoding.plain.data.DPairingCancel(id, TempData.clientId))
            RustPeerStore.delete(id)
            PeerCacher.load()
        }
    }
}
