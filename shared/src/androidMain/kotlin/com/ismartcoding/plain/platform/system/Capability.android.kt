package com.ismartcoding.plain.platform.system

import android.os.Build
import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.platform.system.AppChannelType
import com.ismartcoding.plain.platform.network.isBluetoothSupported
import com.ismartcoding.plain.platform.network.isWifiAwareSupported

actual fun getDeviceCapabilities(): List<Capability> = Capability.entries.filter { capability ->
    when (capability) {
        Capability.BLE -> isBluetoothSupported()
        Capability.WIFI_AWARE -> isWifiAwareSupported
        Capability.BACKGROUND_SERVICE,
        Capability.LAUNCHER_SHORTCUTS,
        Capability.FILE_BROWSER,
        Capability.IMAGE_LIBRARY,
        Capability.AUDIO_LIBRARY,
        Capability.VIDEO_LIBRARY,
        Capability.DOCUMENT_LIBRARY,
        Capability.SOUND_METER -> true
        Capability.BACKGROUND_TIMER -> isAppForegrounded()
        Capability.BACKGROUND_WAKE -> true
        Capability.FILE_REVEAL, Capability.FILE_CLIPBOARD -> false
        Capability.VIDEO_REMUX, Capability.VIDEO_TRANSCODE, Capability.VIDEO_3GP_CONVERT -> false
        Capability.MEDIA_SCAN, Capability.LAN_SHARE, Capability.DISK_MANAGER -> false
        Capability.MEDIA_TRASH, Capability.MEDIA_FAVORITE -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        Capability.MEDIA_MOVE, Capability.MIRROR_AUDIO -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        Capability.PACKAGES, Capability.SMS, Capability.CALLS, Capability.NOTIFICATIONS, Capability.DONATION -> buildChannel != AppChannelType.GOOGLE.name
        Capability.APP_UPDATES -> buildChannel == AppChannelType.GITHUB.name
        Capability.IMAGE_EDITOR -> isDebugBuild()
        Capability.FILE_EXPORT, Capability.APP_RELAUNCH, Capability.MEDIA_DELETE,
        Capability.DOC_PREVIEW, Capability.IMAGE_SEARCH, Capability.CALL_PHONE,
        Capability.CONTACTS, Capability.NOTES, Capability.FEEDS, Capability.SCREEN_MIRROR,
        Capability.CLIPBOARD, Capability.POMODORO -> true
    }
}
