package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.server.BleGattServer

/**
 * Whether Bluetooth is currently powered on.
 */
expect fun isBluetoothEnabled(): Boolean

/**
 * Whether the device has a Bluetooth adapter at all.
 */
expect fun isBluetoothSupported(): Boolean

/**
 * Returns true if Bluetooth LE permission is granted and BLE is ready for use.
 */
expect fun isBleReady(): Boolean

/**
 * Returns true if Bluetooth is enabled and all required BLE permissions are granted.
 */
expect fun isBluetoothReadyToUse(): Boolean

/**
 * Requests BLE permissions if needed and waits for the user's response.
 * Returns true if permissions were granted and Bluetooth is ready.
 */
expect suspend fun ensureBlePermissionAsync(): Boolean

/**
 * Returns true if Bluetooth advertising is ready to start (permission + adapter).
 */
expect fun isBluetoothAdvertiseReady(): Boolean

/**
 * Sets the `canContinue` flag used by the BLE permission flow. When the user
 * grants BLE permission, this is set to true so pending BLE operations can
 * resume. No-op on platforms without a Bluetooth permission flow (iOS).
 */
expect fun setBluetoothCanContinue(value: Boolean)

interface BleTransport {
    fun createScanner(): BleScanner

    fun createServer(): BleGattServer
}

expect fun bleTransport(): BleTransport
