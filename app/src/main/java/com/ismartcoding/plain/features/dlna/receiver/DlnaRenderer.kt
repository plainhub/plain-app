package com.ismartcoding.plain.features.dlna.receiver

import android.content.Context
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.util.UUID

/** Coordinates the DLNA renderer: starts/stops the HTTP and SSDP servers. */
object DlnaRenderer {

    /** Stable UUID for this device's UPnP identity (regenerated per process). */
    val deviceUuid: String by lazy { UUID.randomUUID().toString() }

    private val CANDIDATE_PORTS = listOf(7878, 7879, 7880)
    private var scope: CoroutineScope? = null

    fun start(context: Context) {
        if (DlnaRendererState.isRunning.value) return
        DlnaRendererState.startError.value = ""
        val port = findAvailablePort()
        if (port == null) {
            val msg = "Failed to bind on ports ${CANDIDATE_PORTS.joinToString()}"
            LogCat.e("DlnaRenderer: $msg")
            DlnaRendererState.startError.value = msg
            return
        }
        DlnaRendererState.port.value = port
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope!!.launch {
            try {
                launch { DlnaHttpServer.run(port) }
                launch { DlnaSsdpAdvertiser.run(context) }
            } catch (e: Exception) {
                LogCat.e("DlnaRenderer startup error: ${e.message}")
                DlnaRendererState.isRunning.value = false
            }
        }
        DlnaRendererState.isRunning.value = true
        LogCat.d("DlnaRenderer started on port $port uuid=$deviceUuid")
    }

    fun stop() {
        scope?.cancel()
        scope = null
        DlnaRendererState.isRunning.value = false
        DlnaRendererState.reset()
        LogCat.d("DlnaRenderer stopped")
    }

    private fun findAvailablePort(): Int? {
        for (port in CANDIDATE_PORTS) {
            try {
                ServerSocket(port).use { return port }
            } catch (_: Exception) {
                LogCat.d("DlnaRenderer: port $port unavailable, trying next")
            }
        }
        return null
    }
}
