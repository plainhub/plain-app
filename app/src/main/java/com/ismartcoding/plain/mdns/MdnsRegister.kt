package com.ismartcoding.plain.mdns

import android.content.Context
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Watches network changes and re-registers mDNS to keep discovery accurate across
 * VPN/Wi-Fi/cellular transitions.
 */
class MdnsRegister(
    context: Context,
    private val isActive: () -> Boolean,
    private val hostnameProvider: () -> String,
    private val httpPortProvider: () -> Int,
    private val httpsPortProvider: () -> Int,
) {
    private val appContext: Context = context.applicationContext

    private var reregisterJob: Job? = null
    private var hotspotWatcher: MdnsHotspotWatcher? = null

    /** Snapshot of the interfaces at last successful registration ("wlan0:192.168.1.5"). */
    @Volatile
    private var lastRegisteredIfaces: Set<String> = emptySet()

    fun start() {
        hotspotWatcher = MdnsHotspotWatcher(appContext) { schedule("hotspotStateChanged") }.also { it.start() }
    }

    fun stop() {
        reregisterJob?.cancel()
        reregisterJob = null
        lastRegisteredIfaces = emptySet()

        hotspotWatcher?.stop()
        hotspotWatcher = null
    }

    fun schedule(reason: String) {
        if (!isActive()) return

        // Fast-path: if LAN interfaces haven't changed and mDNS is already registered,
        // don't cancel an in-flight debounce job.  This prevents cellular
        // onLinkPropertiesChanged storms (DNS updates, CLAT/NAT64 changes) from
        // continuously resetting the debounce timer and blocking re-registration.
        val currentIfaces = candidateInterfaces()
            .map { (iface, ip) -> "${iface.name}:${ip.hostAddress}" }
            .toSet()
        if (currentIfaces.isNotEmpty() && currentIfaces == lastRegisteredIfaces && reregisterJob?.isActive == true) {
            return
        }

        reregisterJob?.cancel()
        reregisterJob = coIO {
            delay(2000) // debounce network churn
            if (!isActive()) return@coIO

            // Re-fetch — interfaces may have changed since fast-path check above.
            val freshIfaces = candidateInterfaces()
                .map { (iface, ip) -> "${iface.name}:${ip.hostAddress}" }
                .toSet()

            // Network gone — tear down responder and reset so the next
            // network-up event triggers a fresh registration.
            if (freshIfaces.isEmpty()) {
                if (lastRegisteredIfaces.isNotEmpty()) {
                    LogCat.d("mDNS teardown ($reason): no interfaces, clearing registration state")
                    NsdHelper.unregisterService()
                    lastRegisteredIfaces = emptySet()
                }
                return@coIO
            }

            if (freshIfaces == lastRegisteredIfaces) return@coIO

            val hostname = hostnameProvider().trim()
            val httpPort = httpPortProvider()
            val httpsPort = httpsPortProvider()
            val httpOk = httpPort in 1..65535
            val httpsOk = httpsPort in 1..65535
            if (hostname.isEmpty() || (!httpOk && !httpsOk)) return@coIO

            LogCat.d("mDNS re-register ($reason): $freshIfaces")
            runCatching {
                NsdHelper.registerServices(
                    context = appContext,
                    httpPort = if (httpOk) httpPort else null,
                    httpsPort = if (httpsOk) httpsPort else null,
                )
            }
                .onSuccess { ok ->
                    if (ok) {
                        lastRegisteredIfaces = freshIfaces
                    } else {
                        LogCat.e("mDNS re-register returned false, resetting registration state")
                        lastRegisteredIfaces = emptySet()
                    }
                }
                .onFailure {
                    LogCat.e("mDNS re-register failed: ${it.message}")
                    lastRegisteredIfaces = emptySet()
                }
        }
    }
}
