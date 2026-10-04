package com.ismartcoding.plain.discover

import com.ismartcoding.plain.lib.logcat.LogCat

object MdnsDiscoverManager {
    fun startReceiver() = RustMdnsRuntime.request("receiver")
    fun startPeriodicDiscovery() = RustMdnsRuntime.request("start")
    fun stopPeriodicDiscovery() = RustMdnsRuntime.request("stop")
    fun isDiscovering(): Boolean = RustMdnsRuntime.scanning
    fun updateAdvertisedService() = RustMdnsRuntime.request("update")
    fun browse() = RustMdnsRuntime.request("browse")
    fun scheduleRestart(reason: String) {
        LogCat.d("Network change ($reason): refreshing Rust mDNS interfaces")
        RustMdnsRuntime.request("restart")
    }
}
