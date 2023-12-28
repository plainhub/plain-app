package com.ismartcoding.plain.services

import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.PortHelper
import com.ismartcoding.lib.isUPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.StartHttpServerErrorEvent
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.server.application.ApplicationStopPreparing

class HttpServerService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureDefaultChannel()
        val notification =
            NotificationHelper.createServiceNotification(
                this,
                "${BuildConfig.APPLICATION_ID}.action.stop_http_server",
                getString(R.string.api_service_is_running),
            )
        val id = NotificationHelper.generateId()
        if (isUPlus()) {
            startForeground(id, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> startHttpServer()
                    Lifecycle.Event.ON_STOP -> stopHttpServer()
                    else -> Unit
                }
            }
        })
    }

    private fun startHttpServer() {
        coIO {
            try {
                if (HttpServerManager.httpServer == null) {
                    HttpServerManager.portsInUse.clear()
                    HttpServerManager.stoppedByUser = false
                    HttpServerManager.httpServerError = ""
                    HttpServerManager.httpServer = HttpServerManager.createHttpServer(MainApp.instance)
                    HttpServerManager.httpServer?.start(wait = true)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                HttpServerManager.httpServer = null
                HttpServerManager.httpServerError = ex.toString()
                if (PortHelper.isPortInUse(TempData.httpPort)) {
                    HttpServerManager.portsInUse.add(TempData.httpPort)
                }
                if (PortHelper.isPortInUse(TempData.httpsPort)) {
                    HttpServerManager.portsInUse.add(TempData.httpsPort)
                }
                sendEvent(StartHttpServerErrorEvent())
                LogCat.e(ex.toString())
            }
        }
    }

    private fun stopHttpServer() {
        coIO {
            try {
                HttpServerManager.httpServer?.let { h ->
                    val application = h.application
                    val environment = application.environment
                    application.monitor.raise(ApplicationStopPreparing, environment)
                    application.dispose()
                }
                HttpServerManager.httpServer = null
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
