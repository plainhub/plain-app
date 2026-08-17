package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.mdns.MdnsHostResponder
import com.ismartcoding.plain.mdns.MdnsServiceBrowser

object MdnsDiscoverManager {
    fun startReceiver() {
        // Ensure the shared mDNS responder socket is up so the browser can
        // send queries and the responder can answer PTR/SRV/TXT/A queries.
        // Service registration itself happens with the HTTP service lifecycle.
        MdnsHostResponder.ensureStarted(TempData.mdnsHostname)
        // Resident listener: parse inbound mDNS responses from app start so a
        // paired peer's IP change is picked up without opening the nearby page.
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
     * Triggers an immediate one-shot mDNS PTR browse. Responses for a paired
     * peer refresh its IP/port via
     * [com.ismartcoding.plain.chat.peer.PeerManager.applyDeviceDiscovered]
     * (which also bumps `updatedAt`), letting
     * [com.ismartcoding.plain.chat.peer.PeerStatusManager.reconnectPeer] detect
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
}
