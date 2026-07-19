package com.ismartcoding.plain.ble
import com.ismartcoding.plain.platform.bleTransport

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DDiscoverReply
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.discover.PairingCore
import com.ismartcoding.plain.discover.PairingSessionStore
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ble.client.BleDeviceApi
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.ble.server.BleGattServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object PairingTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: BleGattServer? = null
    private var awareObserverJob: Job? = null

    fun startAdvertising() {
        if (server != null) return
        val s = bleTransport().createServer()
        s.start()
        server = s
        startAwareObserver()
    }

    fun stopAdvertising() {
        awareObserverJob?.cancel()
        awareObserverJob = null
        server?.stop()
        server = null
    }

    fun refreshAdvertising() {
        server?.refreshAdvertising()
    }

    fun sendNotification(mac: String, charUuid: String, value: String): Boolean {
        return server?.sendNotification(mac, charUuid, value) ?: false
    }

    private fun startAwareObserver() {
        if (awareObserverJob?.isActive == true) return
        awareObserverJob = scope.launch {
            TempData.awareRunning.drop(1).collect {
                refreshAdvertising()
            }
        }
    }

    fun scanAndDiscover(): Flow<DNearbyDevice> = flow {
        val replyCache = mutableMapOf<String, DDiscoverReply>()
        val lastEmit = mutableMapOf<String, Long>()
        bleTransport().createScanner().scan(BleUuids.SERVICE_UUID).collect { device ->
            // device.id is the peer's clientId (parsed from scan response
            // serviceData) — the stable peer identifier. The BLE MAC is no
            // longer cached because Android randomizes it every ~15 min and
            // we identify peers by clientId throughout.
            val clientId = device.id
            val cached = replyCache[clientId]
            if (cached == null) {
                val reply = readDiscoverReply(device)
                if (reply != null) {
                    replyCache[clientId] = reply
                    // Cache the peer's Aware flags from the scan response serviceData:
                    //  - awareSupported: whether the peer supports Wi-Fi Aware (FLAG_AWARE_SUPPORTED)
                    //  - awareRunning: whether the peer's Aware service is currently running (FLAG_AWARE_RUNNING)
                    // WifiAwareTransport uses awareRunning to skip itself when the peer's Aware isn't
                    // running (avoids 10s+ buildLink timeout). PeerTransportPrewarmer uses awareSupported
                    // to decide whether to send a startAware mutation via BLE.
                    val flags = device.awareFlags
                    val awareSupported = (flags and FLAG_AWARE_SUPPORTED) != 0
                    val awareRunning = (flags and FLAG_AWARE_RUNNING) != 0
                    PeerCacher.setAwareSupported(reply.id, awareSupported)
                    PeerCacher.setAwareRunning(reply.id, awareRunning)
                    emit(PairingCore.replyToDevice(reply, device))
                    lastEmit[clientId] = TimeHelper.nowMillis()
                }
            } else {
                val now = TimeHelper.nowMillis()
                if (now - (lastEmit[clientId] ?: 0) > BLE_REFRESH_INTERVAL_MS) {
                    emit(PairingCore.replyToDevice(cached, device))
                    lastEmit[clientId] = now
                }
            }
        }
    }

    private suspend fun readDiscoverReply(device: BleGattClient): DDiscoverReply? {
        return try {
            val api = BleDeviceApi(device)
            api.ensureConnected()
            if (!api.isConnected()) return null

            val requestData = BleRequestData.create().copy(
                body = PairingCore.formatMessage(NearbyMessageType.DISCOVER, "")
            )
            val result = api.requestAsync(BleServices.nearby, requestData)
            if (!result.isSuccess()) {
                LogCat.e("[BLE] readDiscoverReply failed: ${result.status}")
                return null
            }
            val json = result.value as? String ?: return null
            if (json.isEmpty()) return null
            JsonHelper.jsonDecode<DDiscoverReply>(json)
        } catch (e: Exception) {
            LogCat.e("[BLE] readDiscoverReply error: ${e.message}")
            null
        } finally {
            bleTransport().createScanner().teardownConnection(device)
        }
    }

    suspend fun pairViaBle(device: DNearbyDevice): Boolean {
        val bleDevice = device.bleClient ?: run {
            LogCat.e("BLE pairViaBle: no bleClient for ${device.id}")
            return false
        }

        var notificationEnabled = false
        return try {
            val api = BleDeviceApi(bleDevice)
            api.ensureConnected()
            if (!api.isConnected()) {
                LogCat.e("BLE pairViaBle: connection failed")
                return false
            }

            val request = PairingCore.startPairingSession(device, deviceIp = "")
            val body = PairingCore.formatMessage(NearbyMessageType.PAIR_REQUEST, JsonHelper.jsonEncode(request))
            LogCat.d("BLE pairViaBle: sending request to ${device.name}")

            if (!api.sendRequest(BleServices.nearby, BleRequestData.create().copy(body = body))) {
                LogCat.e("BLE pairViaBle: failed to send pairing request")
                PairingCore.notifyFailed(device.id, device.name, "Failed to send pairing request")
                return false
            }
            notificationEnabled = true
            LogCat.d("BLE pairViaBle: request sent, waiting for PAIR_RESPONSE notification")

            val responseJson = waitForPairResponseNotification(bleDevice)

            bleDevice.setNotification(BleServices.nearby, false)
            notificationEnabled = false

            if (responseJson != null) {
                val response = JsonHelper.jsonDecode<DPairingResponse>(responseJson)
                // The responder is identified by its clientId (parsed from the
                // BLE scan response serviceData), which is already on the
                // DPairingResponse.fromId — no BLE MAC needs to be cached.
                PairingCore.handlePairResponse(
                    response = response,
                    senderIp = "",
                )
            } else {
                LogCat.e("BLE pairViaBle: response timeout after ${PAIR_RESPONSE_TIMEOUT_MS}ms")
                PairingCore.notifyFailed(device.id, device.name, "Pairing timed out")
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogCat.e("BLE pairViaBle error: ${e.message}")
            PairingCore.notifyFailed(device.id, device.name, "Pairing failed: ${e.message}")
            false
        } finally {
            withContext(NonCancellable) {
                if (notificationEnabled) {
                    try { bleDevice.setNotification(BleServices.nearby, false) } catch (_: Exception) {}
                }
            }
            bleTransport().createScanner().teardownConnection(bleDevice)
            PairingSessionStore.remove(device.id)
        }
    }

    private suspend fun waitForPairResponseNotification(bleDevice: BleGattClient): String? {
        val startTime = TimeHelper.nowMillis()
        while (true) {
            val remaining = PAIR_RESPONSE_TIMEOUT_MS - (TimeHelper.nowMillis() - startTime)
            if (remaining <= 0) return null
            val notification = bleDevice.waitForNotification(BleServices.nearby, remaining)
            if (notification == null) return null
            if (notification.startsWith(NearbyMessageType.PAIR_RESPONSE.toPrefix())) {
                return notification.removePrefix(NearbyMessageType.PAIR_RESPONSE.toPrefix())
            }
            LogCat.d("BLE pairViaBle: ignoring non-PAIR_RESPONSE notification: ${notification.take(20)}")
        }
    }
}

private const val BLE_REFRESH_INTERVAL_MS = 5_000L
private const val PAIR_RESPONSE_TIMEOUT_MS = 90_000L
// Bit flags matching AndroidBleGattServer.buildAwareFlags():
//   0x01 = peer supports Wi-Fi Aware
//   0x02 = peer's Wi-Fi Aware service is currently running
private const val FLAG_AWARE_SUPPORTED = 0x01
private const val FLAG_AWARE_RUNNING = 0x02
