package com.ismartcoding.plain.web

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.receivers.HttpServerStopBroadcastReceiver
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
        startForeground(1, createNotification())
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, HttpServerStopBroadcastReceiver::class.java).apply {
                action = "com.ismartcoding.plain.action.stop_http_server"
            }, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_notification)
            setContentTitle(getString(R.string.app_name))
            setContentText(getString(R.string.api_service_is_running))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOnlyAlertOnce(true)
            setSilent(true)
            setWhen(System.currentTimeMillis())
            setAutoCancel(false)
            setContentIntent(NotificationHelper.createContentIntent(this@HttpServerService))
            addAction(-1, getString(R.string.stop_service), stopPendingIntent)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }.build()
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