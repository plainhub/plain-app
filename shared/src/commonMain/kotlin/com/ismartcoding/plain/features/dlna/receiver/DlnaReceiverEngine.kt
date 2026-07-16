package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.IODispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Orchestrates the DLNA MediaRenderer receiver: opens the HTTP server socket,
 * accepts control-point connections, drives the SSDP advertiser, and dispatches
 * SOAP requests through [DlnaHttpRouter].
 *
 * Replaces androidMain's `DlnaRenderer` + `DlnaHttpServer` + `DlnaSsdpAdvertiser`
 * trio. The platform layer only needs to provide [createDlnaServerSocket] and
 * [createDlnaSsdpSocket]; everything else — port selection, accept loop,
 * M-SEARCH handling, alive/byebye cadence — lives here in commonMain.
 */
object DlnaReceiverEngine {

    /** Stable UUID for this device's UPnP identity (regenerated per process). */
    val deviceUuid: String by lazy { randomUuid() }

    private val CANDIDATE_PORTS = listOf(7878, 7879, 7880)
    private var lastPort: Int? = null
    private var scope: CoroutineScope? = null
    /** Held so stop() can close it immediately, unblocking accept() on the IO thread. */
    private var activeServerSocket: DlnaServerSocket? = null
    private var activeSsdpSocket: DlnaSsdpSocket? = null

    /**
     * Starts the renderer. Binds one of [CANDIDATE_PORTS] (preferring the last
     * successful port), launches the HTTP accept loop and the SSDP advertiser,
     * and marks [DlnaRendererState.isRunning] true. No-op if already running.
     */
    fun start() {
        if (DlnaRendererState.isRunning.value) return
        DlnaRendererState.startError.value = ""

        val serverSocket = openServerSocket()
        if (serverSocket == null) {
            val msg = "Failed to bind on ports ${CANDIDATE_PORTS.joinToString()}"
            LogCat.e("DlnaReceiverEngine: $msg")
            DlnaRendererState.startError.value = msg
            return
        }
        val port = serverSocket.localPort
        lastPort = port
        activeServerSocket = serverSocket
        DlnaRendererState.port.value = port

        val ssdpSocket = createDlnaSsdpSocket()
        activeSsdpSocket = ssdpSocket

        scope = CoroutineScope(SupervisorJob() + IODispatcher)
        scope!!.launch {
            try {
                launch { runHttpLoop(serverSocket) }
                if (ssdpSocket != null) {
                    launch { runSsdpLoop(ssdpSocket) }
                } else {
                    LogCat.w("DlnaReceiverEngine: SSDP socket unavailable — discovery disabled")
                }
            } catch (e: Exception) {
                LogCat.e("DlnaReceiverEngine startup error: ${e.message}")
                DlnaRendererState.isRunning.value = false
            }
        }
        DlnaRendererState.isRunning.value = true
        LogCat.d("DlnaReceiverEngine started on port $port uuid=$deviceUuid")
    }

    /**
     * Stops the renderer: closes the sockets (unblocking the accept/receive
     * loops immediately), cancels the coroutine scope, and resets state.
     */
    fun stop() {
        // Close sockets first — this immediately unblocks the IO loops and
        // causes the OS to release the port for the next start() call.
        activeServerSocket?.close()
        activeServerSocket = null
        activeSsdpSocket?.close()
        activeSsdpSocket = null
        scope?.cancel()
        scope = null
        DlnaRendererState.isRunning.value = false
        DlnaRendererState.reset()
        LogCat.d("DlnaReceiverEngine stopped")
    }

    /** HTTP accept loop: each connection is handled on its own coroutine. */
    private suspend fun runHttpLoop(serverSocket: DlnaServerSocket) = withIO {
        LogCat.d("DLNA HTTP server started on port ${serverSocket.localPort}")
        while (isActive) {
            val client = try {
                serverSocket.accept()
            } catch (_: Exception) {
                break
            }
            if (client == null) break
            launch { handleClient(client) }
        }
    }

    /** Reads one HTTP request, routes it, writes the response, closes. */
    private suspend fun handleClient(client: DlnaClientConnection) = withIO {
        try {
            val request = client.readHttpRequest() ?: return@withIO
            val senderName = resolveSenderName(request.headers, client.senderIp)
            val response = DlnaHttpRouter.route(request, deviceUuid, client.senderIp, senderName)
            client.writeResponse(response)
        } catch (e: Exception) {
            LogCat.e("DLNA client error: ${e.message}")
        } finally {
            client.close()
        }
    }

    /** SSDP loop: sends initial alive, listens for M-SEARCH, resends alive every 30s. */
    private suspend fun runSsdpLoop(socket: DlnaSsdpSocket) = withIO {
        try {
            DlnaSsdpMessages.aliveMessages(deviceUuid).forEach { socket.sendMulticast(it) }
            while (isActive) {
                val msg = try {
                    socket.receive(30_000)
                } catch (_: Exception) {
                    null
                }
                if (msg != null) {
                    if (msg.contains("M-SEARCH")) {
                        // Respond to the unicast source (parsed from the message)
                        val srcAddress = extractHeaderValue(msg, "HOST")?.substringBefore(':') ?: DlnaSsdpMessages.SSDP_ADDR
                        val srcPort = extractHeaderValue(msg, "HOST")?.substringAfter(':')?.toIntOrNull()
                            ?: DlnaSsdpMessages.SSDP_PORT
                        DlnaSsdpMessages.searchResponses(deviceUuid).forEach {
                            socket.sendUnicast(it, srcAddress, srcPort)
                        }
                    }
                } else {
                    // Timeout — resend alive notifications
                    DlnaSsdpMessages.aliveMessages(deviceUuid).forEach { socket.sendMulticast(it) }
                }
            }
        } catch (e: Exception) {
            LogCat.e("DLNA SSDP error: ${e.message}")
        } finally {
            try {
                DlnaSsdpMessages.byebyeMessages(deviceUuid).forEach { socket.sendMulticast(it) }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Opens a [DlnaServerSocket] with SO_REUSEADDR enabled, trying the last
     * successfully used port first, then the other candidates.
     */
    private fun openServerSocket(): DlnaServerSocket? {
        val candidates = lastPort
            ?.let { listOf(it) + CANDIDATE_PORTS.filter { p -> p != it } }
            ?: CANDIDATE_PORTS
        for (port in candidates) {
            val ss = createDlnaServerSocket(port)
            if (ss != null) return ss
            LogCat.d("DlnaReceiverEngine: port $port unavailable, trying next")
        }
        return null
    }

    /** Extracts a header value from an HTTP/SSDP message (case-insensitive). */
    private fun extractHeaderValue(message: String, headerName: String): String? {
        val lines = message.split("\r\n", "\n")
        val prefix = headerName.uppercase() + ":"
        for (line in lines.drop(1)) {
            if (line.uppercase().startsWith(prefix)) {
                return line.substringAfter(':').trim()
            }
        }
        return null
    }

    /**
     * Pure-Kotlin RFC 4122 v4 UUID generator (replaces `java.util.UUID`).
     * Uses [Random] for the random bits and formats as 8-4-4-4-12 hex string.
     */
    private fun randomUuid(): String {
        val bytes = Random.Default.nextBytes(16)
        // Set version (4) and variant (RFC 4122) bits.
        bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte()
        bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
        val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
    }
}
