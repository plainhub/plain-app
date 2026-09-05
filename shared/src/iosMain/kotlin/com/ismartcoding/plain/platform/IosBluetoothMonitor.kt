package com.ismartcoding.plain.platform

import kotlinx.coroutines.flow.MutableStateFlow
import platform.CoreBluetooth.CBManagerState
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported

/**
 * Maps CBCentralManager state into the common [BleAvailability] enum.
 * The state updates arrive from the central manager delegate owned by
 * [com.ismartcoding.plain.ble.client.IosBleScanner].
 */
object IosBluetoothMonitor {
    val availability = MutableStateFlow(BleAvailability.UNKNOWN)

    fun update(state: CBManagerState) {
        availability.value = when (state) {
            CBManagerStatePoweredOn -> BleAvailability.READY
            CBManagerStatePoweredOff -> BleAvailability.BLUETOOTH_OFF
            CBManagerStateUnauthorized -> BleAvailability.PERMISSION_DENIED
            CBManagerStateUnsupported -> BleAvailability.UNSUPPORTED
            else -> BleAvailability.UNKNOWN
        }
    }
}
