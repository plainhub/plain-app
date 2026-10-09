package com.ismartcoding.plain.tests

import com.ismartcoding.plain.discover.RustBleServiceData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.discover.PairingProjection
import com.ismartcoding.plain.discover.RustNearbyWire
import com.ismartcoding.plain.discover.RustPairingStore
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.ui.models.NearbyItemStatus
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NearbyWirePairingRuntimeRustHttpTest {
    @Test
    fun rustOwnsNearbyWireAndBindsDiscoverIdentityToAdvertisement() = runBlocking {
        val id = "synthetic-nearby-wire-${UUID.randomUUID()}"
        assertEquals("DISCOVER:", RustNearbyWire.encode(NearbyMessageType.DISCOVER, ""))
        assertNull(RustNearbyWire.parse("UNKNOWN:{}"))
        assertNull(RustNearbyWire.parse("PAIR_CANCEL:{}"))
        val wire = RustNearbyWire.encode(NearbyMessageType.PAIR_CANCEL,
            buildJsonObject { put("fromId", id); put("toId", "$id-other") }.toString())
        val parsed = RustNearbyWire.parse(wire)!!
        assertEquals(NearbyMessageType.PAIR_CANCEL, parsed.first)
        assertEquals(id, Json.parseToJsonElement(parsed.second).jsonObject.getValue("fromId").jsonPrimitive.content)
        val reply = buildJsonObject {
            put("id", id); put("name", id); put("port", 1234); put("deviceType", "PHONE")
            put("version", "fixture"); put("platform", "fixture")
        }.toString()
        assertEquals(id, RustNearbyWire.discoverReply(reply, RustBleServiceData.shortIdOf(id)).id)
        var rejected = false
        try { RustNearbyWire.discoverReply(reply, RustBleServiceData.shortIdOf("$id-other")) }
        catch (_: com.ismartcoding.plain.api.RustApiException) { rejected = true }
        assertTrue(rejected)
    }

    @Test
    fun replacementSessionRejectsOldTimeoutAndReconnectReconcilesUiFromRust() = runBlocking {
        val id = "synthetic-pairing-runtime-${UUID.randomUUID()}"
        try {
            val old = RustPairingStore.start(id, id, "127.0.0.1", 1234).second
            val current = RustPairingStore.start(id, id, "127.0.0.1", 1234).second
            assertEquals(current.generation, RustPairingStore.tickets().single { it.deviceId == id }.generation)
            NearbyViewModel.itemStatus[id] = NearbyItemStatus.PAIRING
            PairingProjection.timeout(buildJsonObject {
                put("deviceId", id); put("deviceName", id); put("generation", old.generation); put("error", "fixture timeout")
            }.toString())
            assertEquals(NearbyItemStatus.PAIRING, NearbyViewModel.itemStatus[id])
            assertNull(RustPairingStore.cancel(id, old.generation))
            assertNotNull(RustPairingStore.cancel(id, current.generation))
            PairingProjection.reconcile()
            assertNull(NearbyViewModel.itemStatus[id])
        } finally {
            RustPairingStore.cancel(id)
            NearbyViewModel.itemStatus.remove(id)
        }
    }
}
