package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.discover.RustDiscoveryAdvertisement
import com.ismartcoding.plain.discover.RustBleServiceData
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.preferences.UserPrefs
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class DiscoveryAdvertisementRustHttpTest {
    @Test
    fun currentRootPreferencesAndActualOsFactsProduceAllAdvertisingFormats() = runBlocking {
        val reply = RustDiscoveryAdvertisement.reply()
        assertEquals(TempData.clientId, reply.id)
        assertEquals(UserPrefs.deviceName.value.ifEmpty { getDeviceName() }, reply.name)
        assertEquals(UserPrefs.httpsPort.value, reply.port)
        assertEquals(getDeviceType(), reply.deviceType)
        assertEquals(getAppVersion(), reply.version)
        assertEquals(getPlatformName(), reply.platform)
        val mdns = RustDiscoveryAdvertisement.mdns()
        assertEquals(reply.name, mdns.instanceName)
        assertEquals("_plainapp._tcp.local", mdns.serviceType)
        assertEquals(TempData.mdnsHostname, mdns.targetHostname)
        assertEquals(reply.port, mdns.port)
        assertEquals(listOf("id=${reply.id}", "dv=${reply.deviceType.name}", "ver=${reply.version}", "pf=${reply.platform}", "aw=${if (reply.awareSupported) 1 else 0}", "ar=${if (reply.awareRunning) 1 else 0}"), mdns.txtRecords)
        val payload = RustDiscoveryAdvertisement.ble()
        assertEquals(9, payload.size)
        val parts = RustBleServiceData.decode(payload)!!
        assertEquals(RustBleServiceData.shortIdOf(reply.id), parts.shortId)
        assertEquals(reply.awareSupported, parts.awareSupported)
        assertEquals(reply.awareRunning, parts.awareRunning)
    }
    @Test
    fun rawBleScanHeaderUsesRustForDecodeAndPeerMatching() = runBlocking {
        val bytes = byteArrayOf(0xf3.toByte(), 0, 1, 2, 3, 4, 0x80.toByte(), 0xfe.toByte(), 0xff.toByte())
        val parts = RustBleServiceData.decode(bytes)!!
        assertEquals("000102030480feff", parts.shortId)
        assertTrue(parts.awareSupported)
        assertTrue(parts.awareRunning)
        assertEquals(parts, RustBleServiceData.decode(bytes + byteArrayOf(42)))
        assertNull(RustBleServiceData.decode(null))
        for (size in 0 until 9) assertNull(RustBleServiceData.decode(bytes.copyOf(size)))
        assertEquals("f16d05ec6b29248d", RustBleServiceData.shortIdOf("fixture"))
    }
}
