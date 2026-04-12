package com.ismartcoding.plain.mdns

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.ismartcoding.lib.logcat.LogCat

/**
 * Listens for Android Wi-Fi AP (hotspot) state changes and invokes [onStateChanged].
 *
 * ConnectivityManager.NetworkCallback does not fire on the host device when hotspot
 * is toggled (the tethered network belongs to the clients, not the host). This watcher
 * fills the gap so MdnsReregistrar can restart the responder on the correct interfaces
 * whenever the hotspot interface (ap0 / wlan1) appears or disappears.
 *
 * The action and state constants are `@hide` in the public SDK; raw values are used
 * directly to avoid compilation errors on all target SDK versions.
 */
internal class MdnsHotspotWatcher(
    private val context: Context,
    private val onStateChanged: () -> Unit,
) {
    // android.net.wifi.WifiManager constants (all @hide; raw values are stable since API 1)
    private val apStateChangedAction = "android.net.wifi.WIFI_AP_STATE_CHANGED"
    private val extraApState = "wifi_state"
    private val apStateEnabled = 13
    private val apStateDisabled = 11

    private var receiver: BroadcastReceiver? = null

    fun start() {
        if (receiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val s = intent.getIntExtra(extraApState, -1)
                if (s == apStateEnabled || s == apStateDisabled) onStateChanged()
            }
        }
        val filter = IntentFilter(apStateChangedAction)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(r, filter)
            }
            receiver = r
            LogCat.d("MdnsHotspotWatcher started")
        }.onFailure { LogCat.e("MdnsHotspotWatcher start failed: ${it.message}") }
    }

    fun stop() {
        val r = receiver ?: return
        receiver = null
        runCatching { context.unregisterReceiver(r) }
            .onFailure { LogCat.e("MdnsHotspotWatcher stop failed: ${it.message}") }
    }
}
