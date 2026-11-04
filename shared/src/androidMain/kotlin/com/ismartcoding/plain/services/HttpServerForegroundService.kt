package com.ismartcoding.plain.services

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.ismartcoding.plain.app.AppIntents
import com.ismartcoding.plain.features.settings.ServiceState
import com.ismartcoding.plain.lib.UrlHelper
import com.ismartcoding.plain.ui.platform.notifications.NotificationHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.keep_online_in_background
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
        if (!Prefs.backgroundServiceEnabled.value) { stopSelf(); return START_NOT_STICKY }
        super.onStartCommand(intent, flags, startId)

        try {
            val notification = NotificationHelper.createServiceNotification(
                this,
                AppIntents.ACTION_DISABLE_BACKGROUND_MODE,
                LocaleHelper.getString(Res.string.keep_online_in_background),
                getHttpServerUrls()
            )

            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } catch (e: Exception) {
            LogCat.e("Failed to start foreground service: ${e.message}")
            HttpServerManager.foregroundServiceError.value = "${LocaleHelper.getString(Res.string.background_online_failed)} (${e.message})"
            HttpServerManager.foregroundServiceState.value = ServiceState.ERROR
            stopSelf()
            return START_NOT_STICKY
        }

        HttpServerManager.foregroundServiceState.value = ServiceState.ON

        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (HttpServerManager.foregroundServiceState.value != ServiceState.ERROR) {
            HttpServerManager.foregroundServiceState.value = ServiceState.OFF
        }
    }

    companion object {
        val notificationId: Int by lazy { NotificationHelper.generateId() }

        @Volatile
        var instance: HttpServerForegroundService? = null

        fun isRunning(): Boolean = instance != null

        fun getHttpServerUrls(): String {
            val hostname = Prefs.mdnsHostname.value
            val http = UrlHelper.buildUrl("http", hostname, Prefs.httpPort.value)
            val https = UrlHelper.buildUrl("https", hostname, Prefs.httpsPort.value)
            return "$http\n$https"
        }
    }
}
