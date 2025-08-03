package com.ismartcoding.plain.chat.discover

import android.util.Base64
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DDiscoverReply
import com.ismartcoding.plain.data.DDiscoverRequest
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.events.NearbyDeviceFoundEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

object NearbyDiscoverManager {
    private const val BROADCAST_INTERVAL = 5000L         // 5 seconds broadcast interval
    const val MULTICAST_PORT = 52352            // Same port as multicast for unicast replies

    private var periodicBroadcastJob: Job? = null
    private val networkManager = UdpMulticastManager()

    fun discoverSpecificDevice(toId: String, key: ByteArray) {
        sendDiscoveryMessage(
            DDiscoverRequest(
                fromId = TempData.clientId,
                toId = Base64.encodeToString(
                    CryptoHelper.chaCha20Encrypt(key, toId),
                    Base64.NO_WRAP
                )
            )
        )
        LogCat.d("Directed discovery sent for device: $toId")
    }

    fun startListener() {
        networkManager.startReceiver { message, senderIP ->
            handleReceivedMessage(message, senderIP)
        }
    }

    fun startPeriodicDiscovery() {
        if (periodicBroadcastJob?.isActive == true) return
        periodicBroadcastJob = coIO {
            while (true) {
                try {
                    sendDiscoveryMessage(DDiscoverRequest())
                } catch (e: Exception) {
                    LogCat.e("Error in periodic discovery: ${e.message}")
                }
                delay(BROADCAST_INTERVAL)
            }
        }
        LogCat.d("Started periodic discovery broadcasting")
    }

    fun stopPeriodicDiscovery() {
        periodicBroadcastJob?.cancel()
        periodicBroadcastJob = null
        LogCat.d("Stopped periodic discovery broadcasting")
    }

    private fun sendDiscoveryMessage(request: DDiscoverRequest) {
        val message = "${NearbyMessageType.DISCOVER.toPrefix()}${JsonHelper.jsonEncode(request)}"
        networkManager.sendMulticast(message)
    }

    private suspend fun sendDiscoveryReply(targetIP: String) {
        try {
            val context = MainApp.instance
            val deviceName = DeviceNamePreference.getAsync(context).ifEmpty {
                PhoneHelper.getDeviceName(context)
            }
            val reply = DDiscoverReply(
                id = TempData.clientId,
                name = deviceName,
                deviceType = PhoneHelper.getDeviceType(context),
                port = TempData.httpsPort,
                version = BuildConfig.VERSION_NAME,
                platform = "android"
            )

            val message = "${NearbyMessageType.DISCOVER_REPLY.toPrefix()}${JsonHelper.jsonEncode(reply)}"
            UdpUnicastManager.sendUnicast(message, targetIP, MULTICAST_PORT)
            LogCat.d("Discovery reply sent via unicast to $targetIP")
        } catch (e: Exception) {
            LogCat.e("Error sending discovery reply: ${e.message}")
        }
    }

    private fun handleReceivedMessage(message: String, senderIP: String) {
        val ownIPs = NetworkHelper.getDeviceIP4s()
        if (ownIPs.contains(senderIP)) return

        when {
            message.startsWith(NearbyMessageType.DISCOVER_REPLY.toPrefix()) -> {
                val replyMessage = message.removePrefix(NearbyMessageType.DISCOVER_REPLY.toPrefix())
                processDiscoveryReply(replyMessage, senderIP)
            }

            message.startsWith(NearbyMessageType.DISCOVER.toPrefix()) -> {
                val discoverMessage = message.removePrefix(NearbyMessageType.DISCOVER.toPrefix())
                coIO { processDiscoveryRequest(discoverMessage, senderIP) }
            }

            message.startsWith(NearbyMessageType.PAIR_REQUEST.toPrefix()) -> {
                val request = JsonHelper.jsonDecode<DPairingRequest>(message.removePrefix(NearbyMessageType.PAIR_REQUEST.toPrefix()))
                sendEvent(PairingRequestReceivedEvent(request, senderIP))
            }

            message.startsWith(NearbyMessageType.PAIR_RESPONSE.toPrefix()) -> {
                val response = JsonHelper.jsonDecode<DPairingResponse>(message.removePrefix(NearbyMessageType.PAIR_RESPONSE.toPrefix()))
                coIO {
                    NearbyPairManager.handlePairingResponse(response, senderIP)
                }
            }

            message.startsWith(NearbyMessageType.PAIR_CANCEL.toPrefix()) -> {
                val cancel = JsonHelper.jsonDecode<DPairingCancel>(message.removePrefix(NearbyMessageType.PAIR_CANCEL.toPrefix()))
                NearbyPairManager.handlePairingCancel(cancel)
            }
        }
    }

    private suspend fun processDiscoveryRequest(message: String, senderIP: String) {
        try {
            val request = JsonHelper.jsonDecode<DDiscoverRequest>(message)
            val context = MainApp.instance
            val isDiscoverable = NearbyDiscoverablePreference.getAsync(context)
            val shouldRespond = isDiscoverable || shouldRespondToDirectedQuery(request)
            if (shouldRespond) {
                sendDiscoveryReply(senderIP)
                LogCat.d("Sent discovery reply to $senderIP")
            } else {
                LogCat.d("Discovery request ignored from $senderIP")
            }
        } catch (e: Exception) {
            LogCat.e("Error processing discovery request: ${e.message}")
        }
    }

    private fun shouldRespondToDirectedQuery(request: DDiscoverRequest): Boolean {
        // Must include fromId and toId
        if (request.fromId.isEmpty() || request.toId.isEmpty()) {
            return false
        }

        // Check if fromId is a paired device
        val senderPeer = AppDatabase.instance.peerDao().getById(request.fromId)
        if (senderPeer == null || senderPeer.status != "paired") {
            LogCat.d("Unknown fromId: ${request.fromId}")
            return false
        }

        // Use shared key to decrypt toId
        try {
            val encryptedBytes = Base64.decode(request.toId, Base64.NO_WRAP)
            val decryptedBytes = CryptoHelper.chaCha20Decrypt(senderPeer.key, encryptedBytes)
            val decryptedToId = decryptedBytes?.decodeToString()
            if (decryptedToId == TempData.clientId) {
                LogCat.d("Directed query verified from ${request.fromId}")
                return true
            } else {
                LogCat.d("Decrypted toId does not match our ID")
                return false
            }
        } catch (e: Exception) {
            LogCat.e("Error decrypting toId: ${e.message}")
            return false
        }
    }

    private fun processDiscoveryReply(message: String, senderIP: String) {
        try {
            val reply = JsonHelper.jsonDecode<DDiscoverReply>(message)
            sendEvent(
                NearbyDeviceFoundEvent(
                    DNearbyDevice(
                        id = reply.id,
                        name = reply.name,
                        ip = senderIP,
                        deviceType = reply.deviceType,
                        version = reply.version,
                        platform = reply.platform,
                        lastSeen = Clock.System.now()
                    )
                )
            )
            LogCat.d("Device discovered: ${reply.name} at $senderIP")
        } catch (e: Exception) {
            LogCat.e("Error processing discovery reply: ${e.message}")
        }
    }
}