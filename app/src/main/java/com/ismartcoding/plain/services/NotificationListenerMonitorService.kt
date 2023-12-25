package com.ismartcoding.plain.services

import android.app.ActivityManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import com.ismartcoding.lib.extensions.getSystemServiceCompat
import com.ismartcoding.lib.logcat.LogCat

class NotificationListenerMonitorService : Service() {
    override fun onCreate() {
        super.onCreate()
        LogCat.d("onCreate() called")
        ensureCollectorRunning()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    private fun ensureCollectorRunning() {
        val collectorComponent = ComponentName(this, NotificationListenerService::class.java)
        LogCat.d("ensureCollectorRunning collectorComponent: $collectorComponent")
        val manager = getSystemServiceCompat(ActivityManager::class.java)
        var collectorRunning = false
        val runningServices = manager.getRunningServices(Int.MAX_VALUE)
        if (runningServices == null) {
            LogCat.d("ensureCollectorRunning() runningServices is NULL")
            return
        }
        for (service in runningServices) {
            if (service.service == collectorComponent) {
                if (service.pid == android.os.Process.myPid()) {
                    collectorRunning = true
                }
            }
        }
        if (collectorRunning) {
            LogCat.d("ensureCollectorRunning: collector is running")
            return
        }
        LogCat.d("ensureCollectorRunning: collector not running, reviving...")
        toggleNotificationListenerService()
    }

    private fun toggleNotificationListenerService() {
        LogCat.d("toggleNotificationListenerService() called")
        val thisComponent = ComponentName(this, NotificationListenerService::class.java)
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}