package com.ismartcoding.plain.features.bluetooth

import com.ismartcoding.plain.lib.channel.ChannelEvent
import com.ismartcoding.plain.lib.channel.receiveEventHandler
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import kotlinx.coroutines.withTimeoutOrNull

class RequestEnableBluetoothEvent: ChannelEvent()

class RequestScanConnectBluetoothEvent: ChannelEvent()

class RequestBluetoothLocationPermissionEvent: ChannelEvent()

class RequestBluetoothLocationGPSPermissionEvent: ChannelEvent()

class BluetoothPermissionResultEvent: ChannelEvent()

class BluetoothFindOneEvent(val mac: String): ChannelEvent()

class ScanBTDeviceTimeoutEvent: ChannelEvent()

class BTDeviceFoundEvent(val device: BTDevice): ChannelEvent()

