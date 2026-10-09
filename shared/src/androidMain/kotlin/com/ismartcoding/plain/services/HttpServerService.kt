package com.ismartcoding.plain.services

import com.ismartcoding.plain.preferences.*

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.keep_online_in_background
import com.ismartcoding.plain.i18n.background_online_failed
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.HttpServerManager

class HttpServerService : LifecycleService() {
    private var lockManager: HttpServerLockManager? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureDefaultChannel()

        lockManager = HttpServerLockManager(this).also { it.start() }
    }

    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val t0 = System.currentTimeMillis()
        // In that case we flag it so the startup coroutine can delay before binding ports.
        if (!UserPrefs.service.value) { stopSelf(); return START_NOT_STICKY }
        super.onStartCommand(intent, flags, startId)

        try {
            val notification = NotificationHelper.createServiceNotification(
                this,
                AppIntents.ACTION_DISABLE_BACKGROUND_MODE,
                LocaleHelper.getString(Res.string.keep_online_in_background),
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
            HttpServerManager.backgroundError.value = LocaleHelper.getString(Res.string.background_online_failed)
            HttpServerManager.backgroundState.value = HttpServerState.ERROR
            stopSelf()
            return START_NOT_STICKY
        }

        HttpServerManager.backgroundState.value = HttpServerState.ON
        HttpServerManager.ensureStarted()
        LogCat.d("HttpServerService.onStartCommand: foreground ready ${System.currentTimeMillis() - t0}ms")

        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        lockManager?.stop()
        lockManager = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (HttpServerManager.backgroundState.value != HttpServerState.ERROR) {
            HttpServerManager.backgroundState.value = HttpServerState.OFF
        }
    }

    companion object {
        @Volatile
        var instance: HttpServerService? = null

        fun isRunning(): Boolean = instance != null
    }
}
