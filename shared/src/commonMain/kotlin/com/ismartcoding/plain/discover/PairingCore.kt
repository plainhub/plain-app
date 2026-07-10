package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.ble.server.BlePairingSessionStore
import com.ismartcoding.plain.crypto.PairingCrypto
import com.ismartcoding.plain.data.DDiscoverReply
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.data.DPairingSession
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PairingCanceledEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.getAppVersion
import com.ismartcoding.plain.platform.getDeviceIP4s
import com.ismartcoding.plain.platform.getDeviceName
import com.ismartcoding.plain.platform.getDeviceType
import com.ismartcoding.plain.platform.getPlatformName
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object PairingCore {

    // ---- Discovery ----------------------------------------------------------

    fun buildDiscoverReply(): DDiscoverReply {
        return DDiscoverReply(
            id = TempData.clientId,
            name = TempData.deviceName.value.ifEmpty { getDeviceName() },
            deviceType = getDeviceType(),
            port = TempData.httpsPort.value,
            version = getAppVersion(),
            platform = getPlatformName(),
            ips = getDeviceIP4s(),
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
            discoveredViaLan = bleClient == null,
            discoveredViaBle = bleClient != null,
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
            request.bleMac = senderAddress
            BlePairingSessionStore.put(request.fromId, senderAddress)
        } else {
            request.fromIp = senderAddress
        }
        sendEvent(PairingRequestReceivedEvent(request))
        sendEvent(WebSocketEvent(EventType.PAIRING_REQUEST_RECEIVED, JsonHelper.jsonEncode(request)))
    }

    suspend fun buildRejectionResponse(request: DPairingRequest): DPairingResponse? {
        if (!validatePairingRequest(request)) return null
        val response = DPairingResponse(
            fromId = TempData.clientId,
            toId = request.fromId,
            port = TempData.httpsPort.value,
            deviceType = request.deviceType,
            ecdhPublicKey = "",
            signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async(),
            accepted = false,
            timestamp = TimeHelper.nowMillis(),
            ips = getDeviceIP4s(),
        )
        response.signature = SignatureHelper.signTextAsync(response.toSignatureData())
        return response
    }

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

    suspend fun buildPairingRequest(): Pair<DPairingRequest, com.ismartcoding.plain.crypto.ECDHKeyPair> {
        val keyPair = PairingCrypto.generateECDHKeyPair()
        val ecdhPublicKey = Base64.encode(keyPair.publicKeyEncoded)
        val request = DPairingRequest(
            fromId = TempData.clientId,
            fromName = TempData.deviceName.value,
            port = TempData.httpsPort.value,
            deviceType = getDeviceType(),
            ecdhPublicKey = ecdhPublicKey,
            signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async(),
            timestamp = TimeHelper.nowMillis(),
            ips = getDeviceIP4s(),
        )
        request.signature = SignatureHelper.signTextAsync(request.toSignatureData())
        return request to keyPair
    }

    suspend fun acceptPairingRequest(request: DPairingRequest): DPairingResponse? {
        if (!validatePairingRequest(request)) return null

        val keyPair = PairingCrypto.generateECDHKeyPair()
        PairingSessionStore.put(
            DPairingSession(
                deviceId = request.fromId,
                deviceName = request.fromName,
                deviceIp = request.fromIp,
                keyPair = keyPair,
            )
        )

        val response = DPairingResponse(
            fromId = TempData.clientId,
            toId = request.fromId,
            port = TempData.httpsPort.value,
            deviceType = getDeviceType(),
            ecdhPublicKey = Base64.encode(keyPair.publicKeyEncoded),
            signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async(),
            accepted = true,
            timestamp = TimeHelper.nowMillis(),
            ips = getDeviceIP4s(),
        )
        response.signature = SignatureHelper.signTextAsync(response.toSignatureData())

        val requestEcdhPublicKey = Base64.decode(request.ecdhPublicKey)
        val encryptKey = PairingCrypto.computeECDHSharedKey(keyPair.privateKeyEncoded, requestEcdhPublicKey)
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
        sendEvent(PairingSuccessEvent(request.fromId, request.fromName, request.fromIp, encryptKey))
        sendEvent(
            WebSocketEvent(
                EventType.PAIRING_SUCCESS,
                JsonHelper.jsonEncode(DPairingResult(deviceId = response.fromId, deviceName = request.fromName)),
            )
        )
        return response
    }

    suspend fun processPairingResponse(
        response: DPairingResponse,
        session: DPairingSession,
        senderIp: String,
    ): Boolean {
        if (!PairingSecurity.validateTimestamp(response.timestamp)) {
            LogCat.e("Pairing response timestamp is too old or in the future")
            notifyFailed(response.fromId, session.deviceName, "Invalid timestamp")
            return false
        }
        if (!PairingSecurity.verify(response)) {
            LogCat.e("Pairing response signature verification failed")
            notifyFailed(response.fromId, session.deviceName, "Signature verification failed")
            return false
        }
        LogCat.d("Pairing response signature verified successfully")

        if (response.accepted) {
            val responseEcdhPublicKey = Base64.decode(response.ecdhPublicKey)
            val encryptKey = PairingCrypto.computeECDHSharedKey(session.keyPair.privateKeyEncoded, responseEcdhPublicKey)
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
        val event = PairingFailedEvent(deviceId, reason)
        sendEvent(event)
        sendEvent(
            WebSocketEvent(
                EventType.PAIRING_FAILED,
                JsonHelper.jsonEncode(DPairingResult(deviceId = event.deviceId, deviceName = deviceName, error = event.reason)),
            )
        )
    }

    private fun validatePairingRequest(request: DPairingRequest): Boolean {
        if (!PairingSecurity.validateTimestamp(request.timestamp)) {
            LogCat.e("Pairing request timestamp is too old or in the future")
            return false
        }
        if (!PairingSecurity.verify(request)) {
            LogCat.e("Pairing request signature verification failed")
            return false
        }
        LogCat.d("Pairing request signature verified successfully")
        return true
    }
}
