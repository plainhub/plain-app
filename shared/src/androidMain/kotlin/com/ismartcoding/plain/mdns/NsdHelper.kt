package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.discover.RustDiscoveryAdvertisement
import com.ismartcoding.plain.lib.mdns.MdnsHostResponder
import java.util.concurrent.atomic.AtomicBoolean

object NsdHelper {
    // Prevents concurrent starts from racing multicast lock/socket lifecycle.
    private val registering = AtomicBoolean(false)
    private val serviceLock = Any()
    private var generation = 0L

    /**
     * Start mDNS hostname responder for the active web service.
     */
    suspend fun registerServices(): Boolean {
        if (!registering.compareAndSet(false, true)) {
            LogCat.d("registerServices already in progress, skipping")
            return false
        }
        try {
            return registerServicesInternal()
        } finally {
            registering.set(false)
        }
    }

    private suspend fun registerServicesInternal(): Boolean {
        val expected = synchronized(serviceLock) { unregisterService(); generation }
        val service = RustDiscoveryAdvertisement.mdns()
        val hostname = service.targetHostname
        return synchronized(serviceLock) {
            if (generation != expected) false else MdnsHostResponder.start(hostname, service)
        }
    }

    /**
     * Withdraw the mDNS service advertisement. The shared socket and hostname
     * responder stay alive (the browser may still be discovering), so this is
     * NOT a full stop — see [MdnsHostResponder.clearService].
     */
    fun unregisterService() {
        synchronized(serviceLock) {
            generation++
            MdnsHostResponder.clearService()
        }
    }
}
