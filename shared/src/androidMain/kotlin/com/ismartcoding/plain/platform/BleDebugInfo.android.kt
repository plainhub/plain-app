package com.ismartcoding.plain.platform

import android.Manifest
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.lib.extensions.hasPermission

actual fun getBleDebugInfo(): BleDebugInfo {
    val ctx = appContext
    val isSPlus = isSPlus()

    val scanGranted = ctx.hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    val connectGranted = ctx.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    val advertiseGranted = ctx.hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    val locationGranted = ctx.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
        ctx.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    // Pre-S requires location + bluetooth enabled; S+ requires SCAN/CONNECT/ADVERTISE.
    val blePermissionGranted = if (isSPlus) {
        scanGranted && connectGranted
    } else {
        locationGranted
    }

    val bluetoothSupported = isBluetoothSupported()
    val bluetoothEnabled = isBluetoothEnabled()
    val advertiseReady = isBluetoothAdvertiseReady()
    val advertisingRunning = PairingTransport.isAdvertising()

    return BleDebugInfo(
        bluetoothSupported = bluetoothSupported,
        bluetoothEnabled = bluetoothEnabled,
        scanPermissionGranted = scanGranted,
        connectPermissionGranted = connectGranted,
        advertisePermissionGranted = advertiseGranted,
        locationPermissionGranted = locationGranted,
        blePermissionGranted = blePermissionGranted,
        advertiseReady = advertiseReady,
        advertisingRunning = advertisingRunning,
        clientId = TempData.clientId,
        serviceUuid = BleUuids.SERVICE_UUID,
    )
}
