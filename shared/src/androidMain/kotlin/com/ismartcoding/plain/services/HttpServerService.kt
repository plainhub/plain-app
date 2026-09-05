package com.ismartcoding.plain.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.features.sms.SmsProviderObserver
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.plainapp_service_is_running
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.mdns.MdnsRegister
import com.ismartcoding.plain.mdns.NsdHelper
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.cancelMmsPolling
import com.ismartcoding.plain.platform.startHttpServerAsync
import com.ismartcoding.plain.platform.stopHttpServerCoreAsync
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.httpServer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class HttpServerService : LifecycleService() {
    // Server lifecycle state lives in HttpServerManager.serverState (single
    // source of truth); this service keeps no local copy.
    var mdnsRegister: MdnsRegister? = null
    private var serverJob: Job? = null
    private var lockManager: HttpServerLockManager? = null

    // true when this instance was created by START_STICKY (system restart), not by user
    private var isStickyRestart: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()

        lockManager = HttpServerLockManager(this)
        mdnsRegister = MdnsRegister(
            context = this,
            isActive = { HttpServerManager.serverState.value == HttpServerState.ON },
            hostnameProvider = { TempData.mdnsHostname },
            httpPortProvider = { TempData.httpPort.value },
            httpsPortProvider = { TempData.httpsPort.value },
        ).also { it.start() }

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        lockManager?.start()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        lockManager?.stop()
                        serverJob?.cancel()
                        serverJob = coIO {
                            stopHttpServerAsync()
                        }
                    }

                    else -> Unit
                }
            }
        })
    }

    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent == null means the system restarted this service via START_STICKY after killing it.
        // In that case we flag it so the startup coroutine can delay before binding ports.
        if (intent == null) isStickyRestart = true
        super.onStartCommand(intent, flags, startId)

        try {
            val notification = NotificationHelper.createServiceNotification(
                this,
                AppIntents.ACTION_STOP_HTTP_SERVER,
                LocaleHelper.getString(Res.string.plainapp_service_is_running),
                HttpServerManager.getNotificationContent()
            )

            try {
                ServiceCompat.startForeground(
                    this,
                    HttpServerManager.notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } catch (e: Exception) {
                LogCat.e("Error starting foreground service with specialUse: ${e.message}")
                try {
                    ServiceCompat.startForeground(
                        this,
                        HttpServerManager.notificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e2: Exception) {
                    LogCat.e("Error starting foreground service with dataSync: ${e2.message}")
                    startForeground(HttpServerManager.notificationId, notification)
                }
            }
        } catch (e: Exception) {
            LogCat.e("Failed to start foreground service: ${e.message}")
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        ensureServerRunning()

        return START_STICKY
    }

    /**
     * Single idempotent entry point for "ensure the server is running".
     * onStartCommand is delivered for every start request (first start, user
     * retry, QS tile, ADB, state sync, START_STICKY restart), unlike the
     * ON_START lifecycle event which fires only once per service instance —
     * relying on ON_START made later requests dead letters and left the UI
     * stuck in STARTING after a failed start.
     */
    private fun ensureServerRunning() {
        if (HttpServerManager.serverState.value == HttpServerState.ON || serverJob?.isActive == true) return
        serverJob = coIO {
            if (isStickyRestart) {
                // Give previous Ktor instance time to release its TCP ports
                // before we try to bind again. Without this, the rapid
                // START_STICKY kill/restart cycle on OnePlus/ColorOS causes
                // a port-in-use loop that overwhelms the system.
                LogCat.d("START_STICKY restart — waiting 5s for port release")
                delay(5_000)
                isStickyRestart = false
            }
            startServer()
        }
    }

    private suspend fun startServer() {
        try {
            startHttpServerAsync()
        } catch (ex: Exception) {
            // Ensure a terminal state even if the orchestrator throws before
            // recording its own ERROR state.
            LogCat.e("Server start failed unexpectedly: ${ex.message}")
            HttpServerManager.serverState.value = HttpServerState.ERROR
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // User swiped away the app from recents; stop server immediately to release ports.
        NsdHelper.unregisterService()
        try {
            httpServer?.stop(500, 1000)
        } catch (e: Exception) {
            LogCat.e("Error stopping server on task removed: ${e.message}")
        } finally {
            PeerStatusManager.stop()
            SmsProviderObserver.stop()
            SmsHelper.stopSmsSendTracking()
            cancelMmsPolling()
            httpServer = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        instance = null
        serverJob?.cancel()
        serverJob = null
        super.onDestroy()
        lockManager?.stop()
        lockManager = null
        mdnsRegister?.stop()
        mdnsRegister = null
        // Ensure mDNS responder is stopped
        NsdHelper.unregisterService()
        PeerStatusManager.stop()
        SmsProviderObserver.stop()
        SmsHelper.stopSmsSendTracking()
        cancelMmsPolling()
        try {
            httpServer?.stop(0, 1000)
        } catch (_: Exception) {
        }
        httpServer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        // The engine is stopped and this instance's start job was cancelled
        // above; a state still mid-transition can never complete (a queued
        // restart arrives as a fresh service instance with its own
        // orchestration). Record the terminal state so collectors are not
        // stranded in STARTING/STOPPING. Terminal states (ON with the engine
        // just stopped by onTaskRemoved, ERROR) are left to the health sync.
        if (HttpServerManager.serverState.value.isProcessing()) {
            HttpServerManager.serverState.value = HttpServerState.OFF
        }
    }

    private suspend fun stopHttpServerAsync() = withIO {
        LogCat.d("stopHttpServer")
        // Shared stop body handles /shutdown, engine stop, mDNS/peer-status/
        // notification-listener side effects, state clear and the OFF record.
        stopHttpServerCoreAsync()
    }

    companion object {
        @Volatile
        var instance: HttpServerService? = null

        fun isRunning(): Boolean = instance != null
    }
}
