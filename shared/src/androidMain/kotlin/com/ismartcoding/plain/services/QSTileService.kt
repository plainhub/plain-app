package com.ismartcoding.plain.services

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.desktop_access
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.system.setRemoteAccessEnabled
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val appIconDrawableId: Int by lazy {
    appContext.resources.getIdentifier("app_icon", "drawable", appContext.packageName)
}

class QSTileService : TileService() {
    private var stateEventJob: Job? = null
    private var toggleJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun updateTile(enabled: Boolean) {
        qsTile?.apply {
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = LocaleHelper.getString(Res.string.desktop_access)
            icon = Icon.createWithResource(applicationContext, appIconDrawableId)
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        stateEventJob?.cancel()
        stateEventJob = serviceScope.launch {
            Prefs.remoteAccess.collect { updateTile(it) }
        }
    }

    override fun onStopListening() {
        stateEventJob?.cancel()
        stateEventJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        if (toggleJob?.isActive == true) return
        unlockAndRun {
            if (toggleJob?.isActive == true) return@unlockAndRun
            toggleJob = serviceScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        setRemoteAccessEnabled(!Prefs.remoteAccess.value)
                    }
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (error: Exception) {
                    LogCat.e("Desktop access tile failed: ${error.message}")
                } finally {
                    updateTile(Prefs.remoteAccess.value)
                }
            }
        }
    }
}
