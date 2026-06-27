package com.ismartcoding.plain.discover

import android.util.Base64
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.helpers.NetworkHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.data.DPairingSession
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.SignatureHelper

object PairingInitiator {

    suspend fun start(device: DNearbyDevice) = withIO {
        try {
            val context = appContext
            val deviceName = TempData.deviceName.value

            // Generate ECDH key pair for this pairing session
            val keyPair = CryptoHelper.generateECDHKeyPair()

            // Get our raw Ed25519 signature public key (32 bytes)
            val signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async()

            val bestIp = com.ismartcoding.plain.lib.helpers.NetworkHelper.getBestIp(device.ips)
            val session = DPairingSession(
                deviceId = device.id,
                deviceName = device.name,
                deviceIp = bestIp,
                keyPair = keyPair,
            )
            PairingSessionStore.put(session)

            val currentTimestamp = System.currentTimeMillis()
            val ecdhPublicKeyBytes = CryptoHelper.getPublicKeyBytes(keyPair)
            val ecdhPublicKey = Base64.encodeToString(ecdhPublicKeyBytes, Base64.NO_WRAP)

            val request = DPairingRequest(
                fromId = TempData.clientId,
                fromName = deviceName,
                port = TempData.httpsPort.value,
                deviceType = PhoneHelper.getDeviceType(context),
                ecdhPublicKey = ecdhPublicKey,
                signaturePublicKey = signaturePublicKey,
                timestamp = currentTimestamp,
                ips = NetworkHelper.getDeviceIP4s().toList(),
            )
            request.signature = SignatureHelper.signTextAsync(request.toSignatureData())

            PairingMessenger.sendRequest(request, bestIp)
        } catch (e: Exception) {
            LogCat.e("Error starting pairing: ${e.message}")
            notifyFailed(device.id, device.name, "Failed to send pairing request")
        }
    }

    suspend fun onResponse(response: DPairingResponse, senderIp: String) {
        val session = PairingSessionStore.get(response.fromId)
        if (session == null) {
            LogCat.e("No active pairing session found for device ${response.fromId}")
            return
        }

        try {
            // Verify timestamp to prevent replay attacks
            if (!PairingSecurity.validateTimestamp(response.timestamp)) {
                LogCat.e("Pairing response timestamp is too old or in the future")
                notifyFailed(response.fromId, session.deviceName, "Invalid timestamp")
                return
            }

            // Verify signature for all responses (acceptance and rejection)
            if (!PairingSecurity.verify(response)) {
                LogCat.e("Pairing response signature verification failed")
                notifyFailed(response.fromId, session.deviceName, "Signature verification failed")
                return
            }

            LogCat.d("Pairing response signature verified successfully")

            if (response.accepted) {
                val responseEcdhPublicKey = Base64.decode(response.ecdhPublicKey, Base64.NO_WRAP)
                val encryptKey = CryptoHelper.computeECDHSharedKey(session.keyPair.private, responseEcdhPublicKey)
                if (encryptKey != null) {
                    val peerIps = (listOf(senderIp) + response.ips).distinct()
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
                            JsonHelper.jsonEncode(
                                DPairingResult(
                                    deviceId = response.fromId,
                                    deviceName = session.deviceName,
                                )
                            )
                        )
                    )
                    LogCat.d("Pairing completed successfully with ${session.deviceName}")
                } else {
                    throw Exception("Failed to compute shared key")
                }
            } else {
                notifyFailed(response.fromId, session.deviceName, "Pairing request was rejected")
                LogCat.d("Verified pairing rejection from ${session.deviceName}")
            }
        } catch (e: Exception) {
            LogCat.e("Error processing pairing response: ${e.message}")
            notifyFailed(response.fromId, session.deviceName, "Failed to process pairing response")
        } finally {
            // Clean up session
            PairingSessionStore.remove(response.fromId)
        }
    }

    fun cancel(deviceId: String) {
        val session = PairingSessionStore.get(deviceId)
        if (session != null) {
            try {
                val cancelMessage = DPairingCancel(
                    fromId = TempData.clientId,
                    toId = deviceId,
                )
                PairingMessenger.sendCancel(cancelMessage, session.deviceIp)
                LogCat.d("Pairing cancel message sent to ${session.deviceName}")
            } catch (e: Exception) {
                LogCat.e("Error sending pairing cancel message: ${e.message}")
            }
        }

        PairingSessionStore.remove(deviceId)
        notifyFailed(deviceId, session?.deviceName ?: "", "Pairing cancelled by user")
        LogCat.d("Pairing cancelled for device: $deviceId")
    }

    private fun notifyFailed(deviceId: String, deviceName: String, reason: String) {
        val event = PairingFailedEvent(deviceId, reason)
        sendEvent(event)
        sendEvent(
            WebSocketEvent(
                EventType.PAIRING_FAILED,
                JsonHelper.jsonEncode(
                    DPairingResult(
                        deviceId = event.deviceId,
                        deviceName = deviceName,
                        error = event.reason,
                    )
                )
            )
        )
    }
}
