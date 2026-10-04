package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.chat.peer.transport.PeerTransportHost
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.discover.NearbyHttpClient
import com.ismartcoding.plain.discover.PairingMessenger
import com.ismartcoding.plain.platform.startHttpEngineAsync
import com.ismartcoding.plain.platform.stopHttpEngineAsync
import com.ismartcoding.plain.platform.isBleReady
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.preferences.UserPrefs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PeerPrewarmNearbyRustHttpTest {
    @Test
    fun rustGatesUnpairedPrewarmAndReadsActualOsCapabilities() = runBlocking {
        val id = "synthetic-prewarm-${UUID.randomUUID()}"
        try {
            RustPeerStore.insert(DPeer(id = id, name = id))
            assertTrue(RustContentApi.postJson("chat/prewarm", buildJsonObject { put("id", id) }).getValue("result") is JsonNull)
            val caps = PeerTransportHost.handle("peerTransportPrewarmCapabilities", buildJsonObject {}).jsonObject
            assertEquals(isBleReady(), caps.getValue("bleReady").jsonPrimitive.boolean)
            assertEquals(isWifiAwareSupported, caps.getValue("awareSupported").jsonPrimitive.boolean)
        } finally {
            RustPeerStore.delete(id)
            com.ismartcoding.plain.chat.peer.PeerCacher.load()
        }
    }
    @Test
    fun nearbyProbeAndPairingMessageUseRealRustTlsLoopback() = runBlocking {
        val id = "synthetic-nearby-${UUID.randomUUID()}"
        val service = UserPrefs.service.value
        val http = UserPrefs.httpPort.value
        val https = UserPrefs.httpsPort.value
        try {
            UserPrefs.service.value = true
            stopHttpEngineAsync()
            UserPrefs.httpPort.value = 0
            UserPrefs.httpsPort.value = 0
            assertTrue(startHttpEngineAsync())
            val port = UserPrefs.httpsPort.value
            assertTrue(NearbyHttpClient.probe("127.0.0.1", port))
            assertTrue(PairingMessenger.sendCancel(DPairingCancel(fromId = id, toId = "$id-other"), "127.0.0.1", port))
            stopHttpEngineAsync()
            assertFalse(NearbyHttpClient.probe("127.0.0.1", port))
            assertFalse(NearbyHttpClient.probe("127.0.0.1", 0))
        } finally {
            stopHttpEngineAsync()
            UserPrefs.httpPort.value = http
            UserPrefs.httpsPort.value = https
            UserPrefs.service.value = service
            if (service) startHttpEngineAsync()
        }
    }
}
