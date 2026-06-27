package com.ismartcoding.plain.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.ismartcoding.plain.appContextValue

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