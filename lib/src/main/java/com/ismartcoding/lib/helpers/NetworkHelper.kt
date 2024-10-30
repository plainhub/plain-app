package com.ismartcoding.lib.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkHelper {
    fun getDeviceIP4(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            val map = mutableMapOf<String, String>()
            while (en?.hasMoreElements() == true) {
                val intf = en.nextElement()
                if (intf.isUp) {
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            map[intf.name] = inetAddress.hostAddress ?: ""
                        }
                    }
                }
            }
            if (map.isNotEmpty()) {
                return map["wlan0"] ?: map.values.first()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return ""
    }

    fun getDeviceIP4s(): Set<String> {
        val ips = mutableSetOf<String>()
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            val map = mutableMapOf<String, String>()
            while (en?.hasMoreElements() == true) {
                val intf = en.nextElement()
                if (intf.isUp) {
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            val ip = inetAddress.getHostAddress() ?: ""
                            if (ip.isNotEmpty()) {
                                ips.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return ips
    }

    fun isVPNConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            // Check if the active network is a VPN
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        } catch (ex: Exception) {
            false
        }
    }
}
