package com.ismartcoding.plain.platform

/**
 * Snapshot of Bluetooth LE capabilities, permissions, and runtime status.
 *
 * On platforms without BLE (or where BLE is stubbed) every boolean is false
 * and every nullable field is null/empty.
 */
data class BleDebugInfo(
    // ---- BLE Support ----
    val bluetoothSupported: Boolean = false,
    val bluetoothEnabled: Boolean = false,

    // ---- Permissions (Android S+: SCAN/CONNECT/ADVERTISE; pre-S: ACCESS_FINE_LOCATION) ----
    val scanPermissionGranted: Boolean = false,
    val connectPermissionGranted: Boolean = false,
    val advertisePermissionGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    /** Combined: all permissions required to use BLE are granted. */
    val blePermissionGranted: Boolean = false,
    /** Combined: Bluetooth enabled + permissions granted + advertise permission. */
    val advertiseReady: Boolean = false,

    // ---- Runtime Status ----
    /** Whether the local GATT server (advertising) is currently running. */
    val advertisingRunning: Boolean = false,
    /** This device's clientId (13-char short UUID) advertised in serviceData. */
    val clientId: String = "",
    /** The BLE service UUID used for advertise/scan. */
    val serviceUuid: String = "",
)

/** Returns a fresh snapshot of BLE debug info. */
expect fun getBleDebugInfo(): BleDebugInfo
