package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.network.isBluetoothSupported

actual fun getDeviceCapabilities(): List<Capability> = buildList {
    addAll(listOf(
        Capability.FILE_BROWSER, Capability.SOUND_METER,
        Capability.BACKGROUND_WAKE, Capability.FILE_EXPORT,
        Capability.CLIPBOARD,
        Capability.IMAGE_LIBRARY, Capability.VIDEO_LIBRARY, Capability.AUDIO_LIBRARY,
        Capability.DOCUMENT_LIBRARY, Capability.MEDIA_DELETE, Capability.MEDIA_MOVE,
        Capability.MEDIA_SCAN,
    ))
    if (isBluetoothSupported()) add(Capability.BLE)
    if (isAppForegrounded()) add(Capability.BACKGROUND_TIMER)
}
