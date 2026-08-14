package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.discover.PairingCore
import java.util.concurrent.atomic.AtomicBoolean

object NsdHelper {
    // Prevents concurrent starts from racing multicast lock/socket lifecycle.
    private val registering = AtomicBoolean(false)

    /**
     * Start mDNS hostname responder for the active web service.
     */
    fun registerServices(httpPort: Int?, httpsPort: Int?): Boolean {
        if (!registering.compareAndSet(false, true)) {
            LogCat.d("registerServices already in progress, skipping")
            return false
        }
        try {
            return registerServicesInternal(httpPort, httpsPort)
        } finally {
            registering.set(false)
        }
    }

    private fun registerServicesInternal(httpPort: Int?, httpsPort: Int?): Boolean {
        unregisterService()

        val hasAnyPort = (httpPort != null && httpPort > 0) || (httpsPort != null && httpsPort > 0)
        if (!hasAnyPort) {
            LogCat.e("No active web service port, skip mDNS responder start")
            return false
        }

        val hostname = TempData.mdnsHostname
        val service = buildMdnsServiceInfo(PairingCore.buildDiscoverReply(), hostname)
        return MdnsHostResponder.start(hostname, service)
    }

    /**
     * Withdraw the mDNS service advertisement. The shared socket and hostname
     * responder stay alive (the browser may still be discovering), so this is
     * NOT a full stop — see [MdnsHostResponder.clearService].
     */
    fun unregisterService() {
        MdnsHostResponder.clearService()
    }
}
