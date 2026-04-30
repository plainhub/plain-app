package com.ismartcoding.plain.services

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.events.KeepAwakeChangedEvent
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.events.PowerDisconnectedEvent
import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.preferences.KeepAwakePreference
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.wifiManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private const val CHECK_INTERVAL_MS = 60_000L
private const val INACTIVITY_TIMEOUT_MS = 30 * 60_000L

/**
 * Manages PARTIAL_WAKE_LOCK and WIFI_MODE_FULL_HIGH_PERF for the HTTP server lifecycle.
 *
 * Business rules (see docs/wake-lock-wifi-lock.md):
 *  - Locks are acquired when the service starts; released when it stops.
 *  - USB connected or KeepAwake preference enabled  →  locks held indefinitely.
 *  - Otherwise a 30-minute inactivity timer is active. Each authenticated web request
 *    resets the window by updating [lastActivityMs]; the timer loop is never restarted.
 *  - On USB disconnect or KeepAwake disabled  →  inactivity timer starts from now.
 *  - App returning to foreground re-acquires released locks and resets the window.
 */
internal class HttpServerLockManager(private val context: Context) {

    private val wakeLock: PowerManager.WakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "${BuildConfig.APPLICATION_ID}:http_server",
    )
//    private val wifiLock: WifiManager.WifiLock = wifiManager.createWifiLock(
//        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
//        "${BuildConfig.APPLICATION_ID}:http_server",
//    )

    @Volatile private var lastActivityMs: Long = System.currentTimeMillis()
    @Volatile private var keepAwake: Boolean = true

    private var inactivityJob: Job? = null
    private var eventJob: Job? = null

    fun start() {
        acquireLocksOnly()
        eventJob = coIO {
            keepAwake = KeepAwakePreference.getAsync(context)
            scheduleInactivityTimer()
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is WebRequestReceivedEvent -> lastActivityMs = System.currentTimeMillis()
                    is WindowFocusChangedEvent -> if (event.hasFocus) {
                        lastActivityMs = System.currentTimeMillis()
                        acquireLocks()
                    }
                    is PowerConnectedEvent -> {
                        inactivityJob?.cancel()
                        inactivityJob = null
                        acquireLocks()
                    }
                    is PowerDisconnectedEvent -> {
                        lastActivityMs = System.currentTimeMillis()
                        scheduleInactivityTimer()
                    }
                    is KeepAwakeChangedEvent -> {
                        keepAwake = event.enabled
                        if (keepAwake) {
                            inactivityJob?.cancel()
                            inactivityJob = null
                        } else {
                            lastActivityMs = System.currentTimeMillis()
                            scheduleInactivityTimer()
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        eventJob?.cancel()
        eventJob = null
        inactivityJob?.cancel()
        inactivityJob = null
        releaseLocks()
    }

    private fun acquireLocksOnly() {
        runCatching {
            if (!wakeLock.isHeld) { wakeLock.acquire(); LogCat.d("WakeLock acquired") }
        }.onFailure { LogCat.e("WakeLock acquire failed: ${it.message}") }
//        runCatching {
//            if (!wifiLock.isHeld) { wifiLock.acquire(); LogCat.d("WifiLock acquired") }
//        }.onFailure { LogCat.e("WifiLock acquire failed: ${it.message}") }
    }

    private fun acquireLocks() {
        acquireLocksOnly()
        scheduleInactivityTimer()
    }

    fun releaseLocks() {
        inactivityJob?.cancel()
        inactivityJob = null
        if (wakeLock.isHeld) { wakeLock.release(); LogCat.d("WakeLock released") }
//        if (wifiLock.isHeld) { wifiLock.release(); LogCat.d("WifiLock released") }
    }

    /**
     * Starts the inactivity timer if not already active.
     * When [keepAwake] is true or USB is connected the timer is cancelled (indefinite hold).
     * The loop checks elapsed time every [CHECK_INTERVAL_MS]; it never restarts—only
     * [lastActivityMs] is updated on each request, which slides the window forward.
     */
    private fun scheduleInactivityTimer() {
        if (keepAwake || PlugInControlReceiver.isUSBConnected(context)) {
            inactivityJob?.cancel()
            inactivityJob = null
            return
        }
        if (inactivityJob?.isActive == true) return
        inactivityJob = coIO {
            while (true) {
                delay(CHECK_INTERVAL_MS)
                if (System.currentTimeMillis() - lastActivityMs >= INACTIVITY_TIMEOUT_MS) {
                    LogCat.d("Inactivity timeout: releasing locks")
                    releaseLocks()
                    return@coIO
                }
            }
        }
    }
}
