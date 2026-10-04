package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.discover.*
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class BlePairingRustHttpTest {
    @Test
    fun actualNativeMissingGattHandleRefusesConnectionAndRustConsumesOnlyItsTicket() = runBlocking {
        val id = "synthetic-ble-init-${UUID.randomUUID()}"
        val device = DNearbyDevice(id, id, emptyList(), 2443, DeviceType.PHONE, "1", "test", Clock.System.now(), discoveryMethods = setOf(DiscoveryMethod.BLE))
        try {
            val ticket = RustPairingRuntime.startBle(device).jsonObject.getValue("ticket").jsonObject
            assertTrue(ticket.getValue("generation").jsonPrimitive.content.isNotEmpty())
            assertEquals("", ticket.getValue("deviceIp").jsonPrimitive.content)
            withTimeout(5_000) {
                while (RustPairingStore.tickets().any { it.deviceId == id }) delay(20)
            }
            assertNull(RustPeerStore.getById(id))
            val closed = UUID.randomUUID().toString()
            BlePairingHost.handle("blePairClose", buildJsonObject { put("generation", closed) })
            assertFalse(BlePairingHost.handle("blePairConnect", buildJsonObject { put("generation", closed); put("id", id) }).jsonPrimitive.boolean)
            assertTrue(RustPairingRuntime.start(device) is JsonNull)
            assertFalse(RustPairingStore.tickets().any { it.deviceId == id })
        } finally {
            RustPairingRuntime.cancel(id)
            RustPeerStore.delete(id)
        }
    }
}
