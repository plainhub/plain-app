package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.network.isBluetoothSupported

actual fun getDeviceCapabilities(): List<Capability> = buildList {
    addAll(listOf(
        Capability.DIRECTORY_ACCESS,
        Capability.SOUND_METER,
        Capability.CLIPBOARD,
        Capability.MEDIA_DELETE, Capability.MEDIA_MOVE,
        Capability.MEDIA_SCAN,
    ))
    if (isBluetoothSupported()) add(Capability.BLE)
}
