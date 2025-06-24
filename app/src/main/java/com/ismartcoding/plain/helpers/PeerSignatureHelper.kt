package com.ismartcoding.plain.helpers

import android.util.Base64
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer

object PeerSignatureHelper {
    
    /**
     * Get validated peer for signature operations
     * @param peerId Peer ID
     * @return DPeer object if valid for signature operations, null otherwise
     */
    private suspend fun getValidatedPeer(peerId: String): DPeer? {
        return try {
            val peer = AppDatabase.instance.peerDao().getById(peerId)
            when {
                peer == null -> {
                    LogCat.e("Peer not found: $peerId")
                    null
                }
                peer.status != "paired" -> {
                    LogCat.e("Peer not paired: $peerId")
                    null
                }
                peer.publicKey.isEmpty() -> {
                    LogCat.e("Peer signature public key is empty: $peerId")
                    null
                }
                else -> peer
            }
        } catch (e: Exception) {
            LogCat.e("Error retrieving peer for validation: ${e.message}")
            null
        }
    }
    
    /**
     * Verify message signature from a specific peer
     * @param peerId Peer ID
     * @param message Original message content
     * @param signature Signature (Base64 encoded)
     * @return Verification result
     */
    suspend fun verifyPeerMessage(peerId: String, message: String, signature: String): Boolean {
        return try {
            val peer = getValidatedPeer(peerId) ?: return false
            
            val signatureBytes = Base64.decode(signature, Base64.NO_WRAP)
            val messageBytes = message.toByteArray()
            
            val rawPublicKey = Base64.decode(peer.publicKey, Base64.NO_WRAP)
            val result = CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, messageBytes, signatureBytes)
            result
        } catch (e: Exception) {
            LogCat.e("Error verifying peer message signature: ${e.message}")
            false
        }
    }
    
    /**
     * Create signature for message to be sent to peer
     * @param message Message content to sign
     * @return Signature (Base64 encoded), null if failed
     */
    suspend fun signMessageForPeer(message: String): String? {
        return try {
            SignatureHelper.signTextAsync(message)
        } catch (e: Exception) {
            LogCat.e("Error signing message for peer: ${e.message}")
            null
        }
    }
    
    /**
     * Create signature data for chat message
     * @param content Message content
     * @param timestamp Timestamp
     * @param fromId Sender ID
     * @param toId Receiver ID
     * @return Data string to be signed
     */
    fun createChatMessageSignatureData(
        content: String, 
        timestamp: Long, 
        fromId: String, 
        toId: String
    ): String {
        return "$content|$timestamp|$fromId|$toId"
    }
} 