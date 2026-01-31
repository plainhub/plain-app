package com.ismartcoding.plain.web

import android.content.Context
import android.net.ConnectivityManager
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Watches network changes and re-registers NSD/JmDNS to keep mDNS discovery accurate across
 * VPN/Wi-Fi/cellular transitions.
 */
class MdnsNsdReregistrar(
    context: Context,
    private val isActive: () -> Boolean,
    private val hostnameProvider: () -> String,
    private val httpPortProvider: () -> Int,
    private val httpsPortProvider: () -> Int,
) {
    private val appContext: Context = context.applicationContext

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var reregisterJob: Job? = null

    fun start() {
        if (networkCallback != null) return

        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        if (cm == null) {
            LogCat.e("ConnectivityManager unavailable; mDNS auto re-register disabled")
            return
        }

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                schedule("onAvailable")
            }

            override fun onLost(network: android.net.Network) {
                schedule("onLost")
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: android.net.NetworkCapabilities,
            ) {
                schedule("onCapabilitiesChanged")
            }

            override fun onLinkPropertiesChanged(
                network: android.net.Network,
                linkProperties: android.net.LinkProperties,
            ) {
                schedule("onLinkPropertiesChanged")
            }
        }

        runCatching {
            cm.registerDefaultNetworkCallback(networkCallback!!)
        }
            .onSuccess { LogCat.d("Registered network callback for mDNS re-register") }
            .onFailure {
                LogCat.e("Failed to register network callback: ${it.message}")
                networkCallback = null
            }
    }

    fun stop() {
        reregisterJob?.cancel()
        reregisterJob = null

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
            delay(1200) // debounce network churn (VPN/Wi-Fi toggles can fire multiple callbacks)

            val maxAttempts = 5
            repeat(maxAttempts) { attemptIndex ->
                if (!isActive()) return@coIO
                if (attemptIndex > 0) delay(1500)

                val hostname = hostnameProvider().trim()
                val httpPort = httpPortProvider()
                val httpsPort = httpsPortProvider()

                val httpOk = httpPort in 1..65535
                val httpsOk = httpsPort in 1..65535
                if (hostname.isEmpty() || (!httpOk && !httpsOk)) {
                    LogCat.e(
                        "Skip mDNS/NSD re-register (attempt ${attemptIndex + 1}/$maxAttempts): " +
                            "hostname='$hostname', httpPort=$httpPort, httpsPort=$httpsPort"
                    )
                    return@repeat
                }

                LogCat.d("Network changed ($reason), re-registering NSD/JmDNS (attempt ${attemptIndex + 1}/$maxAttempts)")

                runCatching {
                    NsdHelper.unregisterService()
                    delay(200)
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
                        LogCat.e("mDNS/NSD re-register failed: ${it.message}")
                    }
            }
        }
    }
}
