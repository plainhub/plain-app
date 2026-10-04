package com.ismartcoding.plain.mdns

import android.content.Context
import com.ismartcoding.plain.discover.MdnsDiscoverManager
import com.ismartcoding.plain.lib.coIO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MdnsRegister(context: Context) {
    private val appContext = context.applicationContext
    private var restart: Job? = null
    private var hotspotWatcher: MdnsHotspotWatcher? = null

    fun start() {
        hotspotWatcher = MdnsHotspotWatcher(appContext) { schedule("hotspotStateChanged") }.also { it.start() }
    }
    fun stop() {
        restart?.cancel()
        restart = null
        hotspotWatcher?.stop()
        hotspotWatcher = null
    }
    fun schedule(reason: String) {
        if (restart?.isActive == true) return
        restart = coIO {
            delay(800)
            MdnsDiscoverManager.scheduleRestart(reason)
        }
    }
}
