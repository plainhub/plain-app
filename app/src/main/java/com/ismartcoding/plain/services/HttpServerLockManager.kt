package com.ismartcoding.plain.services

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.events.PowerDisconnectedEvent
import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.wifiManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private const val INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000L

internal class HttpServerLockManager(private val context: Context) {
    private val wakeLock: PowerManager.WakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "${BuildConfig.APPLICATION_ID}:http_server",
    )
    private val wifiLock: WifiManager.WifiLock = wifiManager.createWifiLock(
        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
        "${BuildConfig.APPLICATION_ID}:http_server",
    )
    private var inactivityJob: Job? = null
    private var eventJob: Job? = null

    fun start() {
        acquireLocks()
        eventJob = coIO {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is WebRequestReceivedEvent -> resetInactivityTimer()
                    is WindowFocusChangedEvent -> if (event.hasFocus) acquireLocks()
                    is PowerConnectedEvent -> acquireLocks()
                    is PowerDisconnectedEvent -> resetInactivityTimer()
                }
            }
        }
    }

    fun stop() {
        eventJob?.cancel()
        eventJob = null
        releaseLocks()
    }

    private fun acquireLocks() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire()
            LogCat.d("WakeLock acquired")
        }
        if (!wifiLock.isHeld) {
            wifiLock.acquire()
            LogCat.d("WifiLock acquired")
        }
        resetInactivityTimer()
    }

    fun releaseLocks() {
        inactivityJob?.cancel()
        inactivityJob = null
        if (wakeLock.isHeld) {
            wakeLock.release()
            LogCat.d("WakeLock released")
        }
        if (wifiLock.isHeld) {
            wifiLock.release()
            LogCat.d("WifiLock released")
        }
    }

    private fun resetInactivityTimer() {
        if (!wakeLock.isHeld) return
        inactivityJob?.cancel()
        inactivityJob = null
        if (PlugInControlReceiver.isUSBConnected(context)) return
        inactivityJob = coIO {
            delay(INACTIVITY_TIMEOUT_MS)
            LogCat.d("Inactivity timeout: releasing locks")
            releaseLocks()
        }
    }
}
