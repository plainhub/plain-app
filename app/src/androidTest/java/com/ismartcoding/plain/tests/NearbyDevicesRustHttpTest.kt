package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.discover.NearbyDeviceCache
import com.ismartcoding.plain.discover.RustNearbyDevices
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class NearbyDevicesRustHttpTest {
    @Test
    fun nearbyFactsMergeInRustAndHistoryRemovalAlsoRemovesTheLiveSnapshot() = runBlocking {
        val id = "synthetic-nearby-list-${UUID.randomUUID()}"
        val device = DNearbyDevice(id, id, listOf("192.0.2.1"), 2443, DeviceType.PHONE, "1", "test", Clock.System.now(), discoveryMethods = setOf(DiscoveryMethod.LAN))
        try {
            RustNearbyDevices.seen(device, resident = true)
            RustNearbyDevices.seen(device.copy(ips = listOf("192.0.2.2"), discoveryMethods = setOf(DiscoveryMethod.BLE)))
            RustNearbyDevices.refresh()
            val merged = NearbyViewModel.nearbyDevices.value.single { it.id == id }
            assertEquals(listOf("192.0.2.1", "192.0.2.2"), merged.ips)
            assertEquals(setOf(DiscoveryMethod.LAN, DiscoveryMethod.BLE), merged.discoveryMethods)
            assertTrue(NearbyDeviceCache.getAllAsync().any { it.id == id })
            NearbyDeviceCache.removeAsync(id)
            RustNearbyDevices.refresh()
            assertFalse(NearbyViewModel.nearbyDevices.value.any { it.id == id })
            assertFalse(NearbyDeviceCache.getAllAsync().any { it.id == id })
        } finally {
            NearbyDeviceCache.removeAsync(id)
            RustNearbyDevices.refresh()
        }
    }
}
