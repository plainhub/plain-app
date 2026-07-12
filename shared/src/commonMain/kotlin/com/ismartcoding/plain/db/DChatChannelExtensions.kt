package com.ismartcoding.plain.db
import com.ismartcoding.plain.platform.AppDatabase

import kotlin.io.encoding.ExperimentalEncodingApi
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.verifyEd25519Signature
import com.ismartcoding.plain.platform.getDeviceIP4s
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
                ip = getDeviceIP4s().joinToString(","),
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
    ip = getDeviceIP4s().joinToString(","),
    port = TempData.httpsPort.value,
    deviceType = DeviceType.PHONE.value,
)

fun DChatChannel.getOwner(): DPeer? {
    return if (isOwnedByMe()) mePeer() else PeerCacher.getPeer(owner)
}

@OptIn(ExperimentalEncodingApi::class)
fun verifyEd25519Signature(
    publicKeyBase64: String,
    payload: String,
    signatureBase64: String,
): Boolean {
    if (publicKeyBase64.isEmpty() || signatureBase64.isEmpty()) return true
    return try {
        val publicKey = Base64Lenient.decode(publicKeyBase64)
        val signature = Base64Lenient.decode(signatureBase64)
        verifyEd25519Signature(publicKey, payload.encodeToByteArray(), signature)
    } catch (_: Exception) {
        false
    }
}
