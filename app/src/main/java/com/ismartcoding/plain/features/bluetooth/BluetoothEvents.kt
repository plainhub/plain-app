package com.ismartcoding.plain.features.bluetooth

import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import kotlinx.coroutines.withTimeoutOrNull

class RequestEnableBluetoothEvent

class RequestScanConnectBluetoothEvent

class RequestBluetoothLocationPermissionEvent

class RequestBluetoothLocationGPSPermissionEvent

class BluetoothPermissionResultEvent

class BluetoothFindOneEvent(val mac: String)

class ScanBTDeviceTimeoutEvent

class BTDeviceFoundEvent(val device: BTDevice)

object BluetoothEvents {
    fun register() {
        receiveEventHandler<BluetoothPermissionResultEvent> {
            BluetoothUtil.canContinue = true
        }
        receiveEventHandler<BluetoothFindOneEvent> { event ->
            if (BluetoothUtil.isScanning) {
                return@receiveEventHandler
            }
            withIO {
                withTimeoutOrNull(3000) {
                    BluetoothUtil.currentBTDevice = BluetoothUtil.findOneAsync(event.mac)
                }
                BluetoothUtil.stopScan()
            }
        }
    }
}
