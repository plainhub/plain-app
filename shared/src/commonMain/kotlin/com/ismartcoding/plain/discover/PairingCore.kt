package com.ismartcoding.plain.discover

import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.ble.server.BlePairingSessionStore
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.data.DPairingSession
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PairingCanceledEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.crypto.ECDHKeyPair
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.getAppVersion
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.platform.getDeviceType
import com.ismartcoding.plain.platform.getPlatformName
import com.ismartcoding.plain.platform.isWifiAwareSupported
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object PairingCore {

    // ---- Discovery ----------------------------------------------------------

    fun buildDiscoverReply(): DDiscoverReply {
        return DDiscoverReply(
            id = TempData.clientId,
            name = TempData.deviceName.value.ifEmpty { getDeviceName() },
            deviceType = getDeviceType(),
            port = UserPrefs.httpsPort.value,
            version = getAppVersion(),
            platform = getPlatformName(),
            ips = getDeviceIP4s(),
            awareSupported = isWifiAwareSupported,
            awareRunning = TempData.awareRunning.value,
        )
    }

    fun formatMessage(type: NearbyMessageType, json: String): String {
        return "${type.toPrefix()}$json"
    }

    fun replyToDevice(reply: DDiscoverReply, bleClient: BleGattClient? = null): DNearbyDevice {
        return DNearbyDevice(
            id = reply.id,
            name = reply.name,
            ips = reply.ips,
            port = reply.port,
            deviceType = reply.deviceType,
            version = reply.version,
            platform = reply.platform,
            lastSeen = TimeHelper.now(),
            discoveryMethods = if (bleClient == null) setOf(DiscoveryMethod.LAN) else setOf(DiscoveryMethod.BLE),
            bleClient = bleClient,
        )
    }

    // ---- Initiator (send request / receive response) -----------------------

    suspend fun startPairingSession(device: DNearbyDevice, deviceIp: String): DPairingRequest {
        val (request, keyPair) = buildPairingRequest()
        PairingSessionStore.put(
            DPairingSession(
                deviceId = device.id,
                deviceName = device.name,
                deviceIp = deviceIp,
                devicePort = device.port,
                keyPair = keyPair,
            )
        )
        return request
    }

    suspend fun handlePairResponse(response: DPairingResponse, senderIp: String) {
        val session = PairingSessionStore.get(response.fromId)
        if (session == null) {
            LogCat.e("No active pairing session for ${response.fromId}")
            return
        }
        if (!RustPairingStore.validateResponse(response, session.deviceId)) {
            LogCat.w("Invalid pairing response ignored for ${response.fromId}")
            return
        }
        try {
            processPairingResponse(response, session, senderIp)
        } catch (e: Exception) {
            LogCat.e("Error processing pairing response: ${e.message}")
            notifyFailed(response.fromId, session.deviceName, "Failed to process pairing response")
        } finally {
            PairingSessionStore.remove(response.fromId)
        }
    }

    // ---- Responder (receive request / send response) -----------------------

    fun handlePairRequest(request: DPairingRequest, senderAddress: String, isBle: Boolean) {
        if (isBle) {
            // senderAddress on BLE is the MAC of the connected central. We
            // don't store it on the request anymore (peers are identified by
            // clientId, not MAC), but the GATT server still needs it for
            // routing notifications back to the connected device.
            BlePairingSessionStore.put(request.fromId, senderAddress)
        } else {
            request.fromIp = senderAddress
        }
        sendEvent(PairingRequestReceivedEvent(request))
        sendEvent(WebSocketEvent(EventType.PAIRING_REQUEST_RECEIVED, JsonHelper.jsonEncode(request)))
    }

    suspend fun buildRejectionResponse(request: DPairingRequest): DPairingResponse? = RustPairingStore.response(request, false)?.first

    fun handlePairCancel(cancel: DPairingCancel) {
        val session = PairingSessionStore.get(cancel.fromId)
        sendEvent(PairingCanceledEvent(cancel.fromId))
        sendEvent(
            WebSocketEvent(
                EventType.PAIRING_CANCELED,
                JsonHelper.jsonEncode(
                    DPairingResult(
                        deviceId = cancel.fromId,
                        deviceName = session?.deviceName ?: "",
                    )
                )
            )
        )
        PairingSessionStore.remove(cancel.fromId)
    }

    // ---- Core pairing logic (existing) -------------------------------------

    suspend fun buildPairingRequest(): Pair<DPairingRequest, ECDHKeyPair> = RustPairingStore.request()

    suspend fun acceptPairingRequest(request: DPairingRequest): DPairingResponse? {
        val built = RustPairingStore.response(request, true) ?: return null
        val response = built.first
        val keyPair = requireNotNull(built.second)
        PairingSessionStore.put(DPairingSession(deviceId = request.fromId, deviceName = request.fromName, deviceIp = request.fromIp, devicePort = request.port, keyPair = keyPair))

        val requestEcdhPublicKey = Base64Lenient.decode(request.ecdhPublicKey)
        val encryptKey = RustPairingStore.derive(keyPair.privateKeyEncoded, requestEcdhPublicKey)
        if (encryptKey == null) {
            PairingSessionStore.remove(request.fromId)
            return null
        }

        val peerIps = (listOf(request.fromIp) + request.ips).filter { it.isNotEmpty() }.distinct()
        PairingPeerStore.save(
            deviceId = request.fromId,
            deviceName = request.fromName,
            deviceIps = peerIps,
            port = request.port,
            deviceType = request.deviceType,
            key = encryptKey,
            signaturePublicKey = request.signaturePublicKey,
        )
        NearbyViewModel.handlePairingSuccess(request.fromId)
        sendEvent(PairingSuccessEvent(request.fromId, request.fromName, request.fromIp, encryptKey))
        sendEvent(
            WebSocketEvent(
                EventType.PAIRING_SUCCESS,
                JsonHelper.jsonEncode(DPairingResult(deviceId = request.fromId, deviceName = request.fromName)),
            )
        )
        return response
    }

    suspend fun processPairingResponse(
        response: DPairingResponse,
        session: DPairingSession,
        senderIp: String,
    ): Boolean {
        if (!RustPairingStore.validateResponse(response, session.deviceId)) {
            notifyFailed(response.fromId, session.deviceName, "Invalid pairing response")
            return false
        }

        if (response.accepted) {
            val responseEcdhPublicKey = Base64Lenient.decode(response.ecdhPublicKey)
            val encryptKey = RustPairingStore.derive(session.keyPair.privateKeyEncoded, responseEcdhPublicKey)
            if (encryptKey == null) {
                notifyFailed(response.fromId, session.deviceName, "Failed to compute shared key")
                return false
            }
            val peerIps = (listOf(senderIp) + response.ips).filter { it.isNotEmpty() }.distinct()
            PairingPeerStore.save(
                deviceId = response.fromId,
                deviceName = session.deviceName,
                deviceIps = peerIps,
                port = response.port,
                deviceType = response.deviceType,
                key = encryptKey,
                signaturePublicKey = response.signaturePublicKey,
            )
            NearbyViewModel.handlePairingSuccess(response.fromId)
            sendEvent(PairingSuccessEvent(response.fromId, session.deviceName, senderIp, encryptKey))
            sendEvent(
                WebSocketEvent(
                    EventType.PAIRING_SUCCESS,
                    JsonHelper.jsonEncode(DPairingResult(deviceId = response.fromId, deviceName = session.deviceName)),
                )
            )
            LogCat.d("Pairing completed successfully with ${session.deviceName}")
            return true
        } else {
            notifyFailed(response.fromId, session.deviceName, "Pairing request was rejected")
            LogCat.d("Verified pairing rejection from ${session.deviceName}")
            return false
        }
    }

    fun notifyFailed(deviceId: String, deviceName: String, reason: String) {
        NearbyViewModel.itemStatus.remove(deviceId)
        sendEvent(
            WebSocketEvent(
                EventType.PAIRING_FAILED,
                JsonHelper.jsonEncode(DPairingResult(deviceId = deviceId, deviceName = deviceName, error = reason)),
            )
        )
    }

}
