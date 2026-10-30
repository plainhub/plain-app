package com.ismartcoding.plain

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.ismartcoding.plain.platform.network.NetworkEvents
import com.ismartcoding.plain.platform.network.NetworkInterfaceFacts
import com.ismartcoding.plain.platform.network.networkType
import java.util.concurrent.ConcurrentHashMap

object NetworkMonitor {
    private val networks = ConcurrentHashMap.newKeySet<Network>()

    fun init(context: Context) {
        val cm = context.getSystemService(
            ConnectivityManager::class.java
        )

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                NetworkEvents.changed()
            }

            override fun onLost(network: Network) {
                NetworkEvents.changed()
            }

            // Fires when the IP address changes on the same network (e.g. DHCP
            // renewal or reconnect without a full disconnect). onAvailable/onLost
            // do not cover this case, so the address bar would otherwise go stale.
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                NetworkEvents.changed()
            }
        }

        cm.registerDefaultNetworkCallback(callback)
        cm.registerNetworkCallback(
            NetworkRequest.Builder().clearCapabilities().build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    networks.add(network)
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    NetworkEvents.changed()
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    NetworkEvents.changed()
                }

                override fun onLost(network: Network) {
                    networks.remove(network)
                    NetworkEvents.changed()
                }
            },
        )
    }

    internal fun interfaceFacts(): List<NetworkInterfaceFacts> = networks.mapNotNull { network ->
        val properties = connectivityManager.getLinkProperties(network) ?: return@mapNotNull null
        val name = properties.interfaceName ?: return@mapNotNull null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
        NetworkInterfaceFacts(name, capabilities.networkType(), capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
    }
}
