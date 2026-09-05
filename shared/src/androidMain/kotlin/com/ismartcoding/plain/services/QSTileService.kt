package com.ismartcoding.plain.services
import com.ismartcoding.plain.appContext

import com.ismartcoding.plain.platform.LocaleHelper

import com.ismartcoding.plain.i18n.*

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.platform.stopHttpServiceAsync
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.preferences.ServicePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

private val appIconDrawableId: Int by lazy {
    appContext.resources.getIdentifier("app_icon", "drawable", appContext.packageName)
}

class QSTileService : TileService() {
    private var stateEventJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun setState(state: Int) {
        if (state == Tile.STATE_INACTIVE) {
            qsTile?.state = Tile.STATE_INACTIVE
            qsTile?.label = LocaleHelper.getString(Res.string.app_name)
            qsTile?.icon = Icon.createWithResource(applicationContext, appIconDrawableId)
        } else if (state == Tile.STATE_ACTIVE) {
            qsTile?.state = Tile.STATE_ACTIVE
            qsTile?.label = LocaleHelper.getString(Res.string.app_name)
            qsTile?.icon = Icon.createWithResource(applicationContext, appIconDrawableId)
        }

        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()

        val serviceRef = WeakReference(this)

        // Collect the server-state source of truth: the flow replays the
        // current value immediately (replacing the old one-shot health probe)
        // and delivers every subsequent transition.
        stateEventJob?.cancel()
        stateEventJob = serviceScope.launch {
            HttpServerManager.serverState.collect { state ->
                val tileState = when (state) {
                    HttpServerState.ON -> Tile.STATE_ACTIVE
                    else -> Tile.STATE_INACTIVE
                }
                serviceRef.get()?.setState(tileState)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()

        // Cancel state subscription to avoid leaking the service instance
        stateEventJob?.cancel()
        stateEventJob = null
    }

    override fun onDestroy() {
        // Ensure all references are released when the service is destroyed
        stateEventJob?.cancel()
        stateEventJob = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        when (qsTile.state) {
            Tile.STATE_INACTIVE -> {
                // Start the service directly
                qsTile?.state = Tile.STATE_UNAVAILABLE
                qsTile?.updateTile()

                // Start HttpServerService directly without launching MainActivity.
                // Going through MainActivity with an intent extra was unsafe because
                // MainActivity is exported and the extra could be triggered by any
                // external caller (e.g. `adb shell am start --ez start_web_service true`).
                serviceScope.launch(Dispatchers.IO) {
                    val appContext = applicationContext
                    ServicePreference.putAsync(true)
                    ContextCompat.startForegroundService(
                        appContext,
                        Intent(appContext, HttpServerService::class.java),
                    )
                }
            }

            Tile.STATE_ACTIVE -> {
                // Stop service
                qsTile?.state = Tile.STATE_UNAVAILABLE
                qsTile?.updateTile()

                serviceScope.launch(Dispatchers.IO) {
                    ServicePreference.putAsync(false)
                    stopHttpServiceAsync()
                    withContext(Dispatchers.Main.immediate) {
                        setState(Tile.STATE_INACTIVE)
                    }
                }
            }
        }
    }


}
