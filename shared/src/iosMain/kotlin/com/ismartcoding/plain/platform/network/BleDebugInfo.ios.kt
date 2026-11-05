package com.ismartcoding.plain.platform.network

import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.network.ble.PairingTransport

actual fun getBleDebugInfo(): BleDebugInfo {
    return BleDebugInfo(
        bluetoothSupported = isBluetoothSupported(),
        bluetoothEnabled = isBluetoothEnabled(),
        advertisingRunning = PairingTransport.isAdvertising(),
        clientId = Prefs.clientId.value,
        serviceUuid = Prefs.clientId.value,
    )
}
