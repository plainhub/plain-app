package com.ismartcoding.plain

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.ismartcoding.plain.discover.LANDiscoverManager
import com.ismartcoding.plain.services.HttpServerService

object NetworkMonitor {
    fun init(context: Context) {
        val cm = context.getSystemService(
            ConnectivityManager::class.java
        )

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                check(cm)
            }

            override fun onLost(network: Network) {
                check(cm)
            }
        }

        cm.registerDefaultNetworkCallback(callback)

        check(cm)
    }

    private fun check(cm: ConnectivityManager) {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)

        val connected =
            caps?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true
        val reason = if (connected) "Connected" else "Disconnected"
        LANDiscoverManager.scheduleRestart(reason)
        HttpServerService.instance?.mdnsRegister?.schedule(reason)
    }
}