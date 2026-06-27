package com.ismartcoding.plain.discover

import android.util.Base64
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.helpers.NetworkHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.getAppVersion
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DDiscoverReply
import com.ismartcoding.plain.data.DDiscoverRequest
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

object NearbyDiscoverManager {
    private const val BROADCAST_INTERVAL_MS = 5_000L

    private var broadcastJob: Job? = null
    private var restartJob: Job? = null

    fun start() {
        NearbyNetwork.startReceiver(::onDatagram)
    }

    fun startPeriodicDiscovery() {
        if (broadcastJob?.isActive == true) return
        sendEvent(WebSocketEvent(EventType.NEARBY_DISCOVERY_STARTED, "{}"))
        broadcastJob = coIO {
            while (isActive) {
                runCatching { broadcastDiscover(DDiscoverRequest()) }
                    .onFailure { LogCat.e("Periodic discovery error: ${it.message}") }
                delay(BROADCAST_INTERVAL_MS)
            }
        }
    }

    fun stopPeriodicDiscovery() {
        broadcastJob?.cancel()
        broadcastJob = null
        sendEvent(WebSocketEvent(EventType.NEARBY_DISCOVERY_STOPPED, "{}"))
    }

    fun isDiscovering(): Boolean {
        return broadcastJob?.isActive == true
    }

    fun discoverSpecificDevice(toId: String, key: ByteArray) {
        broadcastDiscover(
            DDiscoverRequest(
                fromId = TempData.clientId,
                toId = Base64.encodeToString(CryptoHelper.chaCha20Encrypt(key, toId), Base64.NO_WRAP),
            )
        )
    }

    fun scheduleRestart(reason: String) {
        restartJob?.cancel()
        restartJob = coIO {
            delay(1_500) // debounce rapid network churn
            LogCat.d("Network change ($reason) — restarting multicast listener")
            NearbyNetwork.stopReceiver()
            NearbyNetwork.startReceiver(::onDatagram)
        }
    }

    // ---- Message routing -------------------------------------------------------

    private fun onDatagram(message: String, senderIP: String) {
        if (NetworkHelper.getDeviceIP4s().contains(senderIP)) return

        val type = NearbyMessageType.entries.firstOrNull { message.startsWith(it.toPrefix()) } ?: return
        val payload = message.removePrefix(type.toPrefix())

        when (type) {
            NearbyMessageType.DISCOVER -> coIO { handleDiscoverRequest(payload, senderIP) }
            NearbyMessageType.DISCOVER_REPLY -> handleDiscoverReply(payload)
            NearbyMessageType.PAIR_REQUEST -> {
                val request = JsonHelper.jsonDecode<DPairingRequest>(payload)
                request.fromIp = senderIP
                sendEvent(PairingRequestReceivedEvent(request))
                sendEvent(WebSocketEvent(EventType.PAIRING_REQUEST_RECEIVED, JsonHelper.jsonEncode(request)))
            }

            NearbyMessageType.PAIR_RESPONSE -> {
                val response = JsonHelper.jsonDecode<DPairingResponse>(payload)
                coIO { NearbyPairing.handlePairingResponse(response, senderIP) }
            }

            NearbyMessageType.PAIR_CANCEL -> {
                val cancel = JsonHelper.jsonDecode<DPairingCancel>(payload)
                NearbyPairing.handlePairingCancel(cancel)
            }
        }
    }

    // ---- Discovery logic -------------------------------------------------------

    private fun broadcastDiscover(request: DDiscoverRequest) {
        val message = "${NearbyMessageType.DISCOVER.toPrefix()}${JsonHelper.jsonEncode(request)}"
        NearbyNetwork.sendMulticast(message)
    }

    private suspend fun handleDiscoverRequest(payload: String, senderIP: String) {
        try {
            val request = JsonHelper.jsonDecode<DDiscoverRequest>(payload)
            if (request.toId.isNotEmpty()) {
                if (isDirectedQueryForUs(request)) {
                    sendDiscoverReply(senderIP)
                }
            } else {
                val discoverable = NearbyDiscoverablePreference.getAsync()
                if (discoverable) {
                    sendDiscoverReply(senderIP)
                }
            }
        } catch (e: Exception) {
            LogCat.e("Error handling discover request: ${e.message}")
        }
    }

    private fun sendDiscoverReply(targetIP: String) {
        val reply = DDiscoverReply(
            id = TempData.clientId,
            name = TempData.deviceName.value,
            deviceType = PhoneHelper.getDeviceType(appContext),
            port = TempData.httpsPort.value,
            version = getAppVersion(),
            platform = "android",
            ips = NetworkHelper.getDeviceIP4s().toList(),
        )
        val message = "${NearbyMessageType.DISCOVER_REPLY.toPrefix()}${JsonHelper.jsonEncode(reply)}"
        NearbyNetwork.sendUnicast(message, targetIP)
    }

    private fun handleDiscoverReply(payload: String) {
        try {
            val reply = JsonHelper.jsonDecode<DDiscoverReply>(payload)
            val device = DNearbyDevice(
                id = reply.id,
                name = reply.name,
                ips = reply.ips,
                port = reply.port,
                deviceType = reply.deviceType,
                version = reply.version,
                platform = reply.platform,
                lastSeen = TimeHelper.now(),
            )
            sendEvent(NearbyDeviceFoundEvent(device))
            sendEvent(WebSocketEvent(EventType.NEARBY_DEVICE_FOUND, JsonHelper.jsonEncode(device)))
            PeerStatusManager.setOnline(device.id, true)
            coIO {
                PeerManager.applyDeviceDiscovered(
                    deviceId = device.id,
                    ips = device.ips,
                    port = device.port,
                    name = device.name,
                    deviceType = device.deviceType,
                )
            }
        } catch (e: Exception) {
            LogCat.e("Error handling discover reply: ${e.message}")
        }
    }

    private suspend fun isDirectedQueryForUs(request: DDiscoverRequest): Boolean {
        if (request.fromId.isEmpty() || request.toId.isEmpty()) return false

        val peer = AppDatabase.instance.peerDao().getById(request.fromId)
        if (peer == null || peer.status != "paired") return false
        val decrypted = CryptoHelper.chaCha20Decrypt(
            peer.key,
            Base64.decode(request.toId, Base64.NO_WRAP),
        )
        return decrypted?.decodeToString() == TempData.clientId
    }
}
