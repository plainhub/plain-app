package com.ismartcoding.plain.platform

import android.bluetooth.BluetoothManager
import android.content.Context
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.ble.client.AndroidBleScanner
import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.server.AndroidBleGattServer
import com.ismartcoding.plain.ble.server.BleGattServer
import com.ismartcoding.plain.features.bluetooth.client.BluetoothUtil

actual fun isBluetoothEnabled(): Boolean {
    val ctx = appContextValue ?: return false
    val manager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
    return try {
        manager.adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }
}

actual fun isBluetoothSupported(): Boolean {
    val ctx = appContextValue ?: return false
    return ctx.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH)
}

actual fun isBleReady(): Boolean = BluetoothUtil.isBlePermissionGranted()

actual fun isBluetoothReadyToUse(): Boolean = BluetoothUtil.isBluetoothReadyToUse()

actual suspend fun ensureBlePermissionAsync(): Boolean = BluetoothUtil.ensurePermissionAsync()

actual fun isBluetoothAdvertiseReady(): Boolean = BluetoothUtil.isAdvertiseReady()

actual fun bleTransport(): BleTransport = AndroidBleTransport

object AndroidBleTransport : BleTransport {
    override fun createScanner(): BleScanner = AndroidBleScanner

    override fun createServer(): BleGattServer = AndroidBleGattServer()
}