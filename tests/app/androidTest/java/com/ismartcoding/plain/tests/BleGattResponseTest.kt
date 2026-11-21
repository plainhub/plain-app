package com.ismartcoding.plain.tests

import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.platform.network.ble.BleUuids
import com.ismartcoding.plain.platform.network.ble.RustBleWire
import com.ismartcoding.plain.platform.network.ble.server.AndroidBleGattServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BleGattResponseTest {
    private val adapter get() = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(BluetoothManager::class.java).adapter

    @Test
    fun writeFromAnotherDeviceObjectGetsItsPeerGraphqlResponse() = runBlocking {
        val address = "02:00:00:00:00:01"
        val connected = adapter.getRemoteDevice(address)
        val writer = adapter.getRemoteDevice(address)
        assertNotSame(connected, writer)
        assertEquals(connected.address, writer.address)

        val server = AndroidBleGattServer()
        val callback = server.gattCallback
        val characteristic = UUID.fromString(BleUuids.HTTP_CHAR_UUID)
        val requestId = 123
        val request = BleBinaryFixture.peer("fixture-peer", byteArrayOf(1))
        val frame = checkNotNull(RustBleWire.frame(request, requestId, false, 0, 512))
        val assembly = RustBleWire.assembler()
        try {
            callback.onConnectionStateChange(connected, GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
            callback.onMtuChanged(connected, 517)
            var response: ByteArray? = null
            server.respondToWrite(writer, characteristic, frame) { mac, charUuid, bytes ->
                assertEquals(address, mac)
                assertEquals(characteristic.toString(), charUuid)
                response = assembly.push(bytes)
                true
            }
            assertNotNull("A completed GATT write must send a response", response)
            assertEquals(requestId, assembly.requestId)
            assertTrue(assembly.response)
            assertTrue(BleBinaryFixture.response(checkNotNull(response)).first in 400..499)
        } finally {
            assembly.close()
            server.stop()
        }
    }

    @Test
    fun reconnectDoesNotReceiveTheRestOfTheOldResponse() = runBlocking {
        val address = "02:00:00:00:00:02"
        val connected = adapter.getRemoteDevice(address)
        val replacement = adapter.getRemoteDevice(address)
        val server = AndroidBleGattServer()
        val callback = server.gattCallback
        val characteristic = UUID.fromString(BleUuids.NEARBY_CHAR_UUID)
        val request = RustBleWire.nearby("DISCOVER:".encodeToByteArray())
        val frame = checkNotNull(RustBleWire.frame(request, 124, false, 0, 512))
        val assembly = RustBleWire.assembler()
        try {
            callback.onConnectionStateChange(connected, GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
            var notifications = 0
            server.respondToWrite(connected, characteristic, frame) { _, _, bytes ->
                notifications++
                assertNull("The response needs another notification", assembly.push(bytes))
                callback.onConnectionStateChange(connected, GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
                callback.onConnectionStateChange(replacement, GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
                true
            }
            assertEquals(1, notifications)
        } finally {
            assembly.close()
            server.stop()
        }
    }
}
