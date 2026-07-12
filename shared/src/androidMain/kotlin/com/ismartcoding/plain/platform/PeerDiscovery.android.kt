package com.ismartcoding.plain.platform

import android.Manifest
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.discover.LANDiscoverManager
import com.ismartcoding.plain.lib.extensions.hasPermission

actual fun discoverPeerDevice(peerId: String, key: ByteArray) {
    LANDiscoverManager.discoverSpecificDevice(peerId, key)
}

actual fun canShowNotifications(): Boolean {
    return if (isTPlus()) {
        appContext.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        androidx.core.app.NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }
}
