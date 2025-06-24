package com.ismartcoding.plain.features

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.api.GraphQLClient
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.helpers.PeerSignatureHelper

object PeerChatHelper {
    
    // Maximum allowed time difference for timestamp validation (5 minutes)
    private const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L
    
    suspend fun sendMessageToPeerAsync(peerId: String, content: DMessageContent): Boolean {
        return withIO {
            try {
                // Get peer information
                val peer = AppDatabase.instance.peerDao().getById(peerId)
                if (peer == null) {
                    LogCat.e("Peer not found: $peerId")
                    return@withIO false
                }
                
                if (peer.status != "paired") {
                    LogCat.e("Peer not paired: $peerId")
                    return@withIO false
                }
                
                if (peer.key.isEmpty()) {
                    LogCat.e("Peer key is empty: $peerId")
                    return@withIO false
                }
                
                val contentJson = content.toJSONString()
                
                // Create HTTP client
                val httpClient = HttpClientManager.createCryptoHttpClient(peer.key, 10)
                
                // Send GraphQL request using GraphQL client (timestamp and signature will be generated automatically)
                val url = "https://${peer.ip}:${peer.port}/peer_graphql"
                val clientId = TempData.clientId
                
                val response = GraphQLClient.createChatItem(
                    httpClient = httpClient,
                    url = url,
                    clientId = clientId,
                    content = contentJson
                )
                
                if (response != null && response.errors.isNullOrEmpty()) {
                    LogCat.d("Message sent successfully to peer $peerId: ${response.data}")
                    return@withIO true
                } else {
                    val errorMessages = response?.errors?.joinToString(", ") { it.message } ?: "Unknown error"
                    LogCat.e("Failed to send message to peer $peerId: $errorMessages")
                    return@withIO false
                }
                
            } catch (e: Exception) {
                LogCat.e("Error sending message to peer $peerId: ${e.message}")
                e.printStackTrace()
                return@withIO false
            }
        }
    }
    
    /**
     * Verify signature of message received from peer
     * @param peerId Sender peer ID
     * @param content Message content
     * @param signature Message signature (Base64 encoded) - REQUIRED
     * @param timestamp Message timestamp
     * @return Verification result
     */
    suspend fun verifyPeerMessageAsync(
        peerId: String, 
        content: String, 
        signature: String?, 
        timestamp: Long
    ): Boolean {
        return withIO {
            try {
                // Signature is mandatory - reject messages without signature
                if (signature.isNullOrEmpty()) {
                    LogCat.e("Message from peer $peerId has no signature - rejected")
                    return@withIO false
                }
                
                // Verify timestamp (prevent replay attacks)
                val currentTime = System.currentTimeMillis()
                if (Math.abs(currentTime - timestamp) > MAX_TIMESTAMP_DIFF_MS) {
                    LogCat.e("Message timestamp is too old or in the future: $timestamp - rejected")
                    return@withIO false
                }
                
                // Create signature data: timestamp + requestJSON (same format as sender)
                val signatureData = "$timestamp$content"
                
                // Verify signature
                val isValid = PeerSignatureHelper.verifyPeerMessage(peerId, signatureData, signature)
                return@withIO isValid
            } catch (e: Exception) {
                LogCat.e("Error verifying peer message: ${e.message}")
                return@withIO false
            }
        }
    }
} 