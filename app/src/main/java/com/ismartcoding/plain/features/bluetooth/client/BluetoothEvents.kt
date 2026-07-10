package com.ismartcoding.plain.features.bluetooth.client

import com.ismartcoding.plain.lib.channel.ChannelEvent

class RequestEnableBluetoothEvent: ChannelEvent()

class RequestScanConnectBluetoothEvent: ChannelEvent()

class RequestBluetoothLocationPermissionEvent: ChannelEvent()

class RequestBluetoothLocationGPSPermissionEvent: ChannelEvent()

class BluetoothPermissionResultEvent: ChannelEvent()
