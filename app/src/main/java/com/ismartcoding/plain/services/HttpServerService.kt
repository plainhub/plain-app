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
import com.ismartcoding.plain.web.NsdHelper
import com.ismartcoding.plain.features.Permission
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay

class HttpServerService : LifecycleService() {
    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureDefaultChannel()
        
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
        sendEvent(HttpServerStateChangedEvent(HttpServerState.STARTING))
        try {
            HttpServerManager.portsInUse.clear()
            HttpServerManager.httpServerError = ""
            HttpServerManager.createHttpServerAsync(MainApp.instance).start(wait = false)
        } catch (ex: Exception) {
            ex.printStackTrace()
            LogCat.e(ex.toString())
            HttpServerManager.httpServerError = ex.toString()
        }

        delay(1000) // make sure server is running
        val checkResult = HttpServerManager.checkServerAsync()
        if (checkResult.websocket && checkResult.http) {
            HttpServerManager.httpServerError = ""
            HttpServerManager.portsInUse.clear()
            NsdHelper.registerService(this, TempData.httpPort)
            sendEvent(HttpServerStateChangedEvent(HttpServerState.ON))
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

            sendEvent(HttpServerStateChangedEvent(HttpServerState.ERROR))
            PNotificationListenerService.toggle(this, false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure NSD service is unregistered
        NsdHelper.unregisterService()
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
            LogCat.e(ex.toString())
            ex.printStackTrace()
        }
        PNotificationListenerService.toggle(this, false)
    }
}
