package com.ismartcoding.plain.app

import android.content.Intent
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.settings.ServiceState
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.background_online_failed
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.services.HttpServerForegroundService
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

internal object HttpServerForegroundServiceManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun observe() = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        merge(
            Channel.sharedFlow.filter { event ->
                event is PermissionsResultEvent && event.has(Permission.POST_NOTIFICATIONS)
            },
            Prefs.backgroundServiceEnabled,
        ).collect { change ->
            try {
                if (change is PermissionsResultEvent && !Permission.POST_NOTIFICATIONS.isGranted()) {
                    stop()
                    Prefs.set(PrefKeys.BACKGROUND_SERVICE_ENABLED, false)
                } else {
                    if (!Prefs.backgroundServiceEnabled.value) stop()
                    else if (Permission.POST_NOTIFICATIONS.isGranted()) start()
                }
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (error: Exception) {
                LogCat.e("Apply background service setting", error)
                HttpServerManager.foregroundServiceError.value = "${LocaleHelper.getString(Res.string.background_online_failed)} (${error.message})"
            }
        }
    }

    private fun start() {
        if (HttpServerManager.foregroundServiceState.value == ServiceState.ON ||
            HttpServerManager.foregroundServiceState.value == ServiceState.STARTING) return
        HttpServerManager.foregroundServiceState.value = ServiceState.STARTING
        HttpServerManager.foregroundServiceError.value = ""
        try {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, HttpServerForegroundService::class.java),
            )
        } catch (error: Exception) {
            LogCat.e("Start background service", error)
            HttpServerManager.foregroundServiceError.value = "${LocaleHelper.getString(Res.string.background_online_failed)} (${error.message})"
            HttpServerManager.foregroundServiceState.value = ServiceState.ERROR
        }
    }

    private fun stop() {
        appContext.stopService(Intent(appContext, HttpServerForegroundService::class.java))
        HttpServerManager.foregroundServiceState.value = if (HttpServerForegroundService.isRunning()) {
            ServiceState.STOPPING
        } else {
            ServiceState.OFF
        }
        HttpServerManager.foregroundServiceError.value = ""
    }
}
