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
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.data.DPairingSession
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.PairingCanceledEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.SignatureHelper

object PairingResponder {

    suspend fun respond(request: DPairingRequest, accepted: Boolean) = withIO {
        try {
            // Verify timestamp to prevent replay attacks
            if (!PairingSecurity.validateTimestamp(request.timestamp)) {
                LogCat.e("Pairing request timestamp is too old or in the future")
                notifyFailed(request.fromId, request.fromName, "Invalid timestamp")
                return@withIO
            }

            // Verify signature
            if (!PairingSecurity.verify(request)) {
                LogCat.e("Pairing request signature verification failed")
                notifyFailed(request.fromId, request.fromName, "Signature verification failed")
                return@withIO
            }

            LogCat.d("Pairing request signature verified successfully")

            if (accepted) {
                // Generate ECDH key pair for this pairing session
                val keyPair = CryptoHelper.generateECDHKeyPair()

                // Get our raw Ed25519 signature public key (32 bytes)
                val signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async()

                val session = DPairingSession(
                    deviceId = request.fromId,
                    deviceName = request.fromName,
                    deviceIp = request.fromIp,
                    keyPair = keyPair,
                )
                PairingSessionStore.put(session)

                val responseTimestamp = System.currentTimeMillis()
                val ecdhPublicKeyBytes = CryptoHelper.getPublicKeyBytes(keyPair)
                val ecdhPublicKey = Base64.encodeToString(ecdhPublicKeyBytes, Base64.NO_WRAP)

                val response = DPairingResponse(
                    fromId = TempData.clientId,
                    toId = request.fromId,
                    port = TempData.httpsPort.value,
                    deviceType = PhoneHelper.getDeviceType(appContext),
                    ecdhPublicKey = ecdhPublicKey,
                    signaturePublicKey = signaturePublicKey,
                    accepted = true,
                    timestamp = responseTimestamp,
                    ips = NetworkHelper.getDeviceIP4s().toList(),
                )

                response.signature = SignatureHelper.signTextAsync(response.toSignatureData())

                val requestEcdhPublicKey = Base64.decode(request.ecdhPublicKey, Base64.NO_WRAP)
                val encryptKey = CryptoHelper.computeECDHSharedKey(keyPair.private, requestEcdhPublicKey)
                if (encryptKey != null) {
                    // Store peer in database with signature public key
                    val peerIps = (listOf(request.fromIp) + request.ips).distinct()
                    PairingPeerStore.save(
                        deviceId = request.fromId,
                        deviceName = request.fromName,
                        deviceIps = peerIps,
                        port = request.port,
                        deviceType = request.deviceType,
                        key = encryptKey,
                        signaturePublicKey = request.signaturePublicKey,
                    )
                    PairingMessenger.sendResponse(response, request.fromIp)
                    sendEvent(PairingSuccessEvent(request.fromId, request.fromName, request.fromIp, encryptKey))
                    sendEvent(
                        WebSocketEvent(
                            EventType.PAIRING_SUCCESS,
                            JsonHelper.jsonEncode(
                                DPairingResult(
                                    deviceId = response.fromId,
                                    deviceName = request.fromName,
                                )
                            )
                        )
                    )
                } else {
                    throw Exception("Failed to compute shared key")
                }
            } else {
                // Send rejection response with signature for security
                val signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async()
                val rejectionTimestamp = System.currentTimeMillis()

                val response = DPairingResponse(
                    fromId = TempData.clientId,
                    toId = request.fromId,
                    port = TempData.httpsPort.value,
                    deviceType = request.deviceType,
                    ecdhPublicKey = "",
                    signaturePublicKey = signaturePublicKey,
                    accepted = false,
                    timestamp = rejectionTimestamp,
                )

                response.signature = SignatureHelper.signTextAsync(response.toSignatureData())

                PairingMessenger.sendResponse(response, request.fromIp)
                LogCat.d("Signed pairing rejection response sent to ${request.fromName}")
            }
        } catch (e: Exception) {
            LogCat.e("Error responding to pairing: ${e.message}")
            notifyFailed(request.fromId, request.fromName, "Failed to respond to pairing request")
        } finally {
            // Clean up session if not accepted
            if (!accepted) {
                PairingSessionStore.remove(request.fromId)
            }
        }
    }

    fun onCancel(cancel: DPairingCancel) {
        LogCat.d("Pairing cancelled by remote device: ${cancel.fromId}")
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
        // Clean up session if exists
        PairingSessionStore.remove(cancel.fromId)
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
