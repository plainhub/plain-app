package com.ismartcoding.plain.chat.discover

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkRequest
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
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Central manager for nearby device discovery.
 *
 * Responsibilities:
 *  - Lifecycle: start/stop the multicast listener and network watcher.
 *  - Discovery: periodic broadcast + directed queries, reply to incoming requests.
 *  - Message routing: dispatch incoming datagrams to discovery or [NearbyPairManager].
 */
object NearbyDiscoverManager {
    private const val BROADCAST_INTERVAL_MS = 5_000L

    private var broadcastJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var restartJob: Job? = null

    // ---- Lifecycle -------------------------------------------------------------

    /**
     * Start the multicast listener **and** register a network-change watcher so the
     * socket is recreated whenever WiFi (dis)connects.  Call once at app startup.
     */
    fun start() {
        NearbyNetwork.startReceiver(::onDatagram)
        registerNetworkWatcher()
    }

    // ---- Periodic broadcast discovery ------------------------------------------

    fun startPeriodicDiscovery() {
        if (broadcastJob?.isActive == true) return
        broadcastJob = coIO {
            while (true) {
                runCatching { broadcastDiscover(DDiscoverRequest()) }
                    .onFailure { LogCat.e("Periodic discovery error: ${it.message}") }
                delay(BROADCAST_INTERVAL_MS)
            }
        }
    }

    fun stopPeriodicDiscovery() {
        broadcastJob?.cancel()
        broadcastJob = null
    }

    /** Send a directed discovery to a specific paired device. */
    fun discoverSpecificDevice(toId: String, key: ByteArray) {
        broadcastDiscover(
            DDiscoverRequest(
                fromId = TempData.clientId,
                toId = Base64.encodeToString(CryptoHelper.chaCha20Encrypt(key, toId), Base64.NO_WRAP),
            )
        )
    }

    // ---- Network watcher -------------------------------------------------------

    private fun registerNetworkWatcher() {
        if (networkCallback != null) return
        val cm = MainApp.instance.getSystemService(ConnectivityManager::class.java) ?: return

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = scheduleRestart("onAvailable")
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) =
                scheduleRestart("onLinkPropertiesChanged")
        }
        runCatching { cm.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback!!) }
            .onFailure {
                LogCat.e("Network callback registration failed: ${it.message}")
                networkCallback = null
            }
    }

    private fun scheduleRestart(reason: String) {
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
                sendEvent(PairingRequestReceivedEvent(request, senderIP))
            }

            NearbyMessageType.PAIR_RESPONSE -> {
                val response = JsonHelper.jsonDecode<DPairingResponse>(payload)
                coIO { NearbyPairManager.handlePairingResponse(response, senderIP) }
            }

            NearbyMessageType.PAIR_CANCEL -> {
                val cancel = JsonHelper.jsonDecode<DPairingCancel>(payload)
                NearbyPairManager.handlePairingCancel(cancel)
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
                val discoverable = NearbyDiscoverablePreference.getAsync(MainApp.instance)
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
            name = TempData.deviceName,
            deviceType = PhoneHelper.getDeviceType(MainApp.instance),
            port = TempData.httpsPort,
            version = BuildConfig.VERSION_NAME,
            platform = "android",
            ips = NetworkHelper.getDeviceIP4s().toList(),
        )
        val message = "${NearbyMessageType.DISCOVER_REPLY.toPrefix()}${JsonHelper.jsonEncode(reply)}"
        NearbyNetwork.sendUnicast(message, targetIP)
    }

    private fun handleDiscoverReply(payload: String) {
        try {
            val reply = JsonHelper.jsonDecode<DDiscoverReply>(payload)
            sendEvent(
                NearbyDeviceFoundEvent(
                    DNearbyDevice(
                        id = reply.id,
                        name = reply.name,
                        ips = reply.ips,
                        port = reply.port,
                        deviceType = reply.deviceType,
                        version = reply.version,
                        platform = reply.platform,
                        lastSeen = TimeHelper.now(),
                    )
                )
            )
        } catch (e: Exception) {
            LogCat.e("Error handling discover reply: ${e.message}")
        }
    }

    private fun isDirectedQueryForUs(request: DDiscoverRequest): Boolean {
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
