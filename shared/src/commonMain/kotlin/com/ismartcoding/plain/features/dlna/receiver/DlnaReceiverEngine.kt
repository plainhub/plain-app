package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.features.dlna.DlnaCommand
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.IODispatcher
import com.ismartcoding.plain.platform.startDlnaRenderer
import com.ismartcoding.plain.platform.stopDlnaRenderer
import com.ismartcoding.plain.preferences.DlnaAllowedSendersPreference
import com.ismartcoding.plain.preferences.DlnaDeniedSendersPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives the DLNA MediaRenderer receiver.
 *
 * Responsibilities:
 * 1. SSDP advertiser — M-SEARCH responses + ssdp:alive/byebye notifications so
 *    control points on the network can find the renderer.
 * 2. Command processing — consumes [DlnaRendererState.commandChannel] and
 *    updates playback state.
 * 3. Rule checking — inspects [DlnaRendererState.rawPendingCastRequest] and
 *    auto-accepts (known-allowed senders), auto-rejects (known-denied senders),
 *    or promotes to [DlnaRendererState.pendingCastRequest] for user confirmation.
 *
 * The HTTP control endpoints (description.xml, SOAP control, SUBSCRIBE) are
 * served by the shared web server — see `web/routes/DlnaRoutes.kt`.
 *
 * The engine runs independently of any page — once started (via the
 * WebSettings toggle or app startup) the receiver accepts casts from any
 * screen. The cast-request dialog and media player are rendered globally by
 * `DlnaReceiverOverlay`.
 */
object DlnaReceiverEngine {

    /** Stable UUID for this device's UPnP identity (regenerated per process). */
    val deviceUuid: String by lazy { randomUuid() }

    private var scope: CoroutineScope? = null
    private var activeSsdpSocket: DlnaSsdpSocket? = null
    private var commandJob: Job? = null
    private var ruleCheckJob: Job? = null

    /**
     * Starts the receiver: sets [DlnaRendererState.port] to the current web
     * server HTTP port (so SSDP LOCATION URLs point at the web server), launches
     * the SSDP loop, command processing, and rule checking. No-op if already
     * running.
     */
    fun start() {
        if (DlnaRendererState.isRunning.value) return
        DlnaRendererState.startError.value = ""

        DlnaRendererState.port.value = TempData.httpPort.value

        val ssdpSocket = createDlnaSsdpSocket()
        activeSsdpSocket = ssdpSocket

        scope = CoroutineScope(SupervisorJob() + IODispatcher)
        scope!!.launch {
            try {
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
        startCommandProcessing()
        startRuleCheck()
        DlnaRendererState.isRunning.value = true
        LogCat.d("DlnaReceiverEngine started, web port=${TempData.httpPort.value} uuid=$deviceUuid")
    }

    /**
     * Stops the receiver: closes the SSDP socket (unblocking the receive loop
     * immediately), cancels the coroutine scope, and resets state.
     */
    fun stop() {
        activeSsdpSocket?.close()
        activeSsdpSocket = null
        scope?.cancel()
        scope = null
        commandJob = null
        ruleCheckJob = null
        DlnaRendererState.isRunning.value = false
        DlnaRendererState.reset()
        LogCat.d("DlnaReceiverEngine stopped")
    }

    /**
     * Restarts the receiver engine. Used when the engine failed to start
     * (e.g. multicast lock failure) and the user taps retry.
     *
     * Runs on an independent [coIO] scope because [stop] cancels the engine's
     * own scope — launching retry inside it would self-cancel before
     * [startDlnaRenderer] runs.
     */
    fun retry() {
        coIO {
            DlnaRendererState.isRetrying.value = true
            stopDlnaRenderer()
            startDlnaRenderer()
            delay(300)
            DlnaRendererState.isRetrying.value = false
        }
    }

    /**
     * Consumes [DlnaRendererState.commandChannel] and updates playback state.
     * Runs for the lifetime of the engine so casts are handled on any screen.
     */
    private fun startCommandProcessing() {
        commandJob = scope?.launch {
            for (command in DlnaRendererState.commandChannel) {
                when (command) {
                    is DlnaCommand.SetUri -> {
                        DlnaRendererState.mediaUri.value = command.uri
                        DlnaRendererState.mediaTitle.value = command.title
                        DlnaRendererState.mediaAlbumArtUri.value = command.albumArtUri
                        DlnaRendererState.mediaType.value = command.mediaType
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.TRANSITIONING
                    }
                    is DlnaCommand.Play -> DlnaRendererState.playbackState.value = DlnaPlaybackState.PLAYING
                    is DlnaCommand.Pause -> DlnaRendererState.playbackState.value = DlnaPlaybackState.PAUSED
                    is DlnaCommand.Stop -> {
                        DlnaRendererState.seekTargetMs.value = 0L
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.STOPPED
                    }
                    is DlnaCommand.Seek -> DlnaRendererState.seekTargetMs.value = command.positionMs
                }
            }
        }
    }

    /**
     * Inspects [DlnaRendererState.rawPendingCastRequest]: auto-accepts known
     * senders, auto-rejects denied senders, and promotes unknown senders to
     * [DlnaRendererState.pendingCastRequest] for user confirmation (rendered
     * globally by `DlnaReceiverOverlay`).
     */
    private fun startRuleCheck() {
        ruleCheckJob = scope?.launch {
            DlnaRendererState.rawPendingCastRequest.filterNotNull().collect { pending ->
                val allowed = DlnaAllowedSendersPreference.getAsync()
                val denied = DlnaDeniedSendersPreference.getAsync()
                when {
                    DlnaAllowedSendersPreference.containsIp(allowed, pending.senderIp) -> {
                        DlnaRendererState.pendingCastRequest.value = null
                        DlnaRendererState.rawPendingCastRequest.value = null
                        val playQueued = DlnaRendererState.pendingPlayQueued.value
                        DlnaRendererState.pendingPlayQueued.value = false
                        DlnaRendererState.commandChannel.trySend(
                            DlnaCommand.SetUri(pending.mediaUri, pending.mediaTitle, pending.mediaType, pending.albumArtUri)
                        )
                        if (playQueued) DlnaRendererState.commandChannel.trySend(DlnaCommand.Play)
                    }
                    DlnaDeniedSendersPreference.containsIp(denied, pending.senderIp) -> {
                        DlnaRendererState.rawPendingCastRequest.value = null
                        DlnaRendererState.pendingPlayQueued.value = false
                    }
                    else -> {
                        DlnaRendererState.pendingCastRequest.value = pending
                        DlnaRendererState.rawPendingCastRequest.value = null
                    }
                }
            }
        }
    }

    /** SSDP loop: sends initial alive, listens for M-SEARCH, resends alive every 30s. */
    private suspend fun runSsdpLoop(socket: DlnaSsdpSocket) = withIO {
        try {
            DlnaSsdpMessages.aliveMessages(deviceUuid).forEach { socket.sendMulticast(it) }
            LogCat.d("DLNA SSDP advertiser started, sent initial alive")
            while (isActive) {
                val packet = try {
                    socket.receive(30_000)
                } catch (_: Exception) {
                    null
                }
                if (packet != null) {
                    handleSsdpPacket(packet, socket, deviceUuid)
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
     * Processes a single inbound SSDP datagram. M-SEARCH queries are answered
     * via **unicast** to the sender's source address:port (per UPnP spec) —
     * never re-multicasted to the group, because control points listen on a
     * random ephemeral port and cannot receive multicast sent to port 1900.
     */
    internal fun handleSsdpPacket(packet: DlnaSsdpPacket, socket: DlnaSsdpSocket, uuid: String) {
        if (!packet.message.contains("M-SEARCH")) return
        LogCat.d("DLNA M-SEARCH from ${packet.sourceAddress}:${packet.sourcePort}")
        val responses = DlnaSsdpMessages.searchResponses(uuid)
        responses.forEach { socket.sendUnicast(it, packet.sourceAddress, packet.sourcePort) }
        LogCat.d("DLNA sent ${responses.size} search responses to ${packet.sourceAddress}:${packet.sourcePort}")
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
