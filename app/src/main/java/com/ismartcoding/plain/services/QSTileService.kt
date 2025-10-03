package com.ismartcoding.plain.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.web.HttpServerManager

class QSTileService : TileService() {
    private var httpServerStateListener: ((HttpServerState) -> Unit)? = null

    fun setState(state: Int) {
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

        // Register HTTP server state listener
        httpServerStateListener = { state ->
            when (state) {
                HttpServerState.ON -> setState(Tile.STATE_ACTIVE)
                HttpServerState.OFF -> setState(Tile.STATE_INACTIVE)
                HttpServerState.STARTING -> setState(Tile.STATE_INACTIVE)
                HttpServerState.STOPPING -> setState(Tile.STATE_INACTIVE)
                HttpServerState.ERROR -> setState(Tile.STATE_INACTIVE)
            }
        }

        // Listen for HTTP server state changes
        receiveEventHandler<HttpServerStateChangedEvent> { event ->
            httpServerStateListener?.invoke(event.state)
        }

        // Check current server state
        coIO {
            try {
                // First check if webEnabled is true in TempData
                if (TempData.webEnabled) {
                    val checkResult = HttpServerManager.checkServerAsync()
                    if (checkResult.websocket && checkResult.http) {
                        setState(Tile.STATE_ACTIVE)
                    } else {
                        // Service should be running but isn't responding
                        LogCat.d("Web service enabled but not responding, setting inactive state")
                        setState(Tile.STATE_INACTIVE)
                    }
                } else {
                    setState(Tile.STATE_INACTIVE)
                }
            } catch (e: Exception) {
                LogCat.e("Failed to check server state: ${e.message}")
                setState(Tile.STATE_INACTIVE)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()

        // Unregister HTTP server state listener
        httpServerStateListener = null
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

                coIO {
                    WebPreference.putAsync(MainApp.instance, false)
                    HttpServerManager.stopServiceAsync(this@QSTileService)
                    setState(Tile.STATE_INACTIVE)
                }
            }
        }
    }


}
