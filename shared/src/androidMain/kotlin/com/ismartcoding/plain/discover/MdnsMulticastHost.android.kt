package com.ismartcoding.plain.discover

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.plain.appContext

private val multicastMonitor = Any()
private var multicastPermission: WifiManager.MulticastLock? = null

internal actual fun setMdnsMulticastPermission(acquire: Boolean): Boolean = synchronized(multicastMonitor) {
    if (!acquire) {
        multicastPermission?.let { if (it.isHeld) it.release() }
        multicastPermission = null
        return@synchronized true
    }
    if (multicastPermission?.isHeld == true) return@synchronized true
    val wifi = appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return@synchronized false
    val lock = wifi.createMulticastLock("plain-rust-mdns").apply { setReferenceCounted(false) }
    try {
        lock.acquire()
        if (!lock.isHeld) return@synchronized false
        multicastPermission = lock
        true
    } catch (error: Exception) {
        if (lock.isHeld) lock.release()
        throw error
    }
}
