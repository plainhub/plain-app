package com.ismartcoding.plain.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class QSTileService : TileService() {
    private var stateEventJob: Job? = null
    private var stateCheckJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun setState(state: Int) {
        if (state == Tile.STATE_INACTIVE) {
            qsTile?.state = Tile.STATE_INACTIVE
            qsTile?.label = getString(R.string.app_name)
            qsTile?.icon = Icon.createWithResource(applicationContext, R.drawable.app_icon)
        } else if (state == Tile.STATE_ACTIVE) {
            qsTile?.state = Tile.STATE_ACTIVE
            qsTile?.label = getString(R.string.app_name)
            qsTile?.icon = Icon.createWithResource(applicationContext, R.drawable.app_icon)
        }

        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()

        val serviceRef = WeakReference(this)

        // Listen for HTTP server state changes and keep a cancellable reference
        stateEventJob?.cancel()
        stateEventJob = receiveEventHandler<HttpServerStateChangedEvent> { event ->
            val tileState = when (event.state) {
                HttpServerState.ON -> Tile.STATE_ACTIVE
                HttpServerState.OFF -> Tile.STATE_INACTIVE
                HttpServerState.STARTING -> Tile.STATE_INACTIVE
                HttpServerState.STOPPING -> Tile.STATE_INACTIVE
                HttpServerState.ERROR -> Tile.STATE_INACTIVE
            }
            withContext(Dispatchers.Main.immediate) {
                serviceRef.get()?.setState(tileState)
            }
        }

        // Check current server state
        stateCheckJob?.cancel()
        stateCheckJob = serviceScope.launch(Dispatchers.IO) {
            try {
                // First check if webEnabled is true in TempData
                if (TempData.webEnabled) {
                    val checkResult = HttpServerManager.checkServerAsync()
                    if (checkResult.websocket && checkResult.http) {
                        withContext(Dispatchers.Main.immediate) {
                            serviceRef.get()?.setState(Tile.STATE_ACTIVE)
                        }
                    } else {
                        // Service should be running but isn't responding
                        LogCat.d("Web service enabled but not responding, setting inactive state")
                        withContext(Dispatchers.Main.immediate) {
                            serviceRef.get()?.setState(Tile.STATE_INACTIVE)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main.immediate) {
                        serviceRef.get()?.setState(Tile.STATE_INACTIVE)
                    }
                }
            } catch (e: Exception) {
                LogCat.e("Failed to check server state: ${e.message}")
                withContext(Dispatchers.Main.immediate) {
                    serviceRef.get()?.setState(Tile.STATE_INACTIVE)
                }
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()

        // Cancel event subscription to avoid leaking the service instance
        stateEventJob?.cancel()
        stateEventJob = null

        // Cancel any pending state check
        stateCheckJob?.cancel()
        stateCheckJob = null
    }

    override fun onDestroy() {
        // Ensure all references are released when the service is destroyed
        stateEventJob?.cancel()
        stateEventJob = null
        stateCheckJob?.cancel()
        stateCheckJob = null
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

                // Launch the app with unlockAndRun
                unlockAndRun {
                    val intent = Intent(MainApp.instance, Class.forName("com.ismartcoding.plain.ui.MainActivity"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("start_web_service", true)
                    startActivity(intent)
                }
            }

            Tile.STATE_ACTIVE -> {
                // Stop service
                qsTile?.state = Tile.STATE_UNAVAILABLE
                qsTile?.updateTile()

                serviceScope.launch(Dispatchers.IO) {
                    val appContext = applicationContext
                    WebPreference.putAsync(appContext, false)
                    HttpServerManager.stopServiceAsync(appContext)
                    withContext(Dispatchers.Main.immediate) {
                        setState(Tile.STATE_INACTIVE)
                    }
                }
            }
        }
    }


}
