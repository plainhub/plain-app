package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.extensions.hasPermission

actual fun startAwareIfNeeded(): Boolean {
    if (!isWifiAwareSupported) return false
    val hasPermission = if (isTPlus()) {
        appContext.hasPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        appContext.hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (!hasPermission) return false
    WifiAwareTransport.start()
    return true
}

actual fun subscribeAwareForPeer(peer: DPeer) {
    if (!isWifiAwareSupported) return
    WifiAwareTransport.subscribe(peer)
}
