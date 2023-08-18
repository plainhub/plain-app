package com.ismartcoding.plain.services

import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.coroutineScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.isUPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.server.application.ApplicationStopPreparing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HttpServerService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
        val notification = NotificationHelper.createServiceNotification(
            this,
            "com.ismartcoding.plain.action.stop_http_server",
            getString(R.string.api_service_is_running)
        )
        if (isUPlus()) {
            startForeground(1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            try {
                if (MainApp.instance.httpServer == null) {
                    MainApp.instance.httpServer = HttpServerManager.createHttpServer(MainApp.instance)
                    MainApp.instance.httpServer?.start(wait = true)
                    HttpServerManager.httpServerError = ""
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                MainApp.instance.httpServer = null
                HttpServerManager.httpServerError = ex.toString()
                LogCat.e(ex.toString())
            }
        }
    }

    fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        coIO {
            try {
                MainApp.instance.httpServer?.let { h ->
                    val environment = h.environment
                    environment.monitor.raise(ApplicationStopPreparing, environment)
                    environment.stop()
                }
                MainApp.instance.httpServer = null
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    companion object {
        var instance: HttpServerService? = null
    }
}