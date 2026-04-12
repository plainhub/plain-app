package com.ismartcoding.plain.mdns

import android.content.Context
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import java.util.concurrent.atomic.AtomicBoolean

object NsdHelper {
    // Prevents concurrent starts from racing multicast lock/socket lifecycle.
    private val registering = AtomicBoolean(false)

    /**
     * Start mDNS hostname responder for the active web service.
     */
    fun registerServices(context: Context, httpPort: Int?, httpsPort: Int?): Boolean {
        if (!registering.compareAndSet(false, true)) {
            LogCat.d("registerServices already in progress, skipping")
            return false
        }
        try {
            return registerServicesInternal(context, httpPort, httpsPort)
        } finally {
            registering.set(false)
        }
    }

    private fun registerServicesInternal(context: Context, httpPort: Int?, httpsPort: Int?): Boolean {
        unregisterService()

        val hasAnyPort = (httpPort != null && httpPort > 0) || (httpsPort != null && httpsPort > 0)
        if (!hasAnyPort) {
            LogCat.e("No active web service port, skip mDNS responder start")
            return false
        }

        val hostname = TempData.mdnsHostname
        return MdnsHostResponder.start(context, hostname)
    }

    /**
     * Stop mDNS hostname responder.
     */
    fun unregisterService() {
        MdnsHostResponder.stop()
    }
}