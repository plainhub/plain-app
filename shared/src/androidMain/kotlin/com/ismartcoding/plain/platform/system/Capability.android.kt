package com.ismartcoding.plain.platform.system

import android.os.Build
import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.platform.system.AppChannelType
import com.ismartcoding.plain.platform.network.isBluetoothSupported
import com.ismartcoding.plain.platform.network.isWifiAwareSupported

actual fun getDeviceCapabilities(): List<Capability> = Capability.entries.filter { capability ->
    when (capability) {
        Capability.ADB -> true
        Capability.BLE -> isBluetoothSupported()
        Capability.WIFI_AWARE -> isWifiAwareSupported
        Capability.VIDEO_REMUX, Capability.VIDEO_TRANSCODE -> false
        Capability.MEDIA_SCAN, Capability.SMB_SERVER, Capability.DISK_MANAGER -> false
        Capability.MEDIA_TRASH, Capability.MEDIA_FAVORITE -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        Capability.MEDIA_MOVE, Capability.MIRROR_AUDIO -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        Capability.PACKAGES, Capability.NOTIFICATIONS -> buildChannel != AppChannelType.GOOGLE.name
        Capability.APP_UPDATES -> buildChannel == AppChannelType.GITHUB.name
        Capability.APP_RELAUNCH, Capability.MEDIA_DELETE,
        Capability.TELEPHONY,
        Capability.SCREEN_MIRROR,
        Capability.CLIPBOARD -> true
    }
}
