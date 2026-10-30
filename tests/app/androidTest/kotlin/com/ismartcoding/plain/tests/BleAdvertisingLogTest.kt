package com.ismartcoding.plain.tests

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.platform.network.ble.server.AndroidBleGattServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BleAdvertisingLogTest {
    @Test
    fun lateStartCallbackDoesNotStopAnAlreadyRemovedAdvertisement() = runBlocking {
        val server = AndroidBleGattServer()
        val marker = "ble-advertising-log-${UUID.randomUUID()}"
        try {
            server.start(byteArrayOf(0xf0.toByte(), 1, 2, 3, 4, 5, 6, 7, 8), 1)
            val field = AndroidBleGattServer::class.java.getDeclaredField("advertiseCallback").apply { isAccessible = true }
            val callback = field.get(server) as AdvertiseCallback
            server.stop()
            Log.i("PlainNetworkTest", marker)
            callback.onStartSuccess(AdvertiseSettings.Builder().build())
            delay(250)
            val reader = ProcessBuilder("logcat", "-d", "--pid=${Process.myPid()}", "-v", "brief").start()
            val output = reader.inputStream.bufferedReader().use { it.readText() }
            assertEquals(0, reader.waitFor())
            assertTrue("Log marker must be present", output.contains(marker))
            assertTrue("Late completion must not stop an unregistered callback", output.substringAfter(marker).lineSequence().none {
                it.contains("BluetoothLeAdvertiser") && it.contains("Callback not registered or already removed")
            })
        } finally {
            server.stop()
        }
    }
}
