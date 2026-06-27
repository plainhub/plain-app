package com.ismartcoding.plain.platform

/**
 * Whether Bluetooth is currently powered on.
 */
expect fun isBluetoothEnabled(): Boolean

/**
 * Whether the device has a Bluetooth adapter at all.
 */
expect fun isBluetoothSupported(): Boolean