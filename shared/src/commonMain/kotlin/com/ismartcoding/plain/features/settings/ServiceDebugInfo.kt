package com.ismartcoding.plain.features.settings

import com.ismartcoding.plain.platform.network.wifiAwareAttached
import com.ismartcoding.plain.platform.network.ble.PairingTransport
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.features.sessions.onlineClientIds
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.nearby.RustMdnsRuntime
import com.ismartcoding.plain.platform.network.isWifiAwareSupported
import com.ismartcoding.plain.platform.network.wifiAwareDiscoveredPeerCount

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

fun getServiceDebugInfo(): ServiceDebugInfo {
    val httpState = HttpServerManager.httpServerState.value
    val dlnaRunning = DlnaRendererState.isRunning.value
    val bleRunning = PairingTransport.isAdvertising()
    val awareRunning = wifiAwareAttached.value

    return ServiceDebugInfo(
        httpServerRunning = httpState == ServiceState.ON,
        httpServerState = httpState.name,
        httpPort = Prefs.httpPort.value,
        httpsPort = Prefs.httpsPort.value,
        wsSessionCount = onlineClientIds.value.size,
        httpServerError = HttpServerManager.httpServerError.value.ifEmpty { HttpServerManager.httpServerResourcesError.value },

        mdnsRunning = RustMdnsRuntime.running,
        mdnsHostname = Prefs.mdnsHostname.value,

        dlnaRunning = dlnaRunning,
        dlnaPlaybackState = DlnaRendererState.playbackState.value.name,
        dlnaStartError = DlnaRendererState.startError.value,

        bleRunning = bleRunning,
        bleClientId = Prefs.clientId.value,
        bleServiceUuid = Prefs.clientId.value,

        awareRunning = awareRunning,
        awareAttachStatus = when {
            !isWifiAwareSupported -> "not available"
            awareRunning -> "attached"
            else -> "not attached"
        },
        awareDiscoveredPeerCount = wifiAwareDiscoveredPeerCount,
    )
}
