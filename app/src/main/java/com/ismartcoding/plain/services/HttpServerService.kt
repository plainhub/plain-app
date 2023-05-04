package com.ismartcoding.plain.services

import android.content.Intent
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.web.HttpServerManager
import kotlin.concurrent.thread

class HttpServerService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        thread {
            try {
                if (MainApp.instance.httpServer == null) {
                    MainApp.instance.httpServer = HttpServerManager.createHttpServer(MainApp.instance)
                    MainApp.instance.httpServer?.start(wait = true)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                LogCat.e(ex.toString())
            }
        }
        val notification = NotificationHelper.createServiceNotification(
            this,
            "com.ismartcoding.plain.action.stop_http_server",
            getString(R.string.api_service_is_running)
        )
        startForeground(1, notification)
        return START_STICKY
    }

    fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        coIO {
            try {
                Runtime.getRuntime().addShutdownHook(Thread {
                    MainApp.instance.httpServer?.stop(1000, 5000)
                    MainApp.instance.httpServer = null
                })
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    companion object {
        var instance: HttpServerService? = null
    }
}