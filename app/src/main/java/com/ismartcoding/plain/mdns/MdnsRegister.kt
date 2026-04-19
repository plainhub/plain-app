package com.ismartcoding.plain.mdns

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkRequest
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

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var reregisterJob: Job? = null
    private var hotspotWatcher: MdnsHotspotWatcher? = null

    /** Snapshot of the interfaces at last successful registration ("wlan0:192.168.1.5"). */
    @Volatile private var lastRegisteredIfaces: Set<String> = emptySet()

    fun start() {
        if (networkCallback != null) return

        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        if (cm == null) {
            LogCat.e("ConnectivityManager unavailable; mDNS auto re-register disabled")
            return
        }

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                schedule("onAvailable")
            }

            override fun onLost(network: Network) {
                schedule("onLost")
            }

            // onCapabilitiesChanged is intentionally omitted: it fires for signal-
            // strength changes, bandwidth estimate updates, and validation state —
            // none of which affect the interface/IP set.  On some Samsung/MediaTek
            // devices it fires every few seconds, causing pointless mDNS restarts.

            override fun onLinkPropertiesChanged(
                network: Network,
                linkProperties: LinkProperties,
            ) {
                schedule("onLinkPropertiesChanged")
            }
        }

        runCatching {
            // Use a broad NetworkRequest so we get callbacks for ALL networks
            // (Wi-Fi, Ethernet, VLAN sub-interfaces, VPN) rather than only the
            // current default network.  This ensures mDNS is re-registered whenever
            // any interface comes up or changes IP — e.g. a VPN connecting while
            // Wi-Fi is already the default, or a VLAN assignment changing.
            val request = NetworkRequest.Builder().build()
            cm.registerNetworkCallback(request, networkCallback!!)
        }
            .onSuccess { LogCat.d("Registered network callback for mDNS re-register") }
            .onFailure {
                LogCat.e("Failed to register network callback: ${it.message}")
                networkCallback = null
            }

        hotspotWatcher = MdnsHotspotWatcher(appContext) { schedule("hotspotStateChanged") }.also { it.start() }
    }

    fun stop() {
        reregisterJob?.cancel()
        reregisterJob = null
        lastRegisteredIfaces = emptySet()

        hotspotWatcher?.stop()
        hotspotWatcher = null

        val callback = networkCallback ?: return
        networkCallback = null

        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching { cm.unregisterNetworkCallback(callback) }
            .onFailure { LogCat.e("Failed to unregister network callback: ${it.message}") }
    }

    private fun schedule(reason: String) {
        if (!isActive()) return

        reregisterJob?.cancel()
        reregisterJob = coIO {
            delay(2000) // debounce network churn
            if (!isActive()) return@coIO

            // Compare current interface set with last registration — skip if unchanged.
            val currentIfaces = candidateInterfaces()
                .map { (iface, ip) -> "${iface.name}:${ip.hostAddress}" }
                .toSet()

            // Network gone — tear down responder and reset so the next
            // network-up event triggers a fresh registration.
            if (currentIfaces.isEmpty()) {
                if (lastRegisteredIfaces.isNotEmpty()) {
                    LogCat.d("mDNS teardown ($reason): no interfaces, clearing registration state")
                    NsdHelper.unregisterService()
                    lastRegisteredIfaces = emptySet()
                }
                return@coIO
            }

            if (currentIfaces == lastRegisteredIfaces) return@coIO

            val hostname = hostnameProvider().trim()
            val httpPort = httpPortProvider()
            val httpsPort = httpsPortProvider()
            val httpOk = httpPort in 1..65535
            val httpsOk = httpsPort in 1..65535
            if (hostname.isEmpty() || (!httpOk && !httpsOk)) return@coIO

            LogCat.d("mDNS re-register ($reason): $currentIfaces")
            runCatching {
                NsdHelper.registerServices(
                    context = appContext,
                    httpPort = if (httpOk) httpPort else null,
                    httpsPort = if (httpsOk) httpsPort else null,
                )
            }
                .onSuccess { ok ->
                    if (ok) {
                        lastRegisteredIfaces = currentIfaces
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
