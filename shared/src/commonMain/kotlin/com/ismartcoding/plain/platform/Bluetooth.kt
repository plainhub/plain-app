package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.server.BleGattServer
import kotlinx.coroutines.flow.StateFlow

/**
 * Coarse-grained BLE availability for UI gating. Single source of truth on
 * both platforms; the boolean helpers below derive from it.
 */
enum class BleAvailability {
    /** Not resolved yet (iOS: central manager not created / state resetting). */
    UNKNOWN,

    /** All required BLE permissions granted and Bluetooth is powered on. */
    READY,

    /** Bluetooth adapter is powered off (user must enable it in system UI). */
    BLUETOOTH_OFF,

    /** Required BLE permissions were denied by the user. */
    PERMISSION_DENIED,

    /** The device has no Bluetooth support at all. */
    UNSUPPORTED,
}

expect fun bleAvailability(): BleAvailability

/**
 * Observable availability. Android refreshes it on permission results and
 * adapter state changes; iOS is driven by centralManagerDidUpdateState.
 */
expect val bleAvailabilityFlow: StateFlow<BleAvailability>

fun isBluetoothEnabled(): Boolean = bleAvailability() == BleAvailability.READY

fun isBluetoothSupported(): Boolean = bleAvailability() != BleAvailability.UNSUPPORTED

fun isBleReady(): Boolean = bleAvailability() == BleAvailability.READY

fun isBluetoothReadyToUse(): Boolean = bleAvailability() == BleAvailability.READY

fun isBluetoothAdvertiseReady(): Boolean = bleAvailability() == BleAvailability.READY

/**
 * Runs the platform permission flow and suspends until the user responds.
 * On iOS this creates the central manager, which shows the one-time system
 * Bluetooth prompt; it never re-prompts after a denial.
 */
expect suspend fun ensureBlePermissionAsync(): Boolean

/**
 * Sets the `canContinue` flag used by the Android BLE permission flow. When
 * the user grants BLE permission, this is set to true so pending BLE
 * operations can resume. No-op on iOS.
 */
expect fun setBluetoothCanContinue(value: Boolean)

interface BleTransport {
    fun createScanner(): BleScanner

    fun createServer(): BleGattServer
}

expect fun bleTransport(): BleTransport
