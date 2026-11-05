package com.ismartcoding.plain.tests

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.nearby.RustBleAdvertisement
import com.ismartcoding.plain.platform.network.ble.BleUuids
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscoveryAdvertisementRustHttpTest {
    @Test
    fun advertisedUuidsUseRustFilteringWithoutServiceData() {
        val id = "706c6169-2d74-8e53-9a16-f08b62c591d3"
        assertEquals(id, RustBleAdvertisement.decode(listOf("180D", id.uppercase())))
        assertNull(RustBleAdvertisement.decode(emptyList()))
        assertNull(RustBleAdvertisement.decode(listOf("180D")))
        assertNull(RustBleAdvertisement.decode(listOf(id.replace("8e53", "4e53"))))
        val uuid = ParcelUuid.fromString(id).uuid
        val target = ParcelUuid.fromString(BleUuids.DISCOVERY_UUID).uuid
        val mask = ParcelUuid.fromString(BleUuids.DISCOVERY_UUID_MASK).uuid
        assertEquals(target.mostSignificantBits, uuid.mostSignificantBits and mask.mostSignificantBits)
        assertEquals(target.leastSignificantBits, uuid.leastSignificantBits and mask.leastSignificantBits)
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(target), ParcelUuid(mask)).build()
        assertEquals(ParcelUuid(target), filter.serviceUuid)
        assertEquals(ParcelUuid(mask), filter.serviceUuidMask)
    }
}
