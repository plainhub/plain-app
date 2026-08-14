package com.ismartcoding.plain.platform

import com.ismartcoding.plain.discover.MdnsDiscoverManager
import kotlinx.cinterop.staticCFunction
import platform.CoreFoundation.CFRunLoopGetMain
import platform.CoreFoundation.kCFRunLoopDefaultMode
import platform.SystemConfiguration.SCNetworkReachabilityCallBack
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityRef
import platform.SystemConfiguration.SCNetworkReachabilityScheduleWithRunLoop
import platform.SystemConfiguration.SCNetworkReachabilitySetCallback
import platform.SystemConfiguration.kSCNetworkFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable

/**
 * iOS counterpart of Android's `NetworkMonitor`: watches reachability changes
 * via SystemConfiguration so [MdnsDiscoverManager.scheduleRestart] can
 * recreate the mDNS responder socket.
 *
 * Necessary, not optional: mDNS multicast group membership is per-interface.
 * The responder socket joins `224.0.0.251` on the interfaces that exist at
 * bind time; after a Wi-Fi ↔ cellular switch the new interface was never
 * joined, so discovery silently dies until the socket is recreated. Without
 * this monitor iOS has no equivalent of the Android `ConnectivityManager`
 * callback to trigger that recreation.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
object IosNetworkMonitor {
    /** Strong reference — the reachability object must outlive init() or updates stop. */
    private var reachability: SCNetworkReachabilityRef? = null

    /** C callback must be context-free; all state lives in this singleton. */
    private val callback: SCNetworkReachabilityCallBack = staticCFunction { _, flags, _ ->
        val reachable =
            flags and kSCNetworkReachabilityFlagsReachable != 0u &&
                flags and kSCNetworkFlagsConnectionRequired == 0u
        MdnsDiscoverManager.scheduleRestart(if (reachable) "Connected" else "Disconnected")
    }

    fun init() {
        if (reachability != null) return
        val ref = SCNetworkReachabilityCreateWithName(null, "0.0.0.0") ?: return
        SCNetworkReachabilitySetCallback(ref, callback, null)
        SCNetworkReachabilityScheduleWithRunLoop(ref, CFRunLoopGetMain(), kCFRunLoopDefaultMode)
        reachability = ref
    }
}
