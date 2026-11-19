package com.ismartcoding.plain.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.ismartcoding.plain.lib.UrlHelper
import com.ismartcoding.plain.ui.platform.notifications.NotificationHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.app_name
import com.ismartcoding.plain.i18n.background_online_failed
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.Prefs

class HttpServerForegroundService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()
    }

    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        try {
            ServiceCompat.startForeground(
                this,
                notificationId,
                createNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } catch (error: Exception) {
            reportStartFailure(error)
            stopSelf()
            return START_NOT_STICKY
        }
        HttpServerManager.foregroundServiceError.value = ""
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        val notificationId: Int by lazy { NotificationHelper.generateId() }

        @Volatile
        private var instance: HttpServerForegroundService? = null

        fun isRunning(): Boolean = instance != null

        fun start(context: Context, refreshNotification: Boolean = false) {
            if (isRunning() && !refreshNotification) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, HttpServerForegroundService::class.java))
            } catch (error: Exception) {
                reportStartFailure(error)
            }
        }

        private fun reportStartFailure(error: Exception) {
            LogCat.e("Start background service", error)
            HttpServerManager.foregroundServiceError.value =
                "${LocaleHelper.getString(Res.string.background_online_failed)} (${error.message})"
        }

        fun createNotification(context: Context) = NotificationHelper.createServiceNotification(
            context = context,
            title = LocaleHelper.getString(Res.string.app_name),
            description = getHttpServerUrls(),
        )

        private fun getHttpServerUrls(): String {
            val hostname = Prefs.mdnsHostname.value
            val http = UrlHelper.buildUrl("http", hostname, Prefs.httpPort.value)
            val https = UrlHelper.buildUrl("https", hostname, Prefs.httpsPort.value)
            return "$http\n$https"
        }
    }
}
