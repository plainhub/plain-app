package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.discover.RustPairingStore
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.SignatureHelper
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
    fun rustHandshakeInteroperatesWithNativeCryptoAndPersistsOnlyOwnedPeerFields() = runBlocking {
        val id = "synthetic-pairing-${UUID.randomUUID()}"
        val remoteEcdh = generateECDHKeyPair()
        val (privateKey, publicKey) = generateEd25519KeyPair()
        val request = DPairingRequest(
            fromId = id, fromName = "Fixture ' 中文", port = 2443, deviceType = DeviceType.PHONE,
            ecdhPublicKey = Base64.encode(remoteEcdh.publicKeyEncoded), signaturePublicKey = Base64.encode(publicKey),
            timestamp = System.currentTimeMillis(), ips = listOf("127.0.0.1"),
        )
        request.signature = Base64.encode(signEd25519(privateKey, request.toSignatureData().encodeToByteArray()))
        val (localRequest, localKey) = RustPairingStore.request()
        assertEquals(32, localKey.privateKeyEncoded.size)
        assertEquals(65, localKey.publicKeyEncoded.size)
        assertTrue(verifyEd25519(Base64.decode(SignatureHelper.getRawPublicKeyBase64Async()), localRequest.toSignatureData().encodeToByteArray(), Base64.decode(localRequest.signature)))
        val remoteReply = DPairingResponse(
            fromId = id, toId = localRequest.fromId, port = 2443, deviceType = DeviceType.PHONE,
            ecdhPublicKey = request.ecdhPublicKey, signaturePublicKey = request.signaturePublicKey,
            accepted = true, timestamp = System.currentTimeMillis(), ips = request.ips,
        )
        remoteReply.signature = Base64.encode(signEd25519(privateKey, remoteReply.toSignatureData().encodeToByteArray()))
        assertTrue(RustPairingStore.validateResponse(remoteReply, id))
        val wrongTarget = remoteReply.copy(toId = "another-device")
        wrongTarget.signature = Base64.encode(signEd25519(privateKey, wrongTarget.toSignatureData().encodeToByteArray()))
        assertFalse(RustPairingStore.validateResponse(wrongTarget, id))
        assertEquals(computeECDHSharedKey(remoteEcdh.privateKeyEncoded, localKey.publicKeyEncoded), RustPairingStore.derive(localKey.privateKeyEncoded, remoteEcdh.publicKeyEncoded))
        val (response, responseKey) = requireNotNull(RustPairingStore.response(request, true))
        assertEquals(id, response.toId)
        assertTrue(verifyEd25519(Base64.decode(response.signaturePublicKey), response.toSignatureData().encodeToByteArray(), Base64.decode(response.signature)))
        val key = requireNotNull(RustPairingStore.derive(requireNotNull(responseKey).privateKeyEncoded, remoteEcdh.publicKeyEncoded))
        assertEquals(computeECDHSharedKey(remoteEcdh.privateKeyEncoded, Base64.decode(response.ecdhPublicKey)), key)
        assertNull(RustPairingStore.response(request.copy(fromName = "forged"), true))
        assertNull(RustPairingStore.response(request.copy(timestamp = Long.MIN_VALUE), true))
        assertFalse(RustPairingStore.validateResponse(response, id))
        val rejected = requireNotNull(RustPairingStore.response(request, false))
        assertNull(rejected.second)
        assertEquals("", rejected.first.ecdhPublicKey)
        assertFalse(rejected.first.accepted)
        try {
            RustPairingStore.save(id, request.fromName, request.ips + request.ips, request.port, request.deviceType, key, request.signaturePublicKey)
            val first = requireNotNull(RustPeerStore.getById(id))
            assertEquals("127.0.0.1", first.ip)
            RustPairingStore.save(id, "updated", request.ips, 2444, request.deviceType, key, request.signaturePublicKey)
            val updated = requireNotNull(RustPeerStore.getById(id))
            assertEquals(first.createdAt, updated.createdAt)
            assertEquals("updated", updated.name)
            assertEquals(2444, updated.port)
        } finally {
            RustPeerStore.delete(id)
            PeerCacher.load()
        }
    }
}
