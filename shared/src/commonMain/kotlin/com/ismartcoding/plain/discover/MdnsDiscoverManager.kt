package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.mdns.MdnsFoundDevice
import com.ismartcoding.plain.lib.mdns.MdnsHostResponder
import com.ismartcoding.plain.lib.mdns.MdnsServiceBrowser
import com.ismartcoding.plain.lib.mdns.MdnsServiceInfo
import com.ismartcoding.plain.lib.mdns.PLAINAPP_SERVICE_TYPE
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.models.NearbyViewModel

/** Installs platform-supplied data the shared-lib mDNS stack needs (iOS interfaces). */
internal expect fun ensureMdnsInterfacesInstalled()

object MdnsDiscoverManager {
    fun startReceiver() {
        ensureMdnsInterfacesInstalled()
        // Ensure the shared mDNS responder socket is up so the browser can
        // send queries and the responder can answer PTR/SRV/TXT/A queries.
        // Service registration itself happens with the HTTP service lifecycle.
        MdnsHostResponder.ensureStarted(TempData.mdnsHostname)
        // Resident listener: parse inbound mDNS responses from app start so a
        // paired peer's IP change is picked up without opening the nearby page.
        MdnsServiceBrowser.hostnameProvider = { TempData.mdnsHostname }
        MdnsServiceBrowser.onDevice = ::handleFoundDevice
        MdnsServiceBrowser.ensureListening()
    }

    fun startPeriodicDiscovery() {
        if (MdnsServiceBrowser.isRunning) return
        sendEvent(WebSocketEvent(EventType.NEARBY_DISCOVERY_STARTED, "{}"))
        MdnsHostResponder.ensureStarted(TempData.mdnsHostname)
        MdnsServiceBrowser.start()
    }

    fun stopPeriodicDiscovery() {
        MdnsServiceBrowser.stop()
        sendEvent(WebSocketEvent(EventType.NEARBY_DISCOVERY_STOPPED, "{}"))
    }

    fun isDiscovering(): Boolean {
        return MdnsServiceBrowser.isRunning
    }

    /**
     * Republishes the local service after the advertised data changed (device
     * renamed, port changed) so peers pick it up without waiting for the next
     * re-announce. No-op while no service is published (web service off).
     *
     * Runs on IO: building the reply enumerates network interfaces and the
     * republish sends multicast datagrams — the UI rename path calls this from
     * the main dispatcher.
     */
    fun updateAdvertisedService() {
        coIO {
            MdnsHostResponder.updateService(
                buildMdnsServiceInfo(PairingCore.buildDiscoverReply(), TempData.mdnsHostname)
            )
        }
    }

    /**
     * Triggers an immediate one-shot mDNS PTR browse. Responses for a paired
     * peer refresh its IP/port via
     * [PeerManager.applyDeviceDiscovered]
     * (which also bumps `updatedAt`), letting
     * [PeerStatusManager.reconnectPeer] detect
     * whether the reply arrived within its wait window.
     */
    fun browse() {
        // The shared socket may be down (e.g. network changed before
        // scheduleRestart ran); sendQuery silently no-ops without it.
        MdnsHostResponder.ensureStarted(TempData.mdnsHostname)
        MdnsServiceBrowser.sendPtrQuery()
    }

    /**
     * Recreates the shared responder socket after a network change.
     *
     * Necessary, not optional: mDNS multicast group membership is
     * per-interface. The responder socket joins `224.0.0.251` on each
     * interface that exists at bind time (see [MdnsHostResponder.restartSocket]).
     * When the device switches networks (Wi-Fi ↔ cellular, new SSID, airplane
     * mode cycle), the new interface was never joined, so the old socket stops
     * receiving multicast — discovery silently dies until the socket is
     * recreated and re-joins on the fresh interface set. Triggered by
     * [com.ismartcoding.plain.NetworkMonitor] on every validated-network
     * change. Socket restarts keep the browser's packet listeners, so
     * discovery resumes with no state loss.
     */
    fun scheduleRestart(reason: String) {
        LogCat.d("Network change ($reason) — restarting mDNS responder")
        MdnsHostResponder.restartSocket()
    }

    private fun handleFoundDevice(device: MdnsFoundDevice) {
        // Skip our own looped-back announcements (multicast loop is enabled on
        // purpose so multiple same-device sockets keep working) instead of
        // emitting this device into the nearby list / peer tables.
        if (device.id == TempData.clientId) return
        val deviceType = runCatching { DeviceType.valueOf(device.deviceType) }
            .getOrDefault(DeviceType.OTHER)
        val d = DNearbyDevice(
            id = device.id,
            name = device.name,
            ips = device.ips,
            port = device.port,
            deviceType = deviceType,
            version = device.version,
            platform = device.platform,
            lastSeen = TimeHelper.now(),
            discoveryMethods = setOf(DiscoveryMethod.LAN),
        )
        coIO {
            // Resident-listener path: always refresh a paired peer's address so a
            // changed IP is picked up by the next reconnect attempt even while
            // the nearby scan loop is off.
            PeerManager.applyDeviceDiscovered(
                deviceId = d.id,
                ips = d.ips,
                port = d.port,
                name = d.name,
                deviceType = d.deviceType,
            )
            // Scan-gated path: nearby-list events only fire while discovery runs.
            if (!MdnsServiceBrowser.isRunning) return@coIO
            NearbyViewModel.handleNewDevice(d)
            PeerStatusManager.setOnline(d.id, true)
        }
    }
}

/**
 * Builds the advertised mDNS service for this device from a discovery reply.
 * TXT keys mirror [DDiscoverReply] fields (see design doc §4.1).
 */
internal fun buildMdnsServiceInfo(reply: DDiscoverReply, hostname: String): MdnsServiceInfo =
    MdnsServiceInfo(
        instanceName = reply.name,
        serviceType = PLAINAPP_SERVICE_TYPE,
        targetHostname = hostname,
        port = reply.port,
        txtRecords = listOf(
            "id=${reply.id}",
            "dv=${reply.deviceType.name}",
            "ver=${reply.version}",
            "pf=${reply.platform}",
            "aw=${if (reply.awareSupported) "1" else "0"}",
            "ar=${if (reply.awareRunning) "1" else "0"}",
        ),
        ips = reply.ips,
    )
