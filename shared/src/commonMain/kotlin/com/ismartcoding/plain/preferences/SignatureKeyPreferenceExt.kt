package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.crypto.PairingCrypto
import com.ismartcoding.plain.data.DSignatureKeyPair
import com.ismartcoding.plain.helpers.JsonHelper
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
suspend fun SignatureKeyPreference.ensureKeyPairAsync() {
    val keyPairJson = getAsync()
    if (keyPairJson.isEmpty()) {
        val (privateKey, publicKey) = PairingCrypto.generateEd25519KeyPair()
        val signatureKeyPair = DSignatureKeyPair(
            privateKey = Base64.encode(privateKey),
            publicKey = Base64.encode(publicKey),
        )
        putAsync(JsonHelper.jsonEncode(signatureKeyPair))
    }
}

suspend fun SignatureKeyPreference.getKeyPairAsync(): DSignatureKeyPair {
    return JsonHelper.jsonDecode<DSignatureKeyPair>(getAsync())
}

suspend fun SignatureKeyPreference.getPublicKeyBytesAsync(): ByteArray {
    val keyPair = getKeyPairAsync()
    return Base64.decode(keyPair.publicKey)
}
