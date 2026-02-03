package com.ismartcoding.plain.chat.discover

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.data.DPairingSession
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.events.PairingCancelledEvent
import com.ismartcoding.plain.events.PairingFailedEvent
import com.ismartcoding.plain.events.PairingSuccessEvent
import com.ismartcoding.plain.chat.discover.NearbyDiscoverManager.MULTICAST_PORT
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.preferences.DeviceNamePreference
import com.ismartcoding.plain.web.ChatApiManager
import java.util.concurrent.ConcurrentHashMap
import android.util.Base64
import kotlin.math.abs

object NearbyPairManager {
    private val activePairingSessions = ConcurrentHashMap<String, DPairingSession>()
    
    // Maximum allowed time difference for timestamp validation (5 minutes)
    private const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    private fun verifyPairingRequestSignature(request: DPairingRequest): Boolean {
        return try {
            val signatureData = request.toSignatureData()
            val signatureBytes = Base64.decode(request.signature, Base64.NO_WRAP)
            val rawPublicKey = Base64.decode(request.signaturePublicKey, Base64.NO_WRAP)
            CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, signatureData.toByteArray(), signatureBytes)
        } catch (e: Exception) {
            LogCat.e("Failed to verify pairing request signature: ${e.message}")
            false
        }
    }

    private fun verifyPairingResponseSignature(response: DPairingResponse): Boolean {
        return try {
            val signatureData = response.toSignatureData()
            val signatureBytes = Base64.decode(response.signature, Base64.NO_WRAP)
            val rawPublicKey = Base64.decode(response.signaturePublicKey, Base64.NO_WRAP)
            CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, signatureData.toByteArray(), signatureBytes)
        } catch (e: Exception) {
            LogCat.e("Failed to verify pairing response signature: ${e.message}")
            false
        }
    }

    suspend fun startPairingAsync(device: DNearbyDevice) {
        try {
            val context = MainApp.instance
            val deviceName = DeviceNamePreference.getAsync(context).ifEmpty {
                PhoneHelper.getDeviceName(context)
            }

            // Generate ECDH key pair for this pairing session
            val keyPair = CryptoHelper.generateECDHKeyPair()
            
            // Get our raw Ed25519 signature public key (32 bytes)
            val signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async()
                ?: throw Exception("Failed to get raw Ed25519 public key")

            // Create pairing session
            val session = DPairingSession(
                deviceId = device.id,
                deviceName = device.name,
                deviceIp = device.ip,
                keyPair = keyPair,
            )
            activePairingSessions[device.id] = session

            val currentTimestamp = System.currentTimeMillis()
            val ecdhPublicKeyBytes = CryptoHelper.getPublicKeyBytes(keyPair)
            val ecdhPublicKey = Base64.encodeToString(ecdhPublicKeyBytes, Base64.NO_WRAP)
            
            val request = DPairingRequest(
                fromId = TempData.clientId,
                fromName = deviceName,
                port = TempData.httpsPort,
                deviceType = PhoneHelper.getDeviceType(context),
                ecdhPublicKey = ecdhPublicKey,
                signaturePublicKey = signaturePublicKey,
                timestamp = currentTimestamp,
            )
            request.signature = SignatureHelper.signTextAsync(request.toSignatureData())
                ?: throw Exception("Failed to sign pairing request")
            
            sendPairingMessage(NearbyMessageType.PAIR_REQUEST, JsonHelper.jsonEncode(request), device.ip)
        } catch (e: Exception) {
            LogCat.e("Error starting pairing: ${e.message}")
            sendEvent(PairingFailedEvent(device.id, "Failed to send pairing request"))
        }
    }

    suspend fun respondToPairing(request: DPairingRequest, fromIp: String, accepted: Boolean) {
        try {
            // Verify timestamp to prevent replay attacks
            val currentTime = System.currentTimeMillis()
            if (abs(currentTime - request.timestamp) > MAX_TIMESTAMP_DIFF_MS) {
                LogCat.e("Pairing request timestamp is too old or in the future")
                sendEvent(PairingFailedEvent(request.fromId, "Invalid timestamp"))
                return
            }
            
            // Verify signature
            if (!verifyPairingRequestSignature(request)) {
                LogCat.e("Pairing request signature verification failed")
                sendEvent(PairingFailedEvent(request.fromId, "Signature verification failed"))
                return
            }
            
            LogCat.d("Pairing request signature verified successfully")
            
            if (accepted) {
                // Generate ECDH key pair for this pairing session
                val keyPair = CryptoHelper.generateECDHKeyPair()
                
                // Get our raw Ed25519 signature public key (32 bytes)
                val signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async()
                    ?: throw Exception("Failed to get raw Ed25519 public key")

                // Create pairing session
                val session = DPairingSession(
                    deviceId = request.fromId,
                    deviceName = request.fromName,
                    deviceIp = fromIp,
                    keyPair = keyPair,
                )
                activePairingSessions[request.fromId] = session
                
                val responseTimestamp = System.currentTimeMillis()
                val ecdhPublicKeyBytes = CryptoHelper.getPublicKeyBytes(keyPair)
                val ecdhPublicKey = Base64.encodeToString(ecdhPublicKeyBytes, Base64.NO_WRAP)
                
                val response = DPairingResponse(
                    fromId = TempData.clientId,
                    toId = request.fromId,
                    port = TempData.httpsPort,
                    deviceType = PhoneHelper.getDeviceType(MainApp.instance),
                    ecdhPublicKey = ecdhPublicKey,
                    signaturePublicKey = signaturePublicKey,
                    accepted = true,
                    timestamp = responseTimestamp,
                )

                response.signature = SignatureHelper.signTextAsync(response.toSignatureData())
                    ?: throw Exception("Failed to sign pairing response")

                val requestEcdhPublicKey = Base64.decode(request.ecdhPublicKey, Base64.NO_WRAP)
                val encryptKey = CryptoHelper.computeECDHSharedKey(keyPair.private, requestEcdhPublicKey)
                if (encryptKey != null) {
                    // Store peer in database with signature public key
                    storePeerInDatabase(
                        request.fromId, 
                        request.fromName, 
                        fromIp, 
                        request.port, 
                        request.deviceType, 
                        encryptKey,
                        request.signaturePublicKey
                    )
                    sendPairingMessage(NearbyMessageType.PAIR_RESPONSE, JsonHelper.jsonEncode(response), fromIp)
                    sendEvent(PairingSuccessEvent(request.fromId, request.fromName, fromIp, encryptKey))
                } else {
                    throw Exception("Failed to compute shared key")
                }
            } else {
                // Send rejection response with signature for security
                val signaturePublicKey = SignatureHelper.getRawPublicKeyBase64Async()
                    ?: throw Exception("Failed to get raw Ed25519 public key for rejection")
                
                val rejectionTimestamp = System.currentTimeMillis()
                
                val response = DPairingResponse(
                    fromId = TempData.clientId,
                    toId = request.fromId,
                    port = TempData.httpsPort,
                    deviceType = request.deviceType,
                    ecdhPublicKey = "",
                    signaturePublicKey = signaturePublicKey,
                    accepted = false,
                    timestamp = rejectionTimestamp,
                )
                
                response.signature = SignatureHelper.signTextAsync(response.toSignatureData())
                    ?: throw Exception("Failed to sign pairing rejection response")

                sendPairingMessage(NearbyMessageType.PAIR_RESPONSE, JsonHelper.jsonEncode(response), fromIp)
                LogCat.d("Signed pairing rejection response sent to ${request.fromName}")
            }
        } catch (e: Exception) {
            LogCat.e("Error responding to pairing: ${e.message}")
            sendEvent(PairingFailedEvent(request.fromId, "Failed to respond to pairing request"))
        } finally {
            // Clean up session if not accepted
            if (!accepted) {
                activePairingSessions.remove(request.fromId)
            }
        }
    }

    private fun sendPairingMessage(type: NearbyMessageType, message: String, targetIp: String) {
        UdpUnicastManager.sendUnicast("${type.toPrefix()}${message}", targetIp, MULTICAST_PORT)
    }

    fun cancelPairing(deviceId: String) {
        val session = activePairingSessions[deviceId]
        if (session != null) {
            // Send cancel message to the other device
            try {
                val cancelMessage = DPairingCancel(
                    fromId = TempData.clientId,
                    toId = deviceId
                )
                sendPairingMessage(NearbyMessageType.PAIR_CANCEL, JsonHelper.jsonEncode(cancelMessage), session.deviceIp)
                LogCat.d("Pairing cancel message sent to ${session.deviceName}")
            } catch (e: Exception) {
                LogCat.e("Error sending pairing cancel message: ${e.message}")
            }
        }

        activePairingSessions.remove(deviceId)
        sendEvent(PairingFailedEvent(deviceId, "Pairing cancelled by user"))
        LogCat.d("Pairing cancelled for device: $deviceId")
    }

    suspend fun handlePairingResponse(response: DPairingResponse, senderIP: String) {
        val session = activePairingSessions[response.fromId]
        if (session == null) {
            LogCat.e("No active pairing session found for device ${response.fromId}")
            return
        }

        // Always verify timestamp and signature for all responses
        try {
            // Verify timestamp to prevent replay attacks
            val currentTime = System.currentTimeMillis()
            if (abs(currentTime - response.timestamp) > MAX_TIMESTAMP_DIFF_MS) {
                LogCat.e("Pairing response timestamp is too old or in the future")
                sendEvent(PairingFailedEvent(response.fromId, "Invalid timestamp"))
                return
            }
            
            // Verify signature for all responses (acceptance and rejection)
            if (!verifyPairingResponseSignature(response)) {
                LogCat.e("Pairing response signature verification failed")
                sendEvent(PairingFailedEvent(response.fromId, "Signature verification failed"))
                return
            }
            
            LogCat.d("Pairing response signature verified successfully")

            if (response.accepted) {
                val responseEcdhPublicKey = Base64.decode(response.ecdhPublicKey, Base64.NO_WRAP)
                val encryptKey = CryptoHelper.computeECDHSharedKey(session.keyPair.private, responseEcdhPublicKey)
                if (encryptKey != null) {
                    // Store peer in database with signature public key
                    storePeerInDatabase(
                        response.fromId, 
                        session.deviceName, 
                        senderIP, 
                        response.port, 
                        response.deviceType, 
                        encryptKey,
                        response.signaturePublicKey
                    )
                    sendEvent(PairingSuccessEvent(response.fromId, session.deviceName, senderIP, encryptKey))
                    LogCat.d("Pairing completed successfully with ${session.deviceName}")
                } else {
                    throw Exception("Failed to compute shared key")
                }
            } else {
                sendEvent(PairingFailedEvent(response.fromId, "Pairing request was rejected"))
                LogCat.d("Verified pairing rejection from ${session.deviceName}")
            }
        } catch (e: Exception) {
            LogCat.e("Error processing pairing response: ${e.message}")
            sendEvent(PairingFailedEvent(response.fromId, "Failed to process pairing response"))
        }

        // Clean up session
        activePairingSessions.remove(response.fromId)
    }

    fun handlePairingCancel(cancel: DPairingCancel) {
        LogCat.d("Pairing cancelled by remote device: ${cancel.fromId}")
        sendEvent(PairingCancelledEvent(cancel.fromId))

        // Clean up session if exists
        activePairingSessions.remove(cancel.fromId)
    }

    private suspend fun storePeerInDatabase(
        deviceId: String,
        deviceName: String,
        deviceIp: String,
        port: Int,
        deviceType: DeviceType,
        key: String,
        signaturePublicKey: String
    ) {
        try {
            val existingPeer = AppDatabase.instance.peerDao().getById(deviceId)
            val currentTime = TimeHelper.now()

            if (existingPeer != null) {
                existingPeer.apply {
                    name = deviceName
                    ip = deviceIp
                    this.port = port
                    this.deviceType = deviceType.value
                    this.key = key
                    publicKey = signaturePublicKey // Save raw Ed25519 signature public key (32 bytes)
                    status = "paired"
                    updatedAt = currentTime
                }
                AppDatabase.instance.peerDao().update(existingPeer)
                LogCat.d("Updated existing peer with signature public key: $deviceId")
            } else {
                AppDatabase.instance.peerDao().insert(DPeer(deviceId).apply {
                    id = deviceId
                    name = deviceName
                    ip = deviceIp
                    this.port = port
                    this.deviceType = deviceType.value
                    this.key = key
                    publicKey = signaturePublicKey // Save raw Ed25519 signature public key (32 bytes)
                    status = "paired"
                    createdAt = currentTime
                    updatedAt = currentTime
                })
                LogCat.d("Inserted new peer with signature public key: $deviceId")
            }
            ChatApiManager.loadKeyCacheAsync()
        } catch (e: Exception) {
            LogCat.e("Error storing peer in database: ${e.message}")
            throw e
        }
    }
} 