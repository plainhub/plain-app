package com.ismartcoding.plain.mdns

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
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

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                schedule("onCapabilitiesChanged")
            }

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
            delay(2000) // debounce network churn (VPN/Wi-Fi toggles can fire multiple callbacks)

            // Keep retries low to avoid churning mDNS registration under unstable networks.
            val maxAttempts = 3
            repeat(maxAttempts) { attemptIndex ->
                if (!isActive()) return@coIO
                if (attemptIndex > 0) delay(3000)

                val hostname = hostnameProvider().trim()
                val httpPort = httpPortProvider()
                val httpsPort = httpsPortProvider()

                val httpOk = httpPort in 1..65535
                val httpsOk = httpsPort in 1..65535
                if (hostname.isEmpty() || (!httpOk && !httpsOk)) {
                    LogCat.e(
                        "Skip mDNS re-register (attempt ${attemptIndex + 1}/$maxAttempts): " +
                            "hostname='$hostname', httpPort=$httpPort, httpsPort=$httpsPort"
                    )
                    return@repeat
                }

                LogCat.d("Network changed ($reason), re-registering mDNS (attempt ${attemptIndex + 1}/$maxAttempts)")

                runCatching {
                    // registerServices() handles stop/start internally.
                    NsdHelper.registerServices(
                        context = appContext,
                        httpPort = if (httpOk) httpPort else null,
                        httpsPort = if (httpsOk) httpsPort else null,
                    )
                }
                    .onSuccess { ok ->
                        if (ok) return@coIO
                    }
                    .onFailure {
                        LogCat.e("mDNS re-register failed: ${it.message}")
                    }
            }
        }
    }
}
