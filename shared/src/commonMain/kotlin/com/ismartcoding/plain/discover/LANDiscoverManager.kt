package com.ismartcoding.plain.discover

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.data.DDiscoverReply
import com.ismartcoding.plain.data.DDiscoverRequest
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.NearbyNetwork
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalEncodingApi::class)
object LANDiscoverManager {
    private const val BROADCAST_INTERVAL_MS = 5_000L

    private var broadcastJob: Job? = null
    private var restartJob: Job? = null

    fun startReceiver() {
        NearbyNetwork.startReceiver(::onDatagram)
    }

    fun startPeriodicDiscovery() {
        if (broadcastJob?.isActive == true) return
        sendEvent(WebSocketEvent(EventType.NEARBY_DISCOVERY_STARTED, "{}"))
        broadcastJob = coIO {
            while (isActive) {
                runCatching { broadcastDiscover(DDiscoverRequest()) }
                    .onFailure { LogCat.e("Periodic discovery error: ${it.message}") }
                delay(BROADCAST_INTERVAL_MS.milliseconds)
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
                toId = Base64.encode(chaCha20Encrypt(key, toId)),
            )
        )
    }

    fun scheduleRestart(reason: String) {
        restartJob?.cancel()
        restartJob = coIO {
            delay(1_500.milliseconds) // debounce rapid network churn
            LogCat.d("Network change ($reason) — restarting multicast listener")
            NearbyNetwork.stopReceiver()
            NearbyNetwork.startReceiver(::onDatagram)
        }
    }

    // ---- Message routing -------------------------------------------------------

    private fun onDatagram(message: String, senderIP: String) {
        if (getDeviceIP4s().contains(senderIP)) return

        val type = NearbyMessageType.entries.firstOrNull { message.startsWith(it.toPrefix()) } ?: return
        val payload = message.removePrefix(type.toPrefix())

        when (type) {
            NearbyMessageType.DISCOVER -> coIO { handleDiscoverRequest(payload, senderIP) }
            NearbyMessageType.DISCOVER_REPLY -> handleDiscoverReply(payload)
            NearbyMessageType.PAIR_REQUEST -> {
                val request = JsonHelper.jsonDecode<DPairingRequest>(payload)
                PairingCore.handlePairRequest(request, senderIP, isBle = false)
            }

            NearbyMessageType.PAIR_RESPONSE -> {
                val response = JsonHelper.jsonDecode<DPairingResponse>(payload)
                coIO { PairingCore.handlePairResponse(response, senderIP) }
            }

            NearbyMessageType.PAIR_CANCEL -> {
                PairingCore.handlePairCancel(JsonHelper.jsonDecode(payload))
            }
        }
    }

    // ---- Discovery logic -------------------------------------------------------

    private fun broadcastDiscover(request: DDiscoverRequest) {
        NearbyNetwork.sendMulticast(PairingCore.formatMessage(NearbyMessageType.DISCOVER, JsonHelper.jsonEncode(request)))
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
        val message = PairingCore.formatMessage(NearbyMessageType.DISCOVER_REPLY, JsonHelper.jsonEncode(PairingCore.buildDiscoverReply()))
        NearbyNetwork.sendUnicast(message, targetIP)
    }

    private fun handleDiscoverReply(payload: String) {
        try {
            val reply = JsonHelper.jsonDecode<DDiscoverReply>(payload)
            val device = PairingCore.replyToDevice(reply)
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
        val decrypted = chaCha20Decrypt(
            Base64Lenient.decode(peer.key),
            Base64Lenient.decode(request.toId),
        )
        return decrypted?.decodeToString() == TempData.clientId
    }
}
