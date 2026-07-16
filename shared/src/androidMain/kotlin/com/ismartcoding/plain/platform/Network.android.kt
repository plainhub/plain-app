package com.ismartcoding.plain.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.helpers.PortHelper
import com.ismartcoding.plain.lib.helpers.NetworkHelper

actual fun getNetworkType(): NetworkType {
    val ctx = appContextValue ?: return NetworkType.NONE
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return NetworkType.NONE
    val network = cm.activeNetwork ?: return NetworkType.NONE
    val caps = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        else -> NetworkType.NONE
    }
}

actual fun getDeviceIP4(): String = NetworkHelper.getDeviceIP4()

actual fun getBestIp(ips: List<String>): String = NetworkHelper.getBestIp(ips)

actual fun getDeviceIP4sWithPrefixLength(): Set<Pair<String, Short>> {
    return NetworkHelper.getDeviceIP4sWithPrefixLength()
}

actual fun isVPNConnected(): Boolean {
    val ctx = appContextValue ?: return false
    return NetworkHelper.isVPNConnected(ctx)
}

actual fun isPortInUse(port: Int): Boolean = PortHelper.isPortInUse(port)