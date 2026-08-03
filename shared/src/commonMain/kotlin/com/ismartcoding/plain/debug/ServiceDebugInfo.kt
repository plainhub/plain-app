package com.ismartcoding.plain.debug

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.web.onlineClientIds

/**
 * Snapshot of all persistent service runtime statuses.
 */
data class ServiceDebugInfo(
    val httpServerRunning: Boolean = false,
    val httpServerState: String = "",
    val httpPort: Int = 0,
    val httpsPort: Int = 0,
    val wsSessionCount: Int = 0,
    val httpServerError: String = "",

    val mdnsRunning: Boolean = false,
    val mdnsHostname: String = "",

    val dlnaRunning: Boolean = false,
    val dlnaPlaybackState: String = "",
    val dlnaStartError: String = "",

    val bleRunning: Boolean = false,
    val bleClientId: String = "",
    val bleServiceUuid: String = "",

    val awareRunning: Boolean = false,
    val awareAttachStatus: String = "",
    val awareDiscoveredPeerCount: Int = 0,
)

/** Returns a fresh snapshot of all service debug info. */
fun getServiceDebugInfo(): ServiceDebugInfo {
    val httpRunning = isHttpServerRunning()
    val dlnaRunning = DlnaRendererState.isRunning.value
    val bleRunning = PairingTransport.isAdvertising()
    val awareRunning = TempData.awareRunning.value

    return ServiceDebugInfo(
        httpServerRunning = httpRunning,
        httpServerState = if (httpRunning) "ON" else "OFF",
        httpPort = TempData.httpPort.value,
        httpsPort = TempData.httpsPort.value,
        wsSessionCount = onlineClientIds.value.size,
        httpServerError = HttpServerManager.httpServerError,

        mdnsRunning = isMdnsRunning(),
        mdnsHostname = TempData.mdnsHostname,

        dlnaRunning = dlnaRunning,
        dlnaPlaybackState = DlnaRendererState.playbackState.value.name,
        dlnaStartError = DlnaRendererState.startError.value,

        bleRunning = bleRunning,
        bleClientId = TempData.clientId,
        bleServiceUuid = BleUuids.SERVICE_UUID,

        awareRunning = awareRunning,
        awareAttachStatus = getAwareAttachStatus(),
        awareDiscoveredPeerCount = getAwareDiscoveredPeerCount(),
    )
}

expect fun isHttpServerRunning(): Boolean
expect fun isMdnsRunning(): Boolean
expect fun getAwareAttachStatus(): String
expect fun getAwareDiscoveredPeerCount(): Int
