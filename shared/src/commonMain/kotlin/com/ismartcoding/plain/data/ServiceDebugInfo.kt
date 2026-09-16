package com.ismartcoding.plain.data

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.onlineClientIds
import com.ismartcoding.plain.platform.getAwareAttachStatus
import com.ismartcoding.plain.platform.getAwareDiscoveredPeerCount
import com.ismartcoding.plain.platform.isHttpServerRunning
import com.ismartcoding.plain.platform.isMdnsRunning

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
        httpServerError = HttpServerManager.httpServerError.value,

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
