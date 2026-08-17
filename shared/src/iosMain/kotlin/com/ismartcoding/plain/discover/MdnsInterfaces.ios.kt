package com.ismartcoding.plain.discover

import com.ismartcoding.plain.lib.mdns.mdnsInterfaceProvider
import com.ismartcoding.plain.platform.getDeviceIP4s

/**
 * iOS interface data needs the Swift bridge, which lives above shared-lib, so
 * the provider is installed from here. Interface names are synthesized (en0
 * is iOS's primary Wi-Fi interface) so `IP_MULTICAST_IF` routing via
 * `if_nametoindex` works.
 */
internal actual fun ensureMdnsInterfacesInstalled() {
    if (mdnsInterfaceProvider != null) return
    mdnsInterfaceProvider = {
        val ips = getDeviceIP4s()
        val out = ArrayList<Triple<String, Short, String>>(ips.size)
        for ((i, ip) in ips.withIndex()) {
            val parts = ip.split(".")
            if (parts.size != 4) continue
            if (parts[0] == "127") continue
            val name = if (i == 0) "en0" else "en$i"
            out.add(Triple(name, 24, ip))
        }
        out
    }
}
