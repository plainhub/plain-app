package com.ismartcoding.plain.db

import android.util.Base64
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.helpers.NetworkHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.SignatureHelper

suspend fun DChatChannel.getPeersAsync(): List<DPeer> = withIO {
    val ids = memberIds()
    val dbPeers = AppDatabase.instance.peerDao().getByIds(ids).associateBy { it.id }
    ids.mapNotNull { peerId ->
        if (peerId == TempData.clientId) {
            DPeer(
                id = peerId,
                name = TempData.deviceName.value,
                ip = NetworkHelper.getDeviceIP4s().joinToString(","),
                port = TempData.httpsPort.value,
                publicKey = SignatureHelper.getRawPublicKeyBase64Async(),
                deviceType = DeviceType.PHONE.value,
            )
        } else {
            dbPeers[peerId]
        }
    }
}

fun mePeer(): DPeer = DPeer(
    id = TempData.clientId,
    name = TempData.deviceName.value,
    ip = NetworkHelper.getDeviceIP4s().joinToString(","),
    port = TempData.httpsPort.value,
    deviceType = DeviceType.PHONE.value,
)

fun DChatChannel.getOwner(): DPeer? {
    return if (isOwnedByMe()) mePeer() else PeerCacher.getPeer(owner)
}

/**
 * Verify an Ed25519 signature with the given raw public key (Base64) against
 * a UTF-8 string payload (Base64 signature).
 *
 * Returns false on missing/empty fields, bad Base64, or any verification error.
 */
fun verifyEd25519Signature(
    publicKeyBase64: String,
    payload: String,
    signatureBase64: String,
): Boolean {
    if (publicKeyBase64.isEmpty() || signatureBase64.isEmpty()) return true
    return try {
        val publicKey = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
        val signature = Base64.decode(signatureBase64, Base64.NO_WRAP)
        CryptoHelper.verifySignatureWithRawEd25519PublicKey(publicKey, payload.toByteArray(), signature)
    } catch (_: Exception) {
        false
    }
}