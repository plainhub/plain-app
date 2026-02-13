package com.ismartcoding.plain.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.PortHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.MdnsNsdReregistrar
import com.ismartcoding.plain.web.NsdHelper
import com.ismartcoding.plain.features.Permission
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay

class HttpServerService : LifecycleService() {
    private var serverState: HttpServerState = HttpServerState.OFF
    private var mdnsNsdReregistrar: MdnsNsdReregistrar? = null

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureDefaultChannel()

        mdnsNsdReregistrar = MdnsNsdReregistrar(
            context = this,
            isActive = { serverState == HttpServerState.ON },
            hostnameProvider = { TempData.mdnsHostname },
            httpPortProvider = { TempData.httpPort },
            httpsPortProvider = { TempData.httpsPort },
        ).also { it.start() }
        
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        coIO {
                            startHttpServerAsync()
                        }
                    }

                    Lifecycle.Event.ON_STOP -> coIO {
                        stopHttpServerAsync()
                    }

                    else -> Unit
                }
            }
        })
    }
    
    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        try {
            val notification = NotificationHelper.createServiceNotification(
                this,
                "${BuildConfig.APPLICATION_ID}.action.stop_http_server",
                getString(R.string.api_service_is_running),
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
        
        return START_STICKY
    }

    private suspend fun startHttpServerAsync() {
        LogCat.d("startHttpServer")
        serverState = HttpServerState.STARTING
        sendEvent(HttpServerStateChangedEvent(serverState))

        HttpServerManager.portsInUse.clear()
        HttpServerManager.httpServerError = ""

        // Stop any previously running server instance and wait for ports to be released
        HttpServerManager.stopPreviousServer()
        if (PortHelper.isPortInUse(TempData.httpPort) || PortHelper.isPortInUse(TempData.httpsPort)) {
            LogCat.d("Ports still in use after stopping previous server, waiting...")
            HttpServerManager.waitForPortsAvailable(TempData.httpPort, TempData.httpsPort)
        }

        // Try starting server with retry on BindException
        var serverStarted = false
        var lastError: Exception? = null
        val maxRetries = 3
        for (attempt in 1..maxRetries) {
            try {
                val server = HttpServerManager.createHttpServerAsync(MainApp.instance)
                server.start(wait = false)
                HttpServerManager.server = server
                serverStarted = true
                break
            } catch (ex: Exception) {
                lastError = ex
                LogCat.e("Server start attempt $attempt/$maxRetries failed: ${ex.message}")
                // If it's a BindException, the old socket may not be fully released yet
                if (ex is java.net.BindException || ex.cause is java.net.BindException) {
                    if (attempt < maxRetries) {
                        HttpServerManager.stopPreviousServer()
                        HttpServerManager.waitForPortsAvailable(TempData.httpPort, TempData.httpsPort, maxWaitMs = 3000)
                    }
                } else {
                    break // non-port error, no point retrying
                }
            }
        }

        if (!serverStarted) {
            HttpServerManager.httpServerError = lastError?.toString() ?: ""
            HttpServerManager.httpServerError = if (HttpServerManager.httpServerError.isNotEmpty()) {
                LocaleHelper.getString(R.string.http_server_failed) + " (${HttpServerManager.httpServerError})"
            } else {
                LocaleHelper.getString(R.string.http_server_failed)
            }
            serverState = HttpServerState.ERROR
            sendEvent(HttpServerStateChangedEvent(serverState))
            PNotificationListenerService.toggle(this, false)
            return
        }

        delay(500) // brief wait for server to fully bind ports
        val checkResult = HttpServerManager.checkServerAsync()
        if (checkResult.websocket && checkResult.http) {
            HttpServerManager.httpServerError = ""
            HttpServerManager.portsInUse.clear()
            NsdHelper.registerServices(this, httpPort = TempData.httpPort, httpsPort = TempData.httpsPort)
            serverState = HttpServerState.ON
            sendEvent(HttpServerStateChangedEvent(serverState))
            PNotificationListenerService.toggle(this, Permission.NOTIFICATION_LISTENER.isEnabledAsync(this))
        } else {
            if (!checkResult.http) {
                if (PortHelper.isPortInUse(TempData.httpPort)) {
                    HttpServerManager.portsInUse.add(TempData.httpPort)
                }

                if (PortHelper.isPortInUse(TempData.httpsPort)) {
                    HttpServerManager.portsInUse.add(TempData.httpsPort)
                }
            }
            HttpServerManager.httpServerError = if (HttpServerManager.portsInUse.isNotEmpty()) {
                LocaleHelper.getStringF(
                    if (HttpServerManager.portsInUse.size > 1) {
                        R.string.http_port_conflict_errors
                    } else {
                        R.string.http_port_conflict_error
                    }, "port", HttpServerManager.portsInUse.joinToString(", ")
                )
            } else if (HttpServerManager.httpServerError.isNotEmpty()) {
                LocaleHelper.getString(R.string.http_server_failed) + " (${HttpServerManager.httpServerError})"
            } else {
                LocaleHelper.getString(R.string.http_server_failed)
            }

            serverState = HttpServerState.ERROR
            sendEvent(HttpServerStateChangedEvent(serverState))
            PNotificationListenerService.toggle(this, false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mdnsNsdReregistrar?.stop()
        mdnsNsdReregistrar = null
        // Ensure NSD service is unregistered
        NsdHelper.unregisterService()
        HttpServerManager.server = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private suspend fun stopHttpServerAsync() {
        LogCat.d("stopHttpServer")
        try {
            // Unregister NSD service
            NsdHelper.unregisterService()
            
            val client = HttpClientManager.httpClient()
            val r = client.get(UrlHelper.getShutdownUrl())
            if (r.status == HttpStatusCode.Gone) {
                LogCat.d("http server is stopped")
            }
        } catch (ex: Exception) {
            LogCat.e("Graceful shutdown failed: ${ex.message}")
            // Fallback: force stop via stored server reference
            try {
                HttpServerManager.server?.stop(500, 1000)
                LogCat.d("Server force-stopped via stored reference")
            } catch (e: Exception) {
                LogCat.e("Force stop also failed: ${e.message}")
            }
        }
        HttpServerManager.server = null
        PNotificationListenerService.toggle(this, false)

        serverState = HttpServerState.OFF
    }
}
