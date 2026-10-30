package com.ismartcoding.plain.platform.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ismartcoding.plain.appContextValue
import kotlinx.serialization.Serializable

actual fun getNetworkType(): NetworkType {
    val ctx = appContextValue ?: return NetworkType.NONE
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return NetworkType.NONE
    val network = cm.activeNetwork ?: return NetworkType.NONE
    val caps = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE
    return caps.networkType()
}

internal fun NetworkCapabilities.networkType(): NetworkType =
    when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        else -> NetworkType.OTHER
    }

@Serializable
internal data class NetworkInterfaceFacts(val name: String, val networkType: NetworkType, val vpn: Boolean)

internal actual fun vpnActive(): Boolean {
    val context = appContextValue ?: return false
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    return manager.getNetworkCapabilities(manager.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
}
