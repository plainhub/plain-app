package com.ismartcoding.plain.services
import com.ismartcoding.plain.appContext

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import com.ismartcoding.plain.lib.ChannelEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.events.KeepAwakeChangedEvent
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.events.PowerDisconnectedEvent
import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.powerManager
import com.ismartcoding.plain.preferences.KeepAwakePreference
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val CHECK_INTERVAL_MS = 60_000L
private const val INACTIVITY_TIMEOUT_MS = 30 * 60_000L

/**
 * Manages the partial wake lock for the HTTP server lifecycle.
 *
 * Business rules (see docs/wake-lock-wifi-lock.md):
 *  - Locks are acquired when the service starts; released when it stops.
 *  - USB connected or KeepAwake preference enabled  →  locks held indefinitely.
 *  - Otherwise a 30-minute inactivity timer is active. Each authenticated web request
 *    resets the window by updating [lastActivityMs]; the timer loop is never restarted.
 *  - On USB disconnect or KeepAwake disabled  →  inactivity timer starts from now.
 *  - App returning to foreground re-acquires released locks and resets the window.
 */
internal class HttpServerLockManager(
    private val setLockHeld: (Boolean) -> Unit,
    private val isUSBConnected: () -> Boolean,
    private val readKeepAwake: suspend () -> Boolean,
    private val clock: () -> Long,
    private val events: Flow<ChannelEvent>,
    private val scope: CoroutineScope,
    private val checkIntervalMs: Long = CHECK_INTERVAL_MS,
    private val inactivityTimeoutMs: Long = INACTIVITY_TIMEOUT_MS,
) {
    constructor(context: Context) : this(
        setLockHeld = createLockUpdater(),
        isUSBConnected = { PlugInControlReceiver.isUSBConnected(context) },
        readKeepAwake = { KeepAwakePreference.getAsync() },
        clock = { SystemClock.elapsedRealtime() },
        events = Channel.sharedFlow,
        scope = CoroutineScope(Dispatchers.IO),
    )

    private var lastActivityMs: Long = clock()
    private var keepAwake: Boolean = true
    private var running = false
    private var generation = 0L

    private var inactivityJob: Job? = null
    private var eventJob: Job? = null

    @Synchronized
    fun start() {
        if (running) return
        running = true
        val startedGeneration = ++generation
        lastActivityMs = clock()
        acquireLocksOnly()
        eventJob = scope.launch {
            val initialKeepAwake = readKeepAwake()
            synchronized(this@HttpServerLockManager) {
                if (!running || startedGeneration != generation) return@launch
                keepAwake = initialKeepAwake
                scheduleInactivityTimer()
            }
            events.collect { event ->
                synchronized(this@HttpServerLockManager) {
                    if (!running || startedGeneration != generation) return@collect
                    when (event) {
                        is WebRequestReceivedEvent -> {
                            lastActivityMs = clock()
                            acquireLocks()
                        }
                        is WindowFocusChangedEvent -> if (event.hasFocus) {
                            lastActivityMs = clock()
                            acquireLocks()
                        }
                        is PowerConnectedEvent -> {
                            inactivityJob?.cancel()
                            inactivityJob = null
                            acquireLocks()
                        }
                        is PowerDisconnectedEvent -> {
                            lastActivityMs = clock()
                            scheduleInactivityTimer()
                        }
                        is KeepAwakeChangedEvent -> {
                            keepAwake = event.enabled
                            if (keepAwake) {
                                acquireLocks()
                            } else {
                                lastActivityMs = clock()
                                scheduleInactivityTimer()
                            }
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        running = false
        generation++
        eventJob?.cancel()
        eventJob = null
        inactivityJob?.cancel()
        inactivityJob = null
        releaseLocks()
    }

    private fun acquireLocksOnly() {
        setLockHeld(true)
    }

    private fun acquireLocks() {
        acquireLocksOnly()
        scheduleInactivityTimer()
    }

    private fun releaseLocks() {
        inactivityJob?.cancel()
        inactivityJob = null
        setLockHeld(false)
    }

    /**
     * Starts the inactivity timer if not already active.
     * When [keepAwake] is true or USB is connected the timer is cancelled (indefinite hold).
     * The loop checks elapsed time every [CHECK_INTERVAL_MS]; it never restarts—only
     * [lastActivityMs] is updated on each request, which slides the window forward.
     */
    private fun scheduleInactivityTimer() {
        if (keepAwake || isUSBConnected()) {
            inactivityJob?.cancel()
            inactivityJob = null
            return
        }
        if (inactivityJob?.isActive == true) return
        val startedGeneration = generation
        inactivityJob = scope.launch {
            while (isActive) {
                delay(checkIntervalMs)
                synchronized(this@HttpServerLockManager) {
                    if (!isActive || !running || startedGeneration != generation) return@launch
                    if (keepAwake || isUSBConnected()) return@launch
                    if (clock() - lastActivityMs >= inactivityTimeoutMs) {
                        releaseLocks()
                        return@launch
                    }
                }
            }
        }
    }

    companion object {
        private fun createLockUpdater(): (Boolean) -> Unit {
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${appContext.packageName}:http_server")
            return { held ->
                runCatching {
                    if (held && !wakeLock.isHeld) wakeLock.acquire()
                    if (!held && wakeLock.isHeld) wakeLock.release()
                }.onFailure { LogCat.e("WakeLock update failed: ${it.message}") }
            }
        }
    }
}
