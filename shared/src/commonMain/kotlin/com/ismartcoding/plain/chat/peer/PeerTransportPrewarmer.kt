package com.ismartcoding.plain.chat.peer

import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.ble.BleServiceData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.chat.peer.transport.TransportUnavailable
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.platform.isBleReady
import com.ismartcoding.plain.platform.isWifiAwareSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pre-warms peer transports when the user enters a ChatPage so that the first
 * message send doesn't waste time on transport discovery:
 *
 * 1. Scans BLE to discover the peer by its shortId (SHA256(clientId)[0:8],
 *    broadcast in the BLE scan response serviceData). The full clientId is
 *    NOT broadcast — only its 8-byte hash prefix is, and the scanner matches
 *    it via [BleServiceData.shortIdOf]. Android's BLE MAC randomizes every
 *    ~15 minutes, so we never store or match on MAC.
 * 2. Reads the peer's Aware flags from the BLE scan response serviceData
 *    (byte[0]) and caches them via [PeerCacher.setAwareSupported] /
 *    [PeerCacher.setAwareRunning]. This lets [com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport]
 *    skip itself immediately when the peer's Aware isn't running, instead of
 *    waiting 10s+ per buildLink attempt.
 * 3. If the peer supports Wi-Fi Aware but isn't running it, sends a `startAware`
 *    GraphQL mutation via BLE ([PeerGraphQLClient.startAware]) asking the peer
 *    to start its Aware service and subscribe for us. By the time the user
 *    types and sends a message, both sides should have Aware ready, and the
 *    message goes over the much faster Aware transport instead of BLE.
 *
 * The prewarm is throttled to once per [PREWARM_TTL_MS] per peer to avoid
 * spamming BLE scans while the user navigates back and forth.
 */
object PeerTransportPrewarmer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Throttle map: peerId -> last prewarm timestamp (millis). Uses mutableStateMapOf
    // for KMP compatibility (ConcurrentHashMap is JVM-only).
    private val prewarmTimestamps = mutableStateMapOf<String, Long>()
    private const val PREWARM_TTL_MS = 30_000L
    private const val BLE_SCAN_TIMEOUT_MS = 15_000L

    /**
     * Kick off prewarm for [peerId] in the background. Safe to call from the
     * UI thread (ChatPage LaunchedEffect). No-op if a prewarm for this peer
     * ran within the last [PREWARM_TTL_MS].
     */
    fun prewarm(peerId: String) {
        val last = prewarmTimestamps[peerId] ?: 0L
        val now = TimeHelper.nowMillis()
        if (now - last < PREWARM_TTL_MS) {
            LogCat.d("[Prewarm] skip peer=$peerId (throttled, ageMs=${now - last})")
            return
        }
        prewarmTimestamps[peerId] = now
        scope.launch {
            try {
                prewarmInternal(peerId)
            } catch (e: Exception) {
                LogCat.e("[Prewarm] failed peer=$peerId msg=${e.message}")
            }
        }
    }

    private suspend fun prewarmInternal(peerId: String) {
        val peer = PeerCacher.getPeer(peerId)
        if (peer == null || !peer.isPaired()) {
            LogCat.d("[Prewarm] skip peer=$peerId (not paired / not in cache)")
            return
        }
        if (!isBleReady()) {
            LogCat.d("[Prewarm] skip peer=$peerId (BLE not ready)")
            return
        }
        LogCat.d("[Prewarm] start peer=$peerId awareRunning=${PeerCacher.isAwareRunning(peerId)}")

        // The peer.id is the clientId — scan BLE for it directly and refresh
        // the Aware flags from the scan response serviceData (no connect
        // needed since the clientId is broadcast in the advertisement).
        refreshAwareFlagFromScan(peerId)

        // After the scan we know the peer's current Aware status. If the peer
        // supports Aware but isn't running it, nudge it to start via BLE.
        maybeStartAwareViaBle(peer)
    }

    /**
     * Scans for [peerId] (the full clientId) by matching its shortId
     * (SHA256(clientId)[0:8]) against the BLE scan response serviceData.
     * Reads the peer's aware flags directly from the scan response — no GATT
     * connection needed — and refreshes [PeerCacher]. Returns true if a
     * matching device was found.
     */
    private suspend fun refreshAwareFlagFromScan(peerId: String): Boolean {
        val shortId = BleServiceData.shortIdOf(peerId)
        val scanner = bleTransport().createScanner()
        val device = withTimeoutOrNull(BLE_SCAN_TIMEOUT_MS) {
            scanner.scan(BleUuids.SERVICE_UUID).firstOrNull { it.id == shortId }
        }
        if (device == null) {
            LogCat.d("[Prewarm] refresh scan timeout peer=$peerId shortId=$shortId")
            return false
        }
        // Refresh aware flags from the scan response (byte[0] of serviceData).
        // These are a cheap hint without a GATT connection; the DISCOVER reply
        // (read by PairingTransport.scanAndDiscover) will overwrite them with
        // authoritative values when a full discovery happens.
        PeerCacher.setAwareSupported(peerId, device.awareSupported)
        PeerCacher.setAwareRunning(peerId, device.awareRunning)
        LogCat.d("[Prewarm] found peer=$peerId shortId=$shortId awareSupported=${device.awareSupported} awareRunning=${device.awareRunning}")
        return true
    }

    /**
     * If the peer supports Wi-Fi Aware but isn't currently running it, sends
     * a `startAware` GraphQL mutation via BLE. This is fire-and-forget from
     * the user's perspective — the next message send will check the (refreshed)
     * awareRunning flag and use Aware if it's up by then.
     */
    private suspend fun maybeStartAwareViaBle(peer: DPeer) {
        // PeerCacher.isAwareSupported returns the BLE-scan-refreshed flag
        // (the source of truth — the DB aware_supported column was removed).
        if (!PeerCacher.isAwareSupported(peer.id)) {
            LogCat.d("[Prewarm] peer=${peer.id} doesn't support Aware, skip startAware")
            return
        }
        if (!isWifiAwareSupported) {
            LogCat.d("[Prewarm] local device doesn't support Aware, skip startAware")
            return
        }
        if (PeerCacher.isAwareRunning(peer.id)) {
            LogCat.d("[Prewarm] peer=${peer.id} Aware already running")
            return
        }
        LogCat.d("[Prewarm] sending startAware to peer=${peer.id} via BLE")
        try {
            val resp = PeerGraphQLClient.startAware(peer)
            if (resp.isSuccess) {
                // Optimistically mark the peer's Aware as running — the peer
                // will have started its Aware service by the time it responds.
                // If it didn't actually start, the next buildLink failure will
                // naturally fall back to BLE.
                PeerCacher.setAwareRunning(peer.id, true)
                LogCat.d("[Prewarm] startAware ok peer=${peer.id}")
            } else {
                LogCat.e("[Prewarm] startAware failed peer=${peer.id} err=${resp.getError()}")
            }
        } catch (e: TransportUnavailable) {
            LogCat.e("[Prewarm] startAware transport unavailable peer=${peer.id}: ${e.message}")
        } catch (e: Exception) {
            LogCat.e("[Prewarm] startAware error peer=${peer.id}: ${e.message}")
        }
    }
}
