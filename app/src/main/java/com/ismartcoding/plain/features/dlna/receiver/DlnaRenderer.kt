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
    private var lastPort: Int? = null
    private var scope: CoroutineScope? = null
    /** Held so stop() can close it immediately, unblocking accept() on the IO thread. */
    private var activeServerSocket: ServerSocket? = null

    fun start(context: Context) {
        if (DlnaRendererState.isRunning.value) return
        DlnaRendererState.startError.value = ""
        val serverSocket = openServerSocket()
        if (serverSocket == null) {
            val msg = "Failed to bind on ports ${CANDIDATE_PORTS.joinToString()}"
            LogCat.e("DlnaRenderer: $msg")
            DlnaRendererState.startError.value = msg
            return
        }
        val port = serverSocket.localPort
        lastPort = port
        activeServerSocket = serverSocket
        DlnaRendererState.port.value = port
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope!!.launch {
            try {
                launch { DlnaHttpServer.run(serverSocket) }
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
        // Close the socket first — this immediately unblocks the accept() call on the IO thread
        // and causes the OS to release the port, so the next start() can reuse it at once.
        activeServerSocket?.close()
        activeServerSocket = null
        scope?.cancel()
        scope = null
        DlnaRendererState.isRunning.value = false
        DlnaRendererState.reset()
        LogCat.d("DlnaRenderer stopped")
    }

    /**
     * Opens a [ServerSocket] with SO_REUSEADDR enabled so the port can be reused
     * immediately after the previous server socket is closed, without entering TIME_WAIT.
     * Tries the last successfully used port first, then falls back to other candidates.
     */
    private fun openServerSocket(): ServerSocket? {
        val candidates = lastPort
            ?.let { listOf(it) + CANDIDATE_PORTS.filter { p -> p != it } }
            ?: CANDIDATE_PORTS
        for (port in candidates) {
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(java.net.InetSocketAddress(port))
                return ss
            } catch (_: Exception) {
                LogCat.d("DlnaRenderer: port $port unavailable, trying next")
            }
        }
        return null
    }
}
